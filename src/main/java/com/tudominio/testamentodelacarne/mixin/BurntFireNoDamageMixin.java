// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pedido del usuario: los bloques encendidos del mod Burnt (llamas, troncos y hojas en brasas, pasto en llamas, alquitran, etc.) hacian
 * demasiado daño al pisarlos. Se apagan los procedimientos que dañan y prenden fuego a la entidad que los toca; el resto del mod
 * (propagacion, humo, explosiones) queda igual.
 */
@Mixin(targets = {
        "net.pixelbank.burnt.procedures.BurntFireDamageProcedure",
        "net.pixelbank.burnt.procedures.BurntLeavesFireDamageProcedure",
        "net.pixelbank.burnt.procedures.BurntLogsFireDamageProcedure",
        "net.pixelbank.burnt.procedures.FireDamageProcedure",
        "net.pixelbank.burnt.procedures.SoulFireDamageProcedure",
        "net.pixelbank.burnt.procedures.TallGrassFireDamageProcedure",
        "net.pixelbank.burnt.procedures.FlamingTarMobCollidesProcedure",
        "net.pixelbank.burnt.procedures.SmokeInhalationDamageProcedure"}, remap = false)
public abstract class BurntFireNoDamageMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void testamentodelacarne$noBurntFireDamage(CallbackInfo ci) {
        ci.cancel();
    }
}
