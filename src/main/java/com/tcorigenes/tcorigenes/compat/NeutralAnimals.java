// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import java.util.Set;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Vaca, oveja, cerdo y caballo adultos pasan de pasivos a neutrales: si les pegas dejan de
 * huir y te atacan, y avisan a la manada cercana. Como esos animales no tienen atributo de
 * daño ni meta de ataque, se los agrega (HurtByTargetGoal solo NO alcanza: elegiria objetivo
 * pero nunca pegaria, y MeleeAttackGoal sin ATTACK_DAMAGE crashea).
 */
public final class NeutralAnimals {
    private static final Set<EntityType<?>> TYPES = Set.of(
            EntityType.COW, EntityType.MOOSHROOM, EntityType.SHEEP, EntityType.PIG, EntityType.HORSE);

    private NeutralAnimals() {
    }

    @EventBusSubscriber(modid = "tcorigenes", bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void onAttributes(EntityAttributeModificationEvent event) {
            for (EntityType<?> type : TYPES) {
                @SuppressWarnings("unchecked")
                EntityType<? extends net.minecraft.world.entity.LivingEntity> living = (EntityType<? extends net.minecraft.world.entity.LivingEntity>) type;
                if (!event.has(living, Attributes.ATTACK_DAMAGE)) {
                    event.add(living, Attributes.ATTACK_DAMAGE, 3.0);
                }
            }
        }
    }

    @EventBusSubscriber(modid = "tcorigenes")
    public static final class ForgeBus {
        @SubscribeEvent
        public static void onJoin(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob mob)
                    || !TYPES.contains(mob.getType()) || mob.isBaby()) {
                return;
            }
            GoalSelector goals = ObfuscationReflectionHelper.getPrivateValue(Mob.class, mob, "f_21345_");
            GoalSelector targets = ObfuscationReflectionHelper.getPrivateValue(Mob.class, mob, "f_21346_");
            if (goals == null || targets == null) {
                return;
            }
            goals.removeAllGoals(goal -> goal instanceof PanicGoal);
            goals.addGoal(1, new MeleeAttackGoal(mob, 1.3, true));
            targets.addGoal(1, new HurtByTargetGoal(mob).setAlertOthers());
        }
    }
}
