// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import com.tcorigenes.tcorigenes.attributes.OriginBonuses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Armaduras marinas (pedido de alejandr0): Depth (Sea Dwellers) y Tide Guardian (Ice and Fire) restan 10% de velocidad de movimiento por pieza
 * mientras no estas en el agua. Ademas la Tide Guardian ya no da el efecto de fuerza (Strength) del set completo.
 * El +2 de daño de agua del set Depth completo (dentro del agua) esta en ArmorSetBonus.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class SeaArmorTweaks {
    private static final double SLOW_PER_PIECE = 0.10;

    private SeaArmorTweaks() {
    }

    private static boolean isSea(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String s = id.toString();
        return s.startsWith("seadwellers:depth_") || s.startsWith("iceandfire:tide_");
    }

    private static boolean isTide(ResourceLocation id) {
        return id != null && id.toString().startsWith("iceandfire:tide_");
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 5 != 0) {
            return;
        }
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (isSea(ForgeRegistries.ITEMS.getKey(player.getItemBySlot(slot).getItem()))) {
                pieces++;
            }
        }
        boolean inWater = player.isInWater() || player.isUnderWater();
        OriginBonuses.set(player, "sea_armor", Attributes.MOVEMENT_SPEED, pieces == 0 || inWater ? 0.0 : -SLOW_PER_PIECE * pieces);
    }

    /** El efecto de fuerza del set Tide Guardian lo aplica la armadura sin particulas (los de pociones y demas si las muestran). */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player) || event.getEffectInstance().getEffect() != MobEffects.DAMAGE_BOOST
                || event.getEffectInstance().isVisible()) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (isTide(ForgeRegistries.ITEMS.getKey(player.getItemBySlot(slot).getItem()))) {
                event.setResult(Event.Result.DENY);
                return;
            }
        }
    }
}
