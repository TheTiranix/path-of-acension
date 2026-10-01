// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pedido de alejandr0: la proteccion contra el wither del set de dark metal es solo del 50%. Born in Chaos le quitaba el efecto Wither
 * al jugador con el set completo en cada tick (inmunidad total); aca no se lo quita y el daño del wither se reduce a la mitad en
 * DarkMetalWither.
 */
@Mixin(targets = "net.mcreator.borninchaosv.procedures.DarkMetalArmorSobytiieTaktovShliemaProcedure", remap = false)
public abstract class DarkMetalArmorTickMixin {

    @Redirect(method = "execute(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_21195_(Lnet/minecraft/world/effect/MobEffect;)Z", remap = false),
            remap = false, require = 0)
    private static boolean testamentodelacarne$keepWither(LivingEntity entity, MobEffect effect) {
        return false;
    }
}
