// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import com.tcorigenes.tcorigenes.core.ClimateImmunity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pedido de alejandr0: los efectos de la armadura de cursium (skymetal) de Cataclysm solo valen con el set completo. Cada pieza trae su
 * efecto suelto (resucitar, esquivar, caida suave) en el manejador de eventos del mod: aca se anulan esos manejadores si el jugador lleva
 * alguna pieza pero no las cuatro.
 */
@Mixin(targets = "com.github.L_Ender.cataclysm.event.ServerEventHandler", remap = false)
public abstract class CursiumSetEventsMixin {

    private static boolean wearsPiece(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getItemBySlot(slot).getItem());
            if (id != null && id.getNamespace().equals("cataclysm") && id.getPath().startsWith("cursium_")) {
                return true;
            }
        }
        return false;
    }

    private static boolean incompleteSet(LivingEntity entity) {
        return entity instanceof Player player && wearsPiece(player) && !ClimateImmunity.wearsSkymetalSet(player);
    }

    @Inject(method = "DeathEvent(Lnet/minecraftforge/event/entity/living/LivingDeathEvent;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$setOnlyDeath(LivingDeathEvent event, CallbackInfo ci) {
        if (incompleteSet(event.getEntity())) {
            ci.cancel();
        }
    }

    @Inject(method = "onLivingAttack(Lnet/minecraftforge/event/entity/living/LivingAttackEvent;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$setOnlyDodge(LivingAttackEvent event, CallbackInfo ci) {
        if (incompleteSet(event.getEntity())) {
            ci.cancel();
        }
    }

    @Inject(method = "onLivingFall(Lnet/minecraftforge/event/entity/living/LivingFallEvent;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$setOnlyFall(LivingFallEvent event, CallbackInfo ci) {
        if (incompleteSet(event.getEntity())) {
            ci.cancel();
        }
    }
}
