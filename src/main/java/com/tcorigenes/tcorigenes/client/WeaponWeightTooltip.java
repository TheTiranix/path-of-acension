package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.core.WeaponWeights;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Muestra "Destreza requerida: N" en el tooltip de armas y escudos (ver WeaponWeights). */
public final class WeaponWeightTooltip {
    private WeaponWeightTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        int weight = WeaponWeights.weightOf(event.getItemStack());
        if (weight > 0) {
            event.getToolTip().add(Component.literal("Destreza requerida: " + weight).withStyle(
                    weight > WeaponWeights.THRESHOLD ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
    }
}
