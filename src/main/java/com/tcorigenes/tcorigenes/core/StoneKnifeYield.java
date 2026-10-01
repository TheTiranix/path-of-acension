// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: el cuchillo de carnicero de piedra da un tercio de lo que da el de hierro. Butcher's Delight suelta lo mismo con
 * cualquier cuchillo, asi que se marca el corte hecho con el cuchillo de piedra (click derecho sobre un animal colgado) y los items de
 * Butcher's Delight que aparezcan ese mismo tick cerca del jugador se reducen a la tercera parte (minimo 1).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class StoneKnifeYield {
    private static final ResourceLocation KNIFE = ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "stone_butcher_knife");
    private static final Map<UUID, Long> CUTS = new HashMap<>();

    private StoneKnifeYield() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && KNIFE.equals(ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem()))) {
            CUTS.put(player.getUUID(), player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onItemSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity drop) || CUTS.isEmpty()) {
            return;
        }
        ItemStack stack = drop.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getNamespace().equals("butchersdelight")) {
            return;
        }
        long now = event.getLevel().getGameTime();
        for (var entry : CUTS.entrySet()) {
            if (now - entry.getValue() > 1) {
                continue;
            }
            Player player = event.getLevel().getPlayerByUUID(entry.getKey());
            if (player != null && player.distanceToSqr(drop) < 64.0 && stack.getCount() > 1) {
                stack.setCount(Math.max(1, stack.getCount() / 3));
                return;
            }
        }
    }
}
