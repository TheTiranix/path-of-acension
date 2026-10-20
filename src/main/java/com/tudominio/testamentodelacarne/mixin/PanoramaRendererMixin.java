// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import com.tcorigenes.tcorigenes.client.MenuScene;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** El menu principal dibuja su propio fondo animado (MenuScene): aca se apaga el panorama de vanilla para que no lo tape. */
@Mixin(PanoramaRenderer.class)
public abstract class PanoramaRendererMixin {

    @Inject(method = "m_110003_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void testamentodelacarne$noPanorama(float deltaT, float alpha, CallbackInfo ci) {
        if (MenuScene.active()) {
            ci.cancel();
        }
    }
}
