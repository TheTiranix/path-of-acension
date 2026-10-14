// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: el bronce de Valoria se elimina y todo lo que lo daba (enemigos del Valoria, cofres, etc.) da el bronce de Mekanism.
 * Los items de Valoria que aparezcan sueltos (drops) o en el inventario del jugador se cambian por los de Mekanism, con la misma cantidad.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ItemConversion {
    /** item de Valoria -> item de Mekanism. */
    public static final Map<ResourceLocation, ResourceLocation> MAP = Map.of(
            ResourceLocation.fromNamespaceAndPath("valoria", "bronze_ingot"), ResourceLocation.fromNamespaceAndPath("mekanism", "ingot_bronze"),
            ResourceLocation.fromNamespaceAndPath("valoria", "bronze_nugget"), ResourceLocation.fromNamespaceAndPath("mekanism", "nugget_bronze"));

    private ItemConversion() {
    }

    private static ItemStack convert(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ResourceLocation target = MAP.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        Item item = target == null ? null : ForgeRegistries.ITEMS.getValue(target);
        return item == null ? stack : new ItemStack(item, stack.getCount());
    }

    @SubscribeEvent
    public static void onItemSpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity drop) {
            ItemStack converted = convert(drop.getItem());
            if (converted != drop.getItem()) {
                drop.setItem(converted);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        for (List<ItemStack> list : List.of(player.getInventory().items, player.getInventory().offhand)) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack converted = convert(list.get(i));
                if (converted != list.get(i)) {
                    list.set(i, converted);
                }
            }
        }
    }
}
