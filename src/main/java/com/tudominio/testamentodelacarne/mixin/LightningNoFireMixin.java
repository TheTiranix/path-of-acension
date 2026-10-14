// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pedido de alejandr0: los rayos (de la espada de dragonsteel de rayo y del daño elemental de aire) no prenden fuego: solo hacen el daño del
 * rayo. Entity#thunderHit prendia fuego (8 segundos) y despues daba 5 de daño; aca queda solo el daño.
 */
@Mixin(Entity.class)
public abstract class LightningNoFireMixin {

    @Inject(method = "m_8038_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$lightningWithoutFire(ServerLevel level, LightningBolt bolt, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        self.hurt(self.damageSources().lightningBolt(), 5.0F);
        ci.cancel();
    }
}
