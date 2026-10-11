// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entomophobia: la entidad de feromona de caza hace en cada tick, tambien en el cliente, "invocar mobs del almacen del mundo", que lee un dato
 * global (Entomophobia.activeData) que solo existe en la PC que hospeda el mundo. Un jugador que se une a un mundo compartido tiene ese dato en
 * null y el juego se cierra con un NullPointerException al ver una feromona. La logica de verdad es del servidor, asi que en el cliente no se ejecuta.
 */
@Mixin(targets = "mod.pilot.entomophobia.entity.pheromones.PheromonePreyHuntEntity", remap = false)
public abstract class EntomophobiaPheromoneClientTickMixin {

    @Inject(method = "m_8119_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$noClientTick(CallbackInfo ci) {
        if (((Entity) (Object) this).level().isClientSide) {
            ci.cancel();
        }
    }
}
