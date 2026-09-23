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
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * "Cargar un save" DE VERDAD: cuando cae el grupo entero (ver PlayerReviveBridge#isFullWipe), no alcanza
 * con reubicar jugadores, porque el mundo (bloques rotos, mobs muertos, cofres saqueados) seguiria como
 * estaba. Un mod no puede revertir el mundo mientras el server lo sigue usando, asi que la unica forma
 * segura es frenar el server (esto es un integrated server: single o LAN, asi que el cliente vuelve solo
 * al menu en segundos, SIN cerrar el juego) y restaurar los archivos recien ahi, con el mundo ya cerrado.
 * Este server-tick solo avisa, cuenta y escribe el marcador; quien hace la copia real es
 * ClientWorldRestore, del lado cliente, apenas detecta que no hay mundo cargado.
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

    /** "restoring"/"countdown" son estaticos: sobreviven a que el server se frene, porque el juego
     *  (y el classloader) sigue vivo hasta que cierran del todo el proceso. Sin este reset, apenas
     *  se restauraba un wipe UNA vez quedaba "restoring=true" para siempre y el sistema dejaba de
     *  detectar cualquier caida de grupo siguiente, en ese mismo mundo o en cualquier otro que se
     *  abriera despues. Arrancar un server nuevo (entrar a CUALQUIER mundo) es el momento correcto
     *  para garantizar que arranca limpio. */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        restoring = false;
        countdown = -1;
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
                LOGGER.warn("[tcorigenes] Grupo caído entero: frenando el server para restaurar el punto de guardado.");
                // OJO: esto corre en el hilo del propio server (server tick). halt(true) espera a
                // que ESE hilo termine, asi que llamado desde el mismo hilo se auto-bloquea (el
                // server queda colgado del todo, ni ticks ni "Saving world" nunca terminan). Con
                // false el pedido de parada queda seteado y el propio bucle del server lo nota y
                // se apaga solo apenas termina este tick, sin esperar a si mismo.
                server.halt(false);
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
        if (target == null && !server.isDedicatedServer()
                && Files.isDirectory(CheckpointSnapshotter.snapshotPathFor(server, CheckpointSnapshotter.INITIAL_ID))) {
            // Sin ningun save de cama se vuelve al save inicial automatico (la primera vez que se entro al mundo).
            beginRestore(server, CheckpointSnapshotter.snapshotPathFor(server, CheckpointSnapshotter.INITIAL_ID),
                    "El grupo cayó entero sin haber guardado en ninguna cama: el mundo vuelve al principio en 5 segundos. "
                            + "Se pierde todo el progreso.");
            return;
        }
        if (target == null && !server.isDedicatedServer()) {
            // Sin ningun save de cama no hay a donde volver: el mundo se regenera de cero con la misma
            // seed y se pierde todo el progreso (ver ClientWorldRestore#regenerate).
            beginRestore(server, (Path) null, "El grupo cayó entero y no había ningún punto de guardado en una cama: "
                    + "el mundo se regenera desde cero con la misma seed en 5 segundos. Se pierde todo el progreso.");
            return;
        }
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
        beginRestore(server, target, "El grupo cayó entero. El mundo va a volver al último punto de guardado en 5 segundos: "
                + "se corta la partida solo, se restaura y podés volver a entrar.");
    }

    /** Pedido de cargar un punto de guardado (caida de grupo, o elegido a mano al entrar al mundo):
     *  escribe el marcador, avisa y en 5 segundos frena el server; ClientWorldRestore hace la copia. */
    public static boolean beginRestore(MinecraftServer server, Checkpoint target, String message) {
        return beginRestore(server, target == null ? null : CheckpointSnapshotter.snapshotPathFor(server, target.id), message);
    }

    /** snapshot == null: regenerar el mundo de cero con la misma seed (ultimo recurso si no hay save inicial). */
    public static boolean beginRestore(MinecraftServer server, Path snapshot, String message) {
        if (restoring) {
            return false;
        }
        if (snapshot != null && !Files.isDirectory(snapshot)) {
            return false;
        }
        writeMarker(server, server.getWorldPath(LevelResource.ROOT), snapshot);
        restoring = true;
        countdown = COUNTDOWN_TICKS;
        server.getPlayerList().broadcastSystemMessage(Component.literal(message).withStyle(ChatFormatting.DARK_RED), false);
        return true;
    }

    private static void writeMarker(MinecraftServer server, Path worldRoot, Path snapshot) {
        Path marker = server.getServerDirectory().toPath().resolve(MARKER_FILE);
        // snapshot == null: regenerar el mundo desde cero con la misma seed (sin ningun save de cama).
        String content = "world_root=" + worldRoot.toAbsolutePath() + System.lineSeparator()
                + (snapshot == null ? "regenerate=true" : "snapshot=" + snapshot.toAbsolutePath()) + System.lineSeparator();
        try {
            Files.writeString(marker, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[tcorigenes] No se pudo escribir el marcador de restauración", e);
        }
    }
}
