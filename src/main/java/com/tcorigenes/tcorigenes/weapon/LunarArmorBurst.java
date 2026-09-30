// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import com.tudominio.elementaldamage.ElementalDamageSource;
import com.tudominio.elementaldamage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Divine Lunar Armor (pedido de alejandr0): reemplaza el 1/5 de reducir el golpe recibido de Celestisynth por una
 * explosion en area al recibir daño, igual a la de la armadura solar pero de daño LUNAR y con particulas violetas.
 * Cada pieza suma daño (mismo 1.2 por pieza que la solar). Solo lastima mobs hostiles.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class LunarArmorBurst {
    private static final float DAMAGE_PER_PIECE = 1.2F;
    private static final double RADIUS = 2.0;

    private LunarArmorBurst() {
    }

    private static int pieces(Player player) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null && id.getNamespace().equals("celestisynth") && id.getPath().startsWith("lunar_stone_")) {
                count++;
            }
        }
        return count;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int count = pieces(player);
        if (count <= 0) {
            return;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.8F);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0 * i / 24.0;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            level.sendParticles(ParticleTypes.WITCH, player.getX() + dx * RADIUS * 0.6, player.getY() + 1.0, player.getZ() + dz * RADIUS * 0.6,
                    1, dx * 0.15, 0.05, dz * 0.15, 0.02);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX() + dx * RADIUS, player.getY() + 0.6, player.getZ() + dz * RADIUS,
                    1, 0.0, 0.1, 0.0, 0.02);
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS))) {
            if (target != player && target instanceof Enemy && target.isAlive()) {
                ElementalDamageSource.hurt(target, ModDamageTypes.LUNAR, player, DAMAGE_PER_PIECE * count);
            }
        }
    }
}
