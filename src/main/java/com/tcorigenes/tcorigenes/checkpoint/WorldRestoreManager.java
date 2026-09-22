// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * "Cargar un save" DE VERDAD: cuando cae el grupo entero (ver PlayerReviveBridge#isFullWipe), no alcanza
 * con reubicar jugadores, porque el mundo (bloques rotos, mobs muertos, cofres saqueados) seguiria como
 * estaba. Un mod no puede revertir el mundo mientras el server sigue corriendo, asi que la unica forma
 * segura es: avisar, escribir que snapshot hay que restaurar (ver CheckpointSnapshotter), y CERRAR EL
 * JUEGO DEL TODO. El restablecimiento real de los archivos lo hace un script aparte, afuera del juego
 * (ver tools/restore_after_wipe.py), que hay que correr antes de volver a abrir el mundo.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class WorldRestoreManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MARKER_FILE = "pending_restore.txt";
    /** Tiempo para que se lea el aviso antes de que el juego se cierre solo. */
    private static final int COUNTDOWN_TICKS = 20 * 5;

    private static boolean restoring = false;
    private static int countdown = -1;

    private WorldRestoreManager() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (restoring) {
            if (countdown > 0) {
                countdown--;
            } else if (countdown == 0) {
                countdown = -1;
                LOGGER.warn("[tcorigenes] Grupo caído entero: cerrando el juego para restaurar el punto de guardado.");
                server.halt(true);
                System.exit(0); // fuerza el cierre completo (integrado o dedicado) para poder tocar los archivos despues
            }
            return;
        }
        if (server.getTickCount() % 20 != 0 || !PlayerReviveBridge.isLoaded()) {
            return; // 1 chequeo por segundo alcanza; sin PlayerRevive no hay forma de saber "todos caidos"
        }
        if (PlayerReviveBridge.isFullWipe(server)) {
            triggerWipe(server);
        }
    }

    private static void triggerWipe(MinecraftServer server) {
        Checkpoint target = CheckpointManager.active(server).stream().findFirst().orElse(null);
        if (target == null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "El grupo cayó entero, pero todavía no hay ningún punto de guardado (coloca una cama). "
                            + "Alguien va a tener que reviviros a mano.").withStyle(ChatFormatting.DARK_RED), false);
            return;
        }
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path snapshot = CheckpointSnapshotter.snapshotPathFor(server, target.id);
        if (!Files.isDirectory(snapshot)) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "El grupo cayó entero, pero la copia de ese punto de guardado todavía no está lista. "
                            + "Alguien va a tener que reviviros a mano.").withStyle(ChatFormatting.DARK_RED), false);
            return;
        }
        writeMarker(server, worldRoot, snapshot);
        restoring = true;
        countdown = COUNTDOWN_TICKS;
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "El grupo cayó entero. El mundo va a volver al último punto de guardado: el juego se cierra en 5 segundos. "
                        + "Corré restaurar_ultimo_punto.bat antes de volver a abrirlo.").withStyle(ChatFormatting.DARK_RED), false);
    }

    private static void writeMarker(MinecraftServer server, Path worldRoot, Path snapshot) {
        Path marker = server.getServerDirectory().toPath().resolve(MARKER_FILE);
        String content = "world_root=" + worldRoot.toAbsolutePath() + System.lineSeparator()
                + "snapshot=" + snapshot.toAbsolutePath() + System.lineSeparator();
        try {
            Files.writeString(marker, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[tcorigenes] No se pudo escribir el marcador de restauración", e);
        }
    }
}
