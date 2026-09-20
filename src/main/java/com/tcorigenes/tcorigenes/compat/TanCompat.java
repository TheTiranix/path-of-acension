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

    /** Calor extremo segun Tough As Nails (nivel HOT). */
    public static boolean isExtremeHeat(Player player) {
        return TemperatureHelper.isTemperatureEnabled()
                && TemperatureHelper.getTemperatureData(player).getLevel() == TemperatureLevel.HOT;
    }

    public static float thirstExhaustion(Player player) {
        return ThirstHelper.getThirst(player).getExhaustion();
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
