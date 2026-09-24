// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Armas "eliminadas" del pack (tipos repetidos entre Variant Tools y Basic Weapons: se deja la daga de
 * Variant Tools y la glaive de Basic Weapons; y se sacan las picas, hachas grandes y hachas de mano).
 * Los items de otros mods no se pueden borrar del registro: se sacan de las recetas (KubeJS), de la
 * pestaña creativa y de JEI, y si llegan a aparecer en un inventario (loot, cofres) se descartan.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class RemovedItems {
    public static final Set<ResourceLocation> IDS = new HashSet<>();

    static {
        String[] materials = {"wooden", "stone", "iron", "golden", "diamond", "netherite"};
        for (String material : materials) {
            for (String type : new String[] {"pike", "greataxe", "handaxe", "glaive"}) {
                IDS.add(ResourceLocation.fromNamespaceAndPath("vtaw_mw", material + "_" + type));
            }
            IDS.add(ResourceLocation.fromNamespaceAndPath("basicweapons", material + "_dagger"));
        }
    }

    private RemovedItems() {
    }

    public static boolean isRemoved(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && IDS.contains(id);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        for (List<ItemStack> list : List.of(player.getInventory().items, player.getInventory().offhand, player.getInventory().armor)) {
            for (int i = 0; i < list.size(); i++) {
                if (!list.get(i).isEmpty() && isRemoved(list.get(i))) {
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }
    }

    /** Fuera de la pestaña creativa (y por lo tanto del buscador del creativo). */
    @EventBusSubscriber(modid = "tcorigenes", bus = EventBusSubscriber.Bus.MOD)
    public static final class CreativeHider {
        private CreativeHider() {
        }

        @SubscribeEvent
        public static void onTabContents(BuildCreativeModeTabContentsEvent event) {
            List<ItemStack> toRemove = new java.util.ArrayList<>();
            for (var entry : event.getEntries()) {
                if (isRemoved(entry.getKey())) {
                    toRemove.add(entry.getKey());
                }
            }
            toRemove.forEach(stack -> event.getEntries().remove(stack));
        }
    }
}
