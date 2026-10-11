// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.core.WeaponWeights;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Muestra "Fuerza requerida: N" en el tooltip de armas, escudos y armaduras pesadas (ver WeaponWeights y HeavyArmor). */
public final class WeaponWeightTooltip {
    private WeaponWeightTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (com.tcorigenes.tcorigenes.core.HeavyArmor.isHeavy(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("pa.msg.1a5219fcbb", com.tcorigenes.tcorigenes.core.HeavyArmor.REQUIREMENT)
                    .withStyle(ChatFormatting.GOLD));
        }
        int weight = WeaponWeights.weightOf(event.getItemStack());
        if (weight > 0) {
            event.getToolTip().add(Component.translatable("pa.msg.1a5219fcbb", weight).withStyle(
                    weight > 75 ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
    }
}
