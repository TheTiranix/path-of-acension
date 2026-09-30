// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pedido de alejandr0: la Breezebreaker (Celestisynth) no tiene debuffs. Su onPlayerHurt original hace inmune a la caida
 * pero multiplica por 1.65 (2.3 en la lista de alejandr0) el daño de cualquier otra fuente; aca se conserva solo la
 * inmunidad a la caida y se saca el aumento.
 */
@Mixin(targets = "com.aqutheseal.celestisynth.common.item.weapons.BreezebreakerItem", remap = false)
public abstract class BreezebreakerItemMixin {

    @Inject(method = "onPlayerHurt", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$noDamageDebuff(LivingHurtEvent event, ItemStack stack, CallbackInfo ci) {
        if (event.getSource() == event.getEntity().damageSources().fall()) {
            event.setCanceled(true);
        }
        ci.cancel();
    }
}
