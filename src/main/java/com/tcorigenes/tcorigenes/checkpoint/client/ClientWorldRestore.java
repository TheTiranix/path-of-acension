// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint.client;

import com.tcorigenes.tcorigenes.checkpoint.CheckpointSnapshotter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Restaura el mundo AL VUELO, sin cerrar el juego: cuando WorldRestoreManager frena el server (integrated
 * server, single o LAN), el cliente vuelve solo al menu en segundos. Apenas detectamos eso (mc.level ==
 * null) y encontramos el marcador que dejo el server, copiamos el snapshot encima del mundo en este mismo
 * proceso (ya no hace falta cerrar el juego ni correr un script aparte: los archivos ya estan libres).
 * Reintenta unas cuantas veces por si Windows todavia no solto algun archivo del mundo recien cerrado.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class ClientWorldRestore {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MARKER_FILE = "pending_restore.txt";
    private static final int MAX_ATTEMPTS = 20;
    private static final long RETRY_DELAY_MS = 500;

    private static volatile boolean restoring = false;

    private ClientWorldRestore() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || restoring) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return; // solo restauramos con el mundo ya cerrado del todo
        }
        Path marker = mc.gameDirectory.toPath().resolve(MARKER_FILE);
        if (!Files.isRegularFile(marker)) {
            return;
        }
        restoring = true;
        Thread thread = new Thread(() -> doRestore(marker), "tcorigenes-world-restore");
        thread.setDaemon(true);
        thread.start();
    }

    private static void doRestore(Path marker) {
        try {
            Map<String, String> values = readMarker(marker);
            String worldRootStr = values.get("world_root");
            String snapshotStr = values.get("snapshot");
            if (worldRootStr == null || snapshotStr == null) {
                LOGGER.error("[tcorigenes] Marcador de restauración incompleto, lo borro sin tocar el mundo: {}", marker);
                Files.deleteIfExists(marker);
                return;
            }
            Path worldRoot = Path.of(worldRootStr);
            Path snapshot = Path.of(snapshotStr);
            if (!Files.isDirectory(snapshot)) {
                LOGGER.error("[tcorigenes] No encuentro la copia del punto de guardado: {}", snapshot);
                Files.deleteIfExists(marker);
                notify("No se pudo restaurar el mundo: no encontré la copia del punto de guardado. Revisá el log.");
                return;
            }
            IOException lastError = tryRestore(worldRoot, snapshot);
            Files.deleteIfExists(marker);
            if (lastError == null) {
                LOGGER.info("[tcorigenes] Mundo restaurado a {}", snapshot);
                notify("Mundo restaurado a tu último punto de guardado. Ya podés volver a entrar.");
            } else {
                LOGGER.error("[tcorigenes] No se pudo restaurar el mundo tras varios intentos", lastError);
                notify("No se pudo restaurar el mundo automáticamente (ver log). No entres a la partida sin revisarlo.");
            }
        } catch (IOException e) {
            LOGGER.error("[tcorigenes] Error leyendo el marcador de restauración", e);
        } finally {
            restoring = false;
        }
    }

    private static IOException tryRestore(Path worldRoot, Path snapshot) {
        IOException lastError = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                CheckpointSnapshotter.restoreFromSnapshot(worldRoot, snapshot);
                return null;
            } catch (IOException e) {
                lastError = e; // probablemente Windows todavia no soltó algún archivo recién cerrado
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return e;
                }
            }
        }
        return lastError;
    }

    private static Map<String, String> readMarker(Path marker) throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String line : Files.readAllLines(marker, StandardCharsets.UTF_8)) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
        return values;
    }

    private static void notify(String message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> SystemToast.add(mc.getToasts(), SystemToast.SystemToastIds.WORLD_BACKUP,
                Component.literal("Path of Ascension"), Component.literal(message)));
    }
}
