// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Pedido de alejandr0: la mitad de las particulas de Celestisynth (la cantidad pedida en cada sendParticles, minimo 1). */
@Mixin(targets = "com.aqutheseal.celestisynth.util.ParticleUtil", remap = false)
public abstract class CelestisynthParticleUtilMixin {

    @ModifyVariable(method = "sendParticles", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private static int testamentodelacarne$halfCount(int count) {
        return count <= 1 ? count : Math.max(1, count / 2);
    }
}
