// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core.capability;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.RaceSyncPacket;
import net.minecraft.server.level.ServerPlayer;

/** Manda la raza (y purificacion) de un jugador a TODOS los clientes (hace falta para cuernos/alas/altura/piel en multijugador). */
public final class RaceSync {
    private RaceSync() {
    }

    public static void broadcast(ServerPlayer player, Race race) {
        boolean purified = player.getPersistentData().getBoolean("malnacido_purificado");
        Networking.sendToAll(new RaceSyncPacket(player.getUUID(), race, purified));
    }

    /** Al loguearse un jugador nuevo: le manda la raza de todos los demas (los demas ya se
     *  enteran de la suya via el broadcast normal que dispara su propio login/eleccion). */
    public static void syncAllTo(ServerPlayer newPlayer) {
        for (ServerPlayer other : newPlayer.getServer().getPlayerList().getPlayers()) {
            other.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> {
                boolean purified = other.getPersistentData().getBoolean("malnacido_purificado");
                Networking.sendToPlayer(newPlayer, new RaceSyncPacket(other.getUUID(), raceInfo.getRace(), purified));
            });
        }
    }
}
