// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pedido de alejandr0: las habilidades de la Keres cuestan mucha mas sangre. Mientras la canalizas, el sacrificio original era 1 de
 * vida cada 30 ticks (cada 15 pasados los 10 segundos). Ahora: 5 de vida por segundo antes de los 10 segundos (un tick cada 20) y,
 * pasados los 10 segundos, 7.5 de vida cada 15 ticks = 10 por segundo (100 de vida cada 10 segundos).
 */
@Mixin(targets = "com.aqutheseal.celestisynth.common.item.weapons.KeresItem", remap = false)
public abstract class KeresItemMixin {
    private static float testamentodelacarne$cost = 1.0F;

    @Inject(method = "m_5929_", at = @At("HEAD"), remap = false, require = 0)
    private void testamentodelacarne$costStart(Level level, LivingEntity entity, ItemStack stack, int remaining, CallbackInfo ci) {
        int elapsed = ((Item) (Object) this).getUseDuration(stack) - remaining;
        testamentodelacarne$cost = elapsed >= 200 ? 7.5F : 5.0F;
    }

    /** Cada cuanto se cobra: 30 ticks pasan a 20 (5 de vida por segundo). */
    @ModifyConstant(method = "m_5929_", constant = @Constant(intValue = 30), remap = false, require = 0)
    private int testamentodelacarne$interval(int original) {
        return 20;
    }

    /** Cuanto se cobra por vez (la primera constante 1.0 del metodo; la segunda es la saturacion que se suma). */
    @ModifyConstant(method = "m_5929_", constant = @Constant(floatValue = 1.0F, ordinal = 0), remap = false, require = 0)
    private float testamentodelacarne$amount(float original) {
        return testamentodelacarne$cost;
    }
}
