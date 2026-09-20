// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.favor;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.favor.network.FavorSyncPacket;
import com.tcorigenes.tcorigenes.networking.Networking;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/**
 * Punto unico para tocar favor: aplica los dos modificadores de raza conocidos (Hereje nunca
 * pasa de 0 con NINGUN dios, Devoto gana un 80% mas rapido del panteon creador real -Pater,
 * Meidris, Filis- pero NO de Luna -ente paralelo- ni de Deiros -angel caido, opuesto a Pater-
 * ni de Tempo -secreto-). Todavia no hay otras fuentes de favor fuera de /favor, los altares
 * (ver AltarBlock) y las 3 acciones automaticas (ver FavorEvents).
 */
public final class FavorManager {
    private static final Set<Deity> CREATOR_PANTHEON = Set.of(Deity.PATER, Deity.MEIDRIS, Deity.FILIS);

    /** Dioses de los que cada raza gana favor un 80% mas rapido y con los que empieza con 10 de favor. */
    private static Set<Deity> affinity(Race race) {
        return switch (race) {
            case DEVOTO, ANGEL -> CREATOR_PANTHEON;
            case DEMONIO -> Set.of(Deity.DEIROS);
            case SIERVO_DE_LA_LUNA -> Set.of(Deity.LUNA);
            default -> Set.of();
        };
    }

    private FavorManager() {
    }

    /** Suma (o resta, con amount negativo) favor de un dios, ya con los modificadores de raza aplicados. */
    public static void addFavor(ServerPlayer player, Deity deity, int amount) {
        player.getCapability(PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY).ifPresent(data -> {
            Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                    .map(raceInfo -> raceInfo.getRace()).orElse(Race.HUMANO);

            int delta = amount;
            if (delta > 0 && affinity(race).contains(deity)) {
                delta = Math.round(delta * 1.8F);
            }

            int newValue = data.getFavor(deity) + delta;
            if (race == Race.HEREJE) {
                newValue = Math.min(newValue, 0);
            }
            data.setFavor(deity, newValue);
            sync(player, data);
        });
    }

    public static int getFavor(ServerPlayer player, Deity deity) {
        return player.getCapability(PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY)
                .map(data -> data.getFavor(deity)).orElse(0);
    }

    /** Al elegir Devoto/Angel (panteon creador), Demonio (Deiros) o Siervo de la Luna (Luna):
     *  empieza con 10 de favor de esos dioses (sin bajarlo si ya tenia mas). */
    public static void grantRaceStartingFavor(ServerPlayer player, Race race) {
        Set<Deity> deities = affinity(race);
        if (deities.isEmpty()) {
            return;
        }
        player.getCapability(PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY).ifPresent(data -> {
            for (Deity deity : deities) {
                data.setFavor(deity, Math.max(data.getFavor(deity), 10));
            }
            sync(player, data);
        });
    }

    /** Al elegir Hereje: el favor que ya tuviera se corta a 0 (nunca puede ser mayor a 0). */
    public static void clampHerejeFavor(ServerPlayer player) {
        player.getCapability(PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY).ifPresent(data -> {
            for (Deity deity : Deity.values()) {
                if (data.getFavor(deity) > 0) {
                    data.setFavor(deity, 0);
                }
            }
            sync(player, data);
        });
    }

    /** Manda el estado completo de favor al cliente (HUD). Publico para poder llamarlo en
     *  login/respawn (ver ModEvents) ademas de cada vez que un valor cambia. */
    public static void syncToClient(ServerPlayer player) {
        player.getCapability(PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY).ifPresent(data -> sync(player, data));
    }

    private static void sync(ServerPlayer player, PlayerFavor.IPlayerFavor data) {
        Networking.sendToPlayer(player, new FavorSyncPacket(data.getAll()));
    }
}
