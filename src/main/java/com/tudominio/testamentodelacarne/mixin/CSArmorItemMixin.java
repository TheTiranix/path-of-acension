// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ArmorMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pedidos de alejandr0 sobre las armaduras divinas de Celestisynth (CSArmorItem#hurtWearer):
 * - la explosion de la solar solo lastima mobs hostiles (se filtra el predicado que elige a quien pegar);
 * - la lunar pierde su 1/5 de reducir el golpe recibido (su explosion propia esta en LunarArmorBurst).
 */
@Mixin(targets = "com.aqutheseal.celestisynth.api.item.CSArmorItem", remap = false)
public abstract class CSArmorItemMixin {

    @Inject(method = "lambda$hurtWearer$0", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void testamentodelacarne$onlyHostile(LivingEntity wearer, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValueZ() && target instanceof Enemy);
    }

    @Redirect(method = "hurtWearer", remap = false, require = 0, at = @At(value = "INVOKE",
            target = "Lcom/aqutheseal/celestisynth/api/item/CSArmorItem;getSameArmorCount(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ArmorMaterial;)I"))
    private static int testamentodelacarne$noLunarReduction(LivingEntity entity, ArmorMaterial material) {
        if (material.getName().contains("lunar")) {
            return 0;
        }
        int count = 0;
        for (net.minecraft.world.item.ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor && armor.getMaterial() == material) {
                count++;
            }
        }
        return count;
    }
}
