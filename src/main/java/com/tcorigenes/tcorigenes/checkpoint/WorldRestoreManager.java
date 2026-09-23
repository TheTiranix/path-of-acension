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
import net.minecraftforge.event.server.ServerStoppingEvent;
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

    /** Singleplayer, al "Guardar y salir": el guardado del mundo va ademas a una carpeta oculta
     *  (autosave_salida, fuera de la lista de saves del juego) y, si no hay ningun save de cama, el
     *  mundo vuelve al save inicial para que la proxima entrada sea desde ahi. Con saves de cama no se
     *  toca nada mas: al entrar se elige uno (ver CheckpointManager#onLogin). */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isSingleplayer() || server.getServerDirectory().toPath().resolve(MARKER_FILE).toFile().exists()) {
            return;
        }
        Path initial = CheckpointSnapshotter.snapshotPathFor(server, CheckpointSnapshotter.INITIAL_ID);
        boolean noBeds = CheckpointManager.active(server).isEmpty();
        writeMarker(server, server.getWorldPath(LevelResource.ROOT),
                noBeds && Files.isDirectory(initial) ? initial : null, CheckpointSnapshotter.autosavePath(server), false);
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
        if (server.isDedicatedServer()) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "El grupo cayó entero. Alguien va a tener que reviviros a mano.").withStyle(ChatFormatting.DARK_RED), false);
            return;
        }
        if (!CheckpointManager.active(server).isEmpty()) {
            // Con saves de cama solo se los saca al menu: al volver a entrar se elige uno de la lista.
            restoring = true;
            countdown = COUNTDOWN_TICKS;
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "El grupo cayó entero: en 5 segundos volvés al menú. Al entrar de nuevo elegí uno de tus saves de cama.")
                    .withStyle(ChatFormatting.DARK_RED), false);
            return;
        }
        Path initial = CheckpointSnapshotter.snapshotPathFor(server, CheckpointSnapshotter.INITIAL_ID);
        if (Files.isDirectory(initial)) {
            beginRestore(server, initial, "El grupo cayó entero sin haber guardado en ninguna cama: "
                    + "en 5 segundos volvés al menú y el mundo vuelve al principio. Se pierde todo el progreso.");
            return;
        }
        beginRestore(server, (Path) null, "El grupo cayó entero y no había ningún save: "
                + "el mundo se regenera desde cero con la misma seed en 5 segundos.");
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
        writeMarker(server, server.getWorldPath(LevelResource.ROOT), snapshot, null, snapshot == null);
        restoring = true;
        countdown = COUNTDOWN_TICKS;
        server.getPlayerList().broadcastSystemMessage(Component.literal(message).withStyle(ChatFormatting.DARK_RED), false);
        return true;
    }

    private static void writeMarker(MinecraftServer server, Path worldRoot, Path snapshot, Path backup, boolean regenerate) {
        Path marker = server.getServerDirectory().toPath().resolve(MARKER_FILE);
        StringBuilder content = new StringBuilder("world_root=" + worldRoot.toAbsolutePath() + System.lineSeparator());
        if (backup != null) {
            content.append("backup=").append(backup.toAbsolutePath()).append(System.lineSeparator());
        }
        if (snapshot != null) {
            content.append("snapshot=").append(snapshot.toAbsolutePath()).append(System.lineSeparator());
        } else if (regenerate) {
            content.append("regenerate=true").append(System.lineSeparator());
        }
        try {
            Files.writeString(marker, content.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[tcorigenes] No se pudo escribir el marcador de restauración", e);
        }
    }
}
