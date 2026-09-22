// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Puente hacia el mod PlayerRevive (ya usado en PlayerReviveCompat): en vez de un "cuerpo" propio que
 * hay que rematar, un jugador que llega a 0 de vida queda TIRADO EN EL PISO, con su cuerpo y su
 * inventario de verdad, esperando que lo revivan (estilo Fortnite) o que se desangre del todo. Esta
 * clase solo consulta/actua sobre ese estado; no lo crea (eso ya lo hace PlayerRevive solo).
 * Unico lugar que toca sus clases, para no romper si el dia de mañana se saca del modpack.
 */
public final class PlayerReviveBridge {
    private PlayerReviveBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("playerrevive");
    }

    /** True si el jugador esta caido ahora mismo (tirado, esperando revivida). */
    public static boolean isDown(ServerPlayer player) {
        if (!isLoaded()) {
            return false;
        }
        return player.getCapability(team.creative.playerrevive.PlayerRevive.BLEEDING)
                .map(team.creative.playerrevive.api.IBleeding::isBleeding).orElse(false);
    }

    /** Revive de una a TODOS los caidos del servidor (el Punto de Reanimacion revive a todo el grupo). */
    public static int reviveAllDown(MinecraftServer server) {
        if (!isLoaded()) {
            return 0;
        }
        int revived = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var bleeding = player.getCapability(team.creative.playerrevive.PlayerRevive.BLEEDING).resolve();
            if (bleeding.isPresent() && bleeding.get().isBleeding()) {
                bleeding.get().revive();
                revived++;
            }
        }
        return revived;
    }

    /** True si NINGUN jugador conectado esta consciente: todos estan caidos o ya murieron de verdad
     *  (en el death screen, esperando respawn). Ese es el momento de restaurar el mundo entero. */
    public static boolean isFullWipe(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return false;
        }
        for (ServerPlayer player : players) {
            boolean conscious = player.isAlive() && player.getHealth() > 0.0F && !isDown(player);
            if (conscious) {
                return false;
            }
        }
        return true;
    }
}
