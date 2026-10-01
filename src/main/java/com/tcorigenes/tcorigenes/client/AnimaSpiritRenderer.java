// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tcorigenes.tcorigenes.ability.AnimaSpiritEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Genio del Guerrero Anima: translucido y brillante, levita meciendose; al aparecer crece desde nada y al irse se encoge (las dos
 * animaciones salen de la edad de la entidad y de los ticks que le quedan).
 */
public class AnimaSpiritRenderer extends MobRenderer<AnimaSpiritEntity, AnimaSpiritModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/entity/anima_spirit.png");

    public AnimaSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new AnimaSpiritModel(context.bakeLayer(ModModelLayers.ANIMA_SPIRIT)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(AnimaSpiritEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(AnimaSpiritEntity entity, boolean visible, boolean invisible, boolean glowing) {
        return RenderType.entityTranslucentEmissive(TEXTURE);
    }

    @Override
    protected void scale(AnimaSpiritEntity entity, PoseStack poseStack, float partialTick) {
        float appear = Mth.clamp((entity.tickCount + partialTick) / AnimaSpiritEntity.APPEAR_TICKS, 0.0F, 1.0F);
        appear = 1.0F - (1.0F - appear) * (1.0F - appear); // sale rapido y se asienta
        float vanish = Mth.clamp((entity.remaining() - partialTick) / AnimaSpiritEntity.VANISH_TICKS, 0.0F, 1.0F);
        float size = 1.1F * Math.min(appear, vanish);
        poseStack.scale(size, size, size);
        // levita meciendose arriba y abajo
        poseStack.translate(0.0F, Mth.sin((entity.tickCount + partialTick) * 0.1F) * 0.06F, 0.0F);
    }

    @Override
    protected boolean shouldShowName(AnimaSpiritEntity entity) {
        return false;
    }
}
