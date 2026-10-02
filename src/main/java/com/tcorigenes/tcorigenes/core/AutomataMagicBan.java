// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * El Autómata tiene prohibido usar magia (pedido de alejandr0): no puede interactuar con los items de
 * los mods de magia del pack (varitas, libros de hechizos, pergaminos, etc. de Ars Nouveau e Iron's
 * Spellbooks). Cancela el click derecho, con o sin bloque apuntado, con cualquier item de esos mods.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class AutomataMagicBan {
    private static final String[] MAGIC_NAMESPACES = {"ars_nouveau", "irons_spellbooks"};

    private AutomataMagicBan() {
    }

    private static boolean isMagicItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        for (String namespace : MAGIC_NAMESPACES) {
            if (id.getNamespace().equals(namespace)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAutomata(net.minecraft.world.entity.player.Player player) {
        return player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace() == Race.AUTOMATA).orElse(false);
    }

    private static void block(PlayerInteractEvent event) {
        if (!event.getEntity().level().isClientSide() && isAutomata(event.getEntity()) && isMagicItem(event.getItemStack())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("pa.msg.b7d930a5f8"), true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        block(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        block(event);
    }
}
