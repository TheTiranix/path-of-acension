// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.faction.ModEntityTypes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Invocacion real del Guerrero Anima (ver AnimaSpiritEntity): un genio de la lampara de color segun la raza del jugador que se
 * queda levitando donde se lo invoco durante 15s, y que golpea a los mobs hostiles cercanos. Un jugador tiene un solo espiritu a la
 * vez: invocar otro hace que el anterior se vaya con su animacion.
 */
public final class AnimaSpiritTracker {
    public static final int DURATION_TICKS = 20 * 15;
    private static final Map<UUID, AnimaSpiritEntity> ACTIVE = new HashMap<>();

    private AnimaSpiritTracker() {
    }

    /** Color del genio segun la raza (RGB). */
    public static int colorOf(Race race) {
        return switch (race) {
            case DEMONIO -> 0xFF3B2F;
            case ANGEL -> 0xBFEFFF;
            case SIERVO_DE_LA_LUNA -> 0x9B5BFF;
            case ENDER_WARRIOR -> 0xD84BFF;
            case STONE_GIANT -> 0xC28B4A;
            case AUTOMATA -> 0x33FFD0;
            case HEREJE -> 0x5CFF6B;
            case MALNACIDO -> 0xB3122B;
            case DEVOTO -> 0xFFD24A;
            default -> 0x55D6FF; // humano: celeste
        };
    }

    public static void summon(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AnimaSpiritEntity previous = ACTIVE.remove(player.getUUID());
        if (previous != null && previous.isAlive()) {
            previous.dismiss();
        }
        Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
        AnimaSpiritEntity spirit = ModEntityTypes.ANIMA_SPIRIT.get().create(serverLevel);
        if (spirit == null) {
            return;
        }
        spirit.setup(colorOf(race), DURATION_TICKS);
        spirit.moveTo(player.getX(), player.getY() + 0.1, player.getZ(), player.getYRot(), 0.0F);
        serverLevel.addFreshEntity(spirit);
        ACTIVE.put(player.getUUID(), spirit);
    }

    /** Antes lo recorria el tick global; ahora el espiritu se maneja solo. Solo limpia los que ya no existen. */
    public static void tick(ServerLevel level) {
        ACTIVE.values().removeIf(spirit -> !spirit.isAlive());
    }
}
