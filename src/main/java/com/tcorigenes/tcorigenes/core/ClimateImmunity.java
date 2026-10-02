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

    private static boolean wearsSet(Player player, String namespace, String prefix) {
        for (EquipmentSlot slot : ARMOR) {
            if (!matches(player.getItemBySlot(slot), namespace, prefix) && !matches(inCurios(player, slot), namespace, prefix)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(ItemStack stack, String namespace, String prefix) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix);
    }

    /** Pedido de alejandr0: las piezas que protegen del clima tambien valen en los slots head/chest/legs/feet de Curios. */
    public static ItemStack inCurios(Player player, EquipmentSlot slot) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            return ItemStack.EMPTY;
        }
        return CuriosGloves.first(player, switch (slot) {
            case HEAD -> "head_clothes";
            case CHEST -> "chest_clothes";
            case LEGS -> "legs_clothes";
            default -> "feet_clothes";
        });
    }

    public static boolean wearsSolarSet(Player player) {
        return wearsSet(player, "celestisynth", SOLAR);
    }

    public static boolean wearsLunarSet(Player player) {
        return wearsSet(player, "celestisynth", LUNAR);
    }

    public static boolean wearsYetiSet(Player player) {
        return wearsSet(player, "twilightforest", "yeti_");
    }

    /** Set completo de Ignitium (Cataclysm): inmune al fuego y al calor (pedido de alejandr0). */
    public static boolean wearsIgnitiumSet(Player player) {
        return wearsSet(player, "cataclysm", "ignitium_");
    }

    /** Set completo de Skymetal (antes Cursium, Cataclysm): inmune al daño por caida. */
    public static boolean wearsSkymetalSet(Player player) {
        return wearsSet(player, "cataclysm", "cursium_");
    }

    /** Set completo de Ghost Warrior (EEEAB's Mobs): inmune al wither. */
    public static boolean wearsGhostWarriorSet(Player player) {
        return wearsSet(player, "eeeabsmobs", "ghost_warrior_");
    }

    /** Inmune al wither: Divine Lunar Armor y Ghost Warrior. */
    public static boolean isWitherImmune(Player player) {
        return wearsLunarSet(player) || wearsGhostWarriorSet(player);
    }

    /** Inmunidad al frio: sets divinos (solar/lunar) y set completo de Yeti. */
    public static boolean isColdImmune(Player player) {
        return wearsSolarSet(player) || wearsLunarSet(player) || wearsYetiSet(player);
    }

    public static boolean wearsImmunitySet(Player player) {
        return wearsSolarSet(player) || wearsLunarSet(player);
    }

    public static boolean isFireImmune(Player player) {
        return player.hasEffect(MobEffects.FIRE_RESISTANCE) || player.fireImmune() || wearsSolarSet(player)
                || wearsIgnitiumSet(player);
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
        if (isWitherImmune(player)) {
            player.removeEffect(MobEffects.WITHER);
        }
        if (wearsIgnitiumSet(player)) {
            refresh(player, MobEffects.FIRE_RESISTANCE);
        }
        if (!solar && !lunar) {
            if (wearsYetiSet(player)) {
                refresh(player, ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.fromNamespaceAndPath("toughasnails", "ice_resistance")));
            }
            return;
        }
        if (solar) {
            refresh(player, MobEffects.FIRE_RESISTANCE);
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
        } else if (source.is(DamageTypes.FREEZE) && isColdImmune(player)) {
            event.setCanceled(true);
        } else if (source.is(DamageTypes.WITHER) && isWitherImmune(player)) {
            event.setCanceled(true);
        } else if (source.typeHolder().unwrapKey().map(k -> k.location().getPath().equals("hyperthermia")).orElse(false)
                && isHeatImmune(player)) {
            event.setCanceled(true);
        }
    }

    /** Skymetal: sin daño de caida. */
    @SubscribeEvent
    public static void onFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && wearsSkymetalSet(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance().getEffect() == MobEffects.WITHER
                && isWitherImmune(player)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
