// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pedido de alejandr0: la espada de gravitite ya no hace volar por los aires a los enemigos. GravititeSwordItem#hurtEnemy llama a
 * launchEntity; esa llamada se anula (el golpe y el desgaste siguen igual).
 */
@Mixin(targets = "com.aetherteam.aether.item.combat.GravititeSwordItem", remap = false)
public abstract class GravititeSwordNoLaunchMixin {

    @Redirect(method = "m_7579_", at = @At(value = "INVOKE",
            target = "Lcom/aetherteam/aether/item/combat/GravititeSwordItem;launchEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V"),
            remap = false, require = 0)
    private void testamentodelacarne$noLaunch(@org.spongepowered.asm.mixin.injection.Coerce Object self,
            net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
    }
}
