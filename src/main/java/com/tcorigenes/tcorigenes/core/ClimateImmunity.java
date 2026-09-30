// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedidos de alejandr0. Divine Solar Armor (set completo de Solar Crystal): inmune al fuego, al frio y al calor.
 * Divine Lunar Armor (set completo de Lunar Stone): inmune al frio, al calor y al wither (NO al fuego).
 * Ademas, toda inmunidad al fuego (efecto, set solar, criatura inmune) da inmunidad al calor (hipertermia de Tough As
 * Nails y su desgaste de sed extra, ver TanCompat#isExtremeHeat).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ClimateImmunity {
    private static final String SOLAR = "solar_crystal_";
    private static final String LUNAR = "lunar_stone_";
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ClimateImmunity() {
    }

    private static boolean wearsSet(Player player, String prefix) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !id.getNamespace().equals("celestisynth") || !id.getPath().startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    public static boolean wearsSolarSet(Player player) {
        return wearsSet(player, SOLAR);
    }

    public static boolean wearsLunarSet(Player player) {
        return wearsSet(player, LUNAR);
    }

    public static boolean wearsImmunitySet(Player player) {
        return wearsSolarSet(player) || wearsLunarSet(player);
    }

    public static boolean isFireImmune(Player player) {
        return player.hasEffect(MobEffects.FIRE_RESISTANCE) || player.fireImmune() || wearsSolarSet(player);
    }

    public static boolean isHeatImmune(Player player) {
        return isFireImmune(player) || wearsLunarSet(player);
    }

    private static void refresh(Player player, MobEffect effect) {
        if (effect != null) {
            player.addEffect(new MobEffectInstance(effect, 100, 0, true, false, false));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 20 != 0) {
            return;
        }
        Player player = event.player;
        boolean solar = wearsSolarSet(player);
        boolean lunar = wearsLunarSet(player);
        if (!solar && !lunar) {
            return;
        }
        if (solar) {
            refresh(player, MobEffects.FIRE_RESISTANCE);
        }
        if (lunar) {
            player.removeEffect(MobEffects.WITHER);
        }
        refresh(player, ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.fromNamespaceAndPath("toughasnails", "ice_resistance")));
        refresh(player, ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.fromNamespaceAndPath("toughasnails", "climate_clemency")));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FIRE) && isFireImmune(player)) {
            event.setCanceled(true);
        } else if (source.is(DamageTypes.FREEZE) && wearsImmunitySet(player)) {
            event.setCanceled(true);
        } else if (source.is(DamageTypes.WITHER) && wearsLunarSet(player)) {
            event.setCanceled(true);
        } else if (source.typeHolder().unwrapKey().map(k -> k.location().getPath().equals("hyperthermia")).orElse(false)
                && isHeatImmune(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance().getEffect() == MobEffects.WITHER
                && wearsLunarSet(player)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
