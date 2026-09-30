// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: los nombres de armas y herramientas van sin color (excepto los items de Celestisynth), para
 * poner despues una escala de colores por nivel. La rareza ya se pone en comun (ver ItemStackRarityMixin); esto limpia
 * ademas los colores que agregan otros mods al nombre (ej. las afijos de Apotheosis).
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class PlainWeaponNames {
    private PlainWeaponNames() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        var item = stack.getItem();
        if (!(item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem)) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || id.getNamespace().equals("celestisynth")) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) {
            return;
        }
        tooltip.set(0, Component.literal(tooltip.get(0).getString()).withStyle(ChatFormatting.WHITE));
    }
}
