package com.tudominio.elementaldamage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Helper para infligir daño elemental desde items/hechizos/habilidades (fase 2). */
public final class ElementalDamageSource {
    private ElementalDamageSource() {
    }

    public static DamageSource create(ServerLevel level, ResourceKey<DamageType> type, Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker);
    }

    public static boolean hurt(LivingEntity target, ResourceKey<DamageType> type, Entity attacker, float amount) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return target.hurt(create(serverLevel, type, attacker), amount);
    }
}
