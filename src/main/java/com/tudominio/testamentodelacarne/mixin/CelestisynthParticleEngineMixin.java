// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pedido de alejandr0: las particulas de Celestisynth a un 25% de su tamaño y la mitad de cantidad. Este lado (cliente)
 * achica las particulas propias del mod y descarta una de cada dos; la cantidad que manda el servidor se corta en
 * CelestisynthParticleUtilMixin.
 */
@Mixin(ParticleEngine.class)
public abstract class CelestisynthParticleEngineMixin {
    private static boolean testamentodelacarne$skipNext;

    @Inject(method = "m_107344_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$shrinkCelestisynth(Particle particle, CallbackInfo ci) {
        if (!particle.getClass().getName().startsWith("com.aqutheseal.celestisynth.")) {
            return;
        }
        testamentodelacarne$skipNext = !testamentodelacarne$skipNext;
        if (testamentodelacarne$skipNext) {
            ci.cancel();
            return;
        }
        particle.scale(0.25F);
    }
}
