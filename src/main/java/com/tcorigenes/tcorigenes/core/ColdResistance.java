// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido de alejandr0: cada pieza de armadura puesta (de la que sea) hace al jugador un 10% mas resistente al clima frio, y
 * otro 10% por tener guantes (slot "hands" de Curios: los guantes del Aether y demas). Se aplica como reduccion del daño
 * de hipotermia de Tough As Nails y del daño de congelacion; con las 4 piezas y guantes da 50%.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ColdResistance {
    public static final double PER_PIECE = 0.10;
    public static final double GLOVES = 0.10;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ColdResistance() {
    }

    public static double resistanceOf(Player player) {
        double resistance = 0.0;
        for (EquipmentSlot slot : ARMOR) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                resistance += PER_PIECE;
            }
        }
        if (hasGloves(player)) {
            resistance += GLOVES;
        }
        return Math.min(1.0, resistance);
    }

    private static boolean hasGloves(Player player) {
        if (!ModList.get().isLoaded("curios")) {
            return false;
        }
        return CuriosGloves.has(player);
    }

    private static boolean isCold(DamageSource source) {
        if (source.is(DamageTypes.FREEZE)) {
            return true;
        }
        return source.typeHolder().unwrapKey().map(key -> key.location().getPath().equals("hypothermia")).orElse(false);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && isCold(event.getSource())) {
            double resistance = resistanceOf(player);
            if (resistance > 0.0) {
                event.setAmount((float) (event.getAmount() * (1.0 - resistance)));
            }
        }
    }
}
