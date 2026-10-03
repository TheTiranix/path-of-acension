// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Set wintry (pedido de alejandr0, "como fiery pero frio"): las armas y herramientas congelan al enemigo que golpean, y cada pieza de la
 * armadura congela a quien te hace daño (cuantas mas piezas, mas fuerte y mas largo el congelamiento).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class WintryEffects {
    private static final String PREFIX = "testamentodelacarne:wintry_";

    private WintryEffects() {
    }

    private static boolean isWintry(net.minecraft.world.item.ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.toString().startsWith(PREFIX);
    }

    /** Congela: lentitud fuerte, la escarcha de la pantalla (ticks de congelacion) y particulas de nieve. */
    private static void freeze(LivingEntity target, int ticks, int slowAmplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, slowAmplifier, false, true));
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    14, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.02);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.4F, 1.6F);
        }
    }

    /** Descripcion en el tooltip: armas/herramientas, armadura, y los materiales nuevos. */
    @SubscribeEvent
    public static void onTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !id.getNamespace().equals("testamentodelacarne")) {
            return;
        }
        String path = id.getPath();
        String key = null;
        if (path.startsWith("wintry_")) {
            boolean armor = path.endsWith("helmet") || path.endsWith("chestplate") || path.endsWith("leggings") || path.endsWith("boots");
            key = armor ? "wintry_armor" : path.equals("wintry_gem") || path.equals("wintry_essence") ? (path.equals("wintry_essence") ? "wintry_essence" : null) : "wintry_weapon";
        } else if (path.equals("phoenix_ingot") || path.equals("valkyrie_ingot")) {
            key = path;
        }
        if (key != null) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.testamentodelacarne." + key)
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        // armas: golpe directo con algo wintry en la mano
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != victim && isWintry(attacker.getMainHandItem())) {
            freeze(victim, 60, 3);
        }
        // armadura: quien te lastima (cuerpo a cuerpo o a distancia) queda congelado
        if (victim instanceof Player player && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
            int pieces = 0;
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (isWintry(player.getItemBySlot(slot))) {
                    pieces++;
                }
            }
            if (pieces > 0) {
                freeze(attacker, 30 + 20 * pieces, Math.min(4, pieces));
            }
        }
    }
}
