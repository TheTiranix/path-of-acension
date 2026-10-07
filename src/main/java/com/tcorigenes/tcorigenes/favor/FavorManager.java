// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.favor;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.favor.network.FavorSyncPacket;
import com.tcorigenes.tcorigenes.networking.Networking;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
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
            case DEMONIO -> Set.of(Deity.DEIROS, Deity.LUNA);
            case SIERVO_DE_LA_LUNA -> Set.of(Deity.LUNA, Deity.DEIROS);
            case AUTOMATA -> Set.of(Deity.DEIROS);
            default -> Set.of();
        };
    }

    /** El Autómata es, en el lore, el sirviente de Deiros: empieza con mucho mas favor que el resto (75, no 10). */
    private static int startingFavorFor(Race race, Deity deity) {
        return race == Race.AUTOMATA && deity == Deity.DEIROS ? 75 : 10;
    }

    private FavorManager() {
    }

    // ------------------------------------------------------------------ dios patron (sistema de favor con opuestos)
    private static final String PATRON_KEY = "pa_patron";
    private static final String FRAC_KEY = "pa_favor_frac_";
    /** Favor que hay que juntar con acciones para que ese dios quede como patron (si antes no se uso un altar). */
    private static final int PATRON_THRESHOLD = 15;
    /** Con patron elegido: el favor de los demas dioses nunca supera esto, y el del opuesto arranca en negativo. */
    private static final int OTHERS_CAP = 5;
    private static final int OPPOSITE_START = -10;
    private static final float SAME_CAMP_GAIN = 0.6F;
    private static final float OTHER_CAMP_GAIN = 0.15F;
    private static final float OPPOSITE_GAIN = 0.1F;
    private static final float OPPOSITE_LOSS = 0.5F;
    private static final Set<Deity> REBEL_CAMP = Set.of(Deity.DEIROS, Deity.LUNA);

    /** El dios opuesto: si ganas favor de uno, baja el del otro. */
    public static Deity opposite(Deity deity) {
        return switch (deity) {
            case PATER -> Deity.DEIROS;
            case DEIROS -> Deity.PATER;
            case MEIDRIS -> Deity.LUNA;
            case LUNA -> Deity.MEIDRIS;
            case FILIS -> Deity.TEMPO;
            case TEMPO -> Deity.FILIS;
        };
    }

    private static boolean sameCamp(Deity a, Deity b) {
        if (a == Deity.TEMPO || b == Deity.TEMPO) {
            return false;
        }
        return REBEL_CAMP.contains(a) == REBEL_CAMP.contains(b);
    }

    /** El dios patron del jugador, o null si todavia no se determino. */
    public static Deity patron(Player player) {
        net.minecraft.nbt.CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(PATRON_KEY)) {
            return null;
        }
        try {
            return Deity.valueOf(persisted.getString(PATRON_KEY));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void setPatron(ServerPlayer player, Deity deity) {
        net.minecraft.nbt.CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putString(PATRON_KEY, deity.name());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        Deity opposite = opposite(deity);
        player.getCapability(PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY).ifPresent(data -> {
            for (Deity other : Deity.values()) {
                if (other == deity) {
                    continue;
                }
                int value = data.getFavor(other);
                data.setFavor(other, other == opposite ? Math.min(value, OPPOSITE_START) : Math.min(value, OTHERS_CAP));
            }
            sync(player, data);
        });
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("tcorigenes.favor.patron", deity.getDisplayName(),
                opposite.getDisplayName()).withStyle(net.minecraft.ChatFormatting.GOLD));
    }

    /** Favor ganado en un altar (ofrenda o plegaria): si todavia no hay patron, ese dios pasa a serlo. */
    public static void addFavorAltar(ServerPlayer player, Deity deity, int amount) {
        if (amount > 0 && deity != Deity.TEMPO && patron(player) == null && getFavorRace(player) != Race.HEREJE) {
            setPatron(player, deity);
        }
        addFavor(player, deity, amount);
    }

    private static Race getFavorRace(ServerPlayer player) {
        return player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
    }

    /** Suma (o resta) favor aplicando las reglas del dios patron: lo que sube de los dioses que no son el patron sube muy poco, y cuando
     *  sube el patron baja el de su opuesto. Sin patron todavia, el primer dios que llega a PATRON_THRESHOLD lo pasa a ser. */
    public static void addFavor(ServerPlayer player, Deity deity, int amount) {
        Deity patron = patron(player);
        float scale = 1.0F;
        if (amount > 0 && patron != null && deity != patron) {
            scale = deity == opposite(patron) ? OPPOSITE_GAIN : sameCamp(patron, deity) ? SAME_CAMP_GAIN : OTHER_CAMP_GAIN;
        }
        int delta = amount;
        if (scale < 1.0F) {
            // lo que da menos de 1 punto se junta hasta completar uno
            net.minecraft.nbt.CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            float total = amount * scale + persisted.getFloat(FRAC_KEY + deity.name());
            delta = (int) total;
            persisted.putFloat(FRAC_KEY + deity.name(), total - delta);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        if (delta != 0) {
            addFavorRaw(player, deity, delta);
        }
        if (amount > 0 && patron != null && deity == patron) {
            addFavorRaw(player, opposite(deity), -Math.max(1, Math.round(amount * OPPOSITE_LOSS)));
        } else if (patron == null && amount > 0 && deity != Deity.TEMPO && getFavor(player, deity) >= PATRON_THRESHOLD
                && getFavorRace(player) != Race.HEREJE) {
            setPatron(player, deity);
        }
    }

    /** Suma (o resta, con amount negativo) favor de un dios, ya con los modificadores de raza aplicados y SIN reglas de patron (comandos). */
    public static void addFavorRaw(ServerPlayer player, Deity deity, int amount) {
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
                data.setFavor(deity, Math.max(data.getFavor(deity), startingFavorFor(race, deity)));
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
