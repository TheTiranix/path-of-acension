// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.damagesource.CombatRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pedido de alejandr0: la reduccion de daño de la proteccion (armadura) de Minecraft esta limitada al 80% (a partir de 20 puntos
 * efectivos no mejora). Aca la formula sigue igual hasta ese punto y despues sigue mejorando sin limite: con e puntos efectivos
 * (e > 20) el daño recibido es 4/e del original (e = 20 -> 20%, e = 100 -> 4%, e = 1000 -> 0.4%).
 */
@Mixin(CombatRules.class)
public abstract class CombatRulesMixin {

    @Inject(method = "m_19272_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void testamentodelacarne$uncappedArmor(float damage, float armor, float toughness, CallbackInfoReturnable<Float> cir) {
        float effective = Math.max(armor * 0.2F, armor - damage / (2.0F + toughness / 4.0F));
        if (effective <= 20.0F) {
            cir.setReturnValue(damage * (1.0F - effective / 25.0F));
        } else {
            cir.setReturnValue(damage * 4.0F / effective);
        }
    }
}
