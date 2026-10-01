// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/** Pedido de alejandr0: con el set completo de dark metal el daño del wither se reduce un 50% (antes era inmune). */
@EventBusSubscriber(modid = "tcorigenes")
public final class DarkMetalWither {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private DarkMetalWither() {
    }

    private static boolean wearsSet(Player player) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !id.getNamespace().equals("born_in_chaos_v1") || !id.getPath().startsWith("dark_metal_armor_")) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && event.getSource().is(DamageTypes.WITHER)
                && wearsSet(player)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }
}
