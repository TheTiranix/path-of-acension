// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Copia real del mundo por cada punto de guardado, para poder "cargar un save" de verdad (ver
 * WorldRestoreManager): sin esto, un punto de guardado solo reubicaba jugadores, pero el mundo (bloques
 * rotos, mobs muertos, cofres saqueados) seguia como estaba. La copia corre en un hilo aparte para no
 * trabar el server; antes se fuerza un guardado sincronico para que la copia sea lo mas consistente posible
 * (un mod no puede garantizar una foto 100% atomica de un mundo que sigue corriendo).
 */
public final class CheckpointSnapshotter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SNAPSHOTS_DIR_NAME = "checkpoint_snapshots";

    /** Id fijo del save inicial automatico (no es un punto de cama: no aparece en la lista). */
    public static final UUID INITIAL_ID = new UUID(0L, 1L);

    private CheckpointSnapshotter() {
    }

    private static Path snapshotsDir(MinecraftServer server) {
        return server.getServerDirectory().toPath().resolve(SNAPSHOTS_DIR_NAME);
    }

    /** Copia oculta del mundo al "Guardar y salir" (no aparece en el juego ni en la lista de saves). */
    public static Path autosavePath(MinecraftServer server) {
        return snapshotsDir(server).resolve("autosave_salida");
    }

    public static Path snapshotPathFor(MinecraftServer server, UUID checkpointId) {
        return snapshotsDir(server).resolve(checkpointId.toString());
    }

    /** Guarda todo (sincronico, es un evento raro) y despues copia el mundo entero a la carpeta del
     *  snapshot en un hilo aparte, para no congelar el server con mundos grandes. */
    public static void takeSnapshotAsync(MinecraftServer server, UUID checkpointId) {
        server.saveEverything(false, true, true);
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path dest = snapshotPathFor(server, checkpointId);
        Thread thread = new Thread(() -> {
            try {
                copyTree(worldRoot, dest);
                LOGGER.info("[tcorigenes] Punto de guardado {}: copia del mundo lista en {}", checkpointId, dest);
            } catch (IOException e) {
                LOGGER.error("[tcorigenes] No se pudo copiar el mundo para el punto de guardado {}", checkpointId, e);
            }
        }, "tcorigenes-checkpoint-snapshot");
        thread.setDaemon(true);
        thread.start();
    }

    public static void deleteSnapshot(MinecraftServer server, UUID checkpointId) {
        deleteTree(snapshotPathFor(server, checkpointId));
    }

    /** Reemplaza worldRoot por la copia del snapshot. Sincronico: se llama con el mundo ya cerrado
     *  (ver ClientWorldRestore), nunca mientras el server sigue corriendo. */
    public static void restoreFromSnapshot(Path worldRoot, Path snapshot) throws IOException {
        deleteTree(worldRoot);
        copyTree(snapshot, worldRoot);
    }

    public static void copyTree(Path source, Path dest) throws IOException {
        Files.createDirectories(dest);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(dest.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // session.lock lo tiene abierto el server: no hace falta copiarlo (no forma parte del mundo en si).
                if (file.getFileName().toString().equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    Files.copy(file, dest.resolve(source.relativize(file)),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.warn("[tcorigenes] No se pudo copiar {} al snapshot", file, e);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteTree(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.warn("[tcorigenes] No se pudo borrar el snapshot viejo {}", dir, e);
        }
    }
}
