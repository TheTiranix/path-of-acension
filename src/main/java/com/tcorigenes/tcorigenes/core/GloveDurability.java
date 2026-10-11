// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido de alejandr0: los guantes de gravitite ya no son irrompibles y los de netherite tambien tienen durabilidad (la de la armadura de
 * gravitite). Cada golpe cuerpo a cuerpo gasta 1 de durabilidad a los guantes puestos que la tengan (los demas guantes siguen sin gastarse).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class GloveDurability {
    private GloveDurability() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F || !(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()
                || event.getSource().getDirectEntity() != player) {
            return;
        }
        com.tcorigenes.tcorigenes.compat.CuriosCompat.damageItemsInSlot(player, "hands");
    }
}
