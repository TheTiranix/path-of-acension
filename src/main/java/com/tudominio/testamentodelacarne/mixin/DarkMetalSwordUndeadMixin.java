// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pedido de alejandr0: la espada de dark metal (Sharpened Dark Metal Sword, Born in Chaos) ya no hace daño extra a los no-muertos.
 * Ese procedimiento le pega 20 (o 14) de daño magico mas y le pone el efecto "Barbed Attack" a todo mob no-muerto: se anula entero.
 */
@Mixin(targets = "net.mcreator.borninchaosv.procedures.SharpenedDarkMetalSwordKoghdaZhivaiaSushchnostPopadaietSPomoshchiuInstrumientaProcedure", remap = false)
public abstract class DarkMetalSwordUndeadMixin {

    @Inject(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void testamentodelacarne$noUndeadBonus(LevelAccessor world, double x, double y, double z, Entity entity,
            Entity sourceentity, ItemStack itemstack, CallbackInfo ci) {
        ci.cancel();
    }
}
