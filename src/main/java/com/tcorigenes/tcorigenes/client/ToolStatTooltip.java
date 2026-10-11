// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.core.Tr;
import com.tcorigenes.tcorigenes.weapon.ToolBalance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/** Velocidad y nivel de minado en el tooltip de las herramientas de zafiro, dark metal, netherite, gravitite, fiery y wintry. */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class ToolStatTooltip {
    private ToolStatTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ToolBalance.Stat stat = ToolBalance.statOf(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()));
        if (stat == null) {
            return;
        }
        String speed = String.format(java.util.Locale.ROOT, "%.2f", stat.speed());
        event.getToolTip().add(Component.literal(Tr.f("Velocidad de minado: %s", speed)).withStyle(ChatFormatting.DARK_GREEN));
        event.getToolTip().add(Component.literal(Tr.f("Nivel de minado: %s", stat.level())).withStyle(ChatFormatting.DARK_GREEN));
    }
}
