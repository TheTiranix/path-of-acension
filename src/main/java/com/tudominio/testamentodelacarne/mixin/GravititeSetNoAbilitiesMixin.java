// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pedido de alejandr0: la armadura de gravitite ya no da el bonus de salto ni anula el daño por caida. Las dos habilidades del Aether se
 * activan solo si EquipmentUtil#hasFullGravititeSet es verdadero (salto: GravititeArmor#boostedJump, caida: ArmorHooks#fallCancellation).
 */
@Mixin(targets = "com.aetherteam.aether.item.EquipmentUtil", remap = false)
public abstract class GravititeSetNoAbilitiesMixin {

    @Inject(method = "hasFullGravititeSet", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void testamentodelacarne$noGravititeAbilities(net.minecraft.world.entity.LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
