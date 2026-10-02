// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import toughasnails.api.temperature.TemperatureHelper;
import toughasnails.api.temperature.TemperatureLevel;
import toughasnails.api.thirst.IThirst;
import toughasnails.api.thirst.ThirstHelper;

/** Unico lugar que toca clases de Tough As Nails, para que el mod no crashee si no esta instalado. */
public final class TanCompat {
    private TanCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("toughasnails");
    }

    /**
     * Registra el modificador de temperatura del jugador (esfuerzo, abrigo y ropa de hielo; ver ClimateTuning). Va despues de que TAN calcula
     * el nivel por bioma, hora y bloques cercanos.
     */
    public static void registerClimate() {
        TemperatureHelper.registerPlayerTemperatureModifier((player, level) -> {
            TemperatureLevel result = level;
            double exertion = com.tcorigenes.tcorigenes.core.ClimateTuning.exertion(player);
            if (exertion >= 0.09 && result.ordinal() < TemperatureLevel.HOT.ordinal()) {
                result = result.increment(1); // el esfuerzo sostenido entra en calor
            }
            if (result.ordinal() < TemperatureLevel.NEUTRAL.ordinal()) {
                double warmth = com.tcorigenes.tcorigenes.core.ClimateTuning.warmth(player);
                int steps = warmth >= 0.65 ? 2 : warmth >= 0.30 ? 1 : 0;
                for (int i = 0; i < steps && result.ordinal() < TemperatureLevel.NEUTRAL.ordinal(); i++) {
                    result = result.increment(1);
                }
            } else if (result.ordinal() > TemperatureLevel.NEUTRAL.ordinal()) {
                double cool = com.tcorigenes.tcorigenes.core.ClimateTuning.coolness(player);
                int steps = cool >= 0.9 ? 2 : cool >= 0.5 ? 1 : 0;
                for (int i = 0; i < steps && result.ordinal() > TemperatureLevel.NEUTRAL.ordinal(); i++) {
                    result = result.decrement(1);
                }
            }
            return result;
        });
    }

    /** Calor extremo segun Tough As Nails (nivel HOT). */
    public static boolean isExtremeHeat(Player player) {
        return TemperatureHelper.isTemperatureEnabled()
                && !com.tcorigenes.tcorigenes.core.ClimateImmunity.isHeatImmune(player)
                && TemperatureHelper.getTemperatureData(player).getLevel() == TemperatureLevel.HOT;
    }

    public static float thirstExhaustion(Player player) {
        return ThirstHelper.getThirst(player).getExhaustion();
    }

    /** Gotas de sed actuales (0 a 20), la misma escala que la barra de hambre. */
    public static int thirstDrops(Player player) {
        return ThirstHelper.getThirst(player).getThirst();
    }

    public static void addThirstExhaustion(Player player, float amount) {
        ThirstHelper.getThirst(player).addExhaustion(amount);
    }

    /** Deja la sed y la hidratacion siempre al maximo; la barra sigue existiendo pero nunca baja. */
    public static void fillThirst(Player player) {
        IThirst thirst = ThirstHelper.getThirst(player);
        if (thirst.getThirst() < 20) {
            thirst.setThirst(20);
        }
        if (thirst.getHydration() < 20.0F) {
            thirst.setHydration(20.0F);
        }
        thirst.setExhaustion(0.0F);
    }
}
