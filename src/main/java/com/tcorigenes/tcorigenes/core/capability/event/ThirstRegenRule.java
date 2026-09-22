// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.compat.TanCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Hace falta un minimo de sed para regenerar vida pasivamente (el corazoncito verde de estar bien
 * alimentado): con menos de MIN_THIRST_DROPS gotas (sobre 20) esa regeneracion natural no ocurre.
 * No es el toggle "thirst_prevent_health_regen" de Tough As Nails (ese exige la sed LLENA, 20/20;
 * aca alcanza con un minimo).
 *
 * Forge no distingue "esta curacion es la regen natural" del resto (pociones, robo de vida, etc.), asi
 * que se usa un heuristico, mismo estilo que ya usa el mod para el agua (ver
 * ElementalDamageEvents#onLivingHeal): la regen natural (tanto la vanilla como la que Tough As Nails
 * reemplaza) siempre cura como mucho 1 corazon de una vez y solo puede pasar con el hambre en 18 o mas;
 * esas dos condiciones juntas casi no se dan en ningun otro tipo de curacion del pack.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ThirstRegenRule {
    private static final int MIN_THIRST_DROPS = 5;
    private static final float MAX_NATURAL_REGEN_AMOUNT = 1.0F;

    private ThirstRegenRule() {
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || !TanCompat.isLoaded()) {
            return;
        }
        if (event.getAmount() > MAX_NATURAL_REGEN_AMOUNT || player.getFoodData().getFoodLevel() < 18) {
            return; // no tiene pinta de ser la regen natural: no la tocamos
        }
        if (TanCompat.thirstDrops(player) < MIN_THIRST_DROPS) {
            event.setCanceled(true);
        }
    }
}
