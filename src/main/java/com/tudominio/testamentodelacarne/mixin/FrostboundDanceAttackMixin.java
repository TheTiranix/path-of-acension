// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import com.aqutheseal.celestisynth.common.attack.base.WeaponAttackInstance;
import com.tcorigenes.tcorigenes.weapon.SkillDamageScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pedido de alejandr0: -60% al daño de la habilidad de solo click derecho de Frostbound (ver SkillDamageScale). */
@Mixin(targets = "com.aqutheseal.celestisynth.common.attack.frostbound.FrostboundDanceAttack", remap = false)
public abstract class FrostboundDanceAttackMixin {

    @Inject(method = "tickAttack", at = @At("HEAD"), remap = false, require = 0)
    private void testamentodelacarne$scaleStart(CallbackInfo ci) {
        SkillDamageScale.begin(((WeaponAttackInstance) (Object) this).player, 0.4F);
    }

    @Inject(method = "tickAttack", at = @At("RETURN"), remap = false, require = 0)
    private void testamentodelacarne$scaleEnd(CallbackInfo ci) {
        SkillDamageScale.end();
    }
}
