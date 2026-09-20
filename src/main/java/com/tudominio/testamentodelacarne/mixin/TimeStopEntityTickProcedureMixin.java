// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.mcreator.relicsinchaos.network.RelicsInChaosModVariables;
import net.mcreator.relicsinchaos.procedures.TimeStopEntityTickProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * relics_in_chaos hace 5 escaneos completos del mundo (getEntities + sort por distancia)
 * en CADA tick de CADA entidad viva, sin chequear primero si alguien esta usando la
 * reliquia de "Detener el Tiempo". Confirmado con /spark: 43% del tick entero del server
 * (ver spark.lucko.me/uw4REIpifh), venia de aca.
 *
 * Segun el nombre de la clase y su hermana TimeStopWorldTickProcedure (que si chequea
 * timeStopped > 0 antes de hacer nada), toda esta logica solo tiene sentido cuando el
 * tiempo esta detenido. Si no lo esta, cortamos el metodo antes de que haga el trabajo caro.
 *
 * No modifica el jar original de relics_in_chaos: esto se aplica en runtime via Mixin,
 * asi que si el mod se actualiza y arregla esto en una version nueva, este parche deja
 * de ser necesario (y no rompe nada si igual sigue aplicado, solo pasa a ser redundante).
 */
@Mixin(value = TimeStopEntityTickProcedure.class, remap = false)
public class TimeStopEntityTickProcedureMixin {

    @Inject(
            method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void testamentodelacarne$skipWhenTimeNotStopped(
            Event event, LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null || RelicsInChaosModVariables.MapVariables.get(world).timeStopped <= 0.0) {
            ci.cancel();
        }
    }
}
