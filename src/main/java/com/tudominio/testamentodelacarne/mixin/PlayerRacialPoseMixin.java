// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses de habilidades raciales: el Devoto que reza junta las manos frente al pecho (con el cuerpo agachado por la pose del jugador) y el
 * Gigante Rocoso que lleva un bloque lo sostiene con los dos brazos hacia adelante. Se aplica al terminar setupAnim para que otras
 * animaciones no lo pisen.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerRacialPoseMixin {

    @Inject(method = "m_6973_", at = @At("TAIL"), remap = false, require = 0)
    private void testamentodelacarne$racialPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, CallbackInfo ci) {
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        int id = entity.getId();
        if (com.tcorigenes.tcorigenes.client.ClientRacialState.isPraying(id)) {
            model.rightArm.xRot = -1.05F;
            model.leftArm.xRot = -1.05F;
            model.rightArm.yRot = -0.62F;
            model.leftArm.yRot = 0.62F;
            model.rightArm.zRot = 0.0F;
            model.leftArm.zRot = 0.0F;
            model.head.xRot = 0.35F; // cabeza gacha, rezando
        } else if (com.tcorigenes.tcorigenes.client.ClientRacialState.heldBlock(id) != null) {
            model.rightArm.xRot = -0.9F;
            model.leftArm.xRot = -0.9F;
            model.rightArm.yRot = -0.25F;
            model.leftArm.yRot = 0.25F;
        } else {
            return;
        }
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }
}
