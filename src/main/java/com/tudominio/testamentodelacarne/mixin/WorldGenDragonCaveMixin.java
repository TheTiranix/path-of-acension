// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pedido de alejandr0: los dragones recien empiezan a aparecer a 2000 bloques del spawn. Las cuevas de dragon (donde nacen)
 * no se generan mas cerca; los esqueletos de dragon no pasan por aca y siguen apareciendo en cualquier lado. El resto
 * de las estructuras peligrosas de Ice and Fire sigue con la distancia de su config (800).
 */
@Mixin(targets = "com.github.alexthe666.iceandfire.world.gen.WorldGenDragonCave", remap = false)
public abstract class WorldGenDragonCaveMixin {
    private static final double MIN_DISTANCE_FROM_SPAWN = 2000.0;

    @Inject(method = "m_142674_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$farFromSpawn(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        var data = context.level().getLevelData();
        double dx = context.origin().getX() - data.getXSpawn();
        double dz = context.origin().getZ() - data.getZSpawn();
        if (dx * dx + dz * dz < MIN_DISTANCE_FROM_SPAWN * MIN_DISTANCE_FROM_SPAWN) {
            cir.setReturnValue(false);
        }
    }
}
