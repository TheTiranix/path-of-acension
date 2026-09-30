// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pedido de alejandr0: ningun mob puede sacarle ni robarle un item, arma o armadura al jugador. Este procedimiento de
 * Born in Chaos hace que el Krampus Henchman, al recibir un golpe, le tire al jugador el arma de la mano (la desarma): con un jugador
 * como atacante no hace nada.
 */
@Mixin(targets = "net.mcreator.borninchaosv.procedures.KrampusHenchmanAttackProcedure", remap = false)
public abstract class KrampusHenchmanAttackProcedureMixin {

    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void testamentodelacarne$noDisarm(Event event, LevelAccessor world, double x, double y, double z, Entity entity,
            Entity sourceentity, CallbackInfo ci) {
        if (sourceentity instanceof Player) {
            ci.cancel();
        }
    }
}
