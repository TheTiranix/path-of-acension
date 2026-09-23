// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Hace que las armas marcadas "twoHanded" en WeaponDamageOverrides (la Valkyrie Lance, por ahora)
 * ocupen las dos manos de verdad: no hay un concepto nativo de "arma a dos manos" para items
 * modeados en Forge 1.20.1, asi que mientras una de estas esta en la mano principal, cualquier
 * cosa que aparezca en la secundaria se devuelve sola al inventario (o se tira si no entra).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class TwoHandedWeapons {
    private TwoHandedWeapons() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty()) {
            return;
        }
        ResourceLocation mainhandId = ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem());
        if (mainhandId == null || !WeaponDamageOverrides.isTwoHanded(mainhandId)) {
            return;
        }
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(offhand);
    }
}
