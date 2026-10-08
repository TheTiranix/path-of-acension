// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tcorigenes.tcorigenes.boss.GodBossEntity;
import com.tcorigenes.tcorigenes.boss.GodSizes;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja a un dios jefe con su modelo de cubos, a la escala que le toca (ver GodSizes). */
public class GodBossRenderer extends MobRenderer<GodBossEntity, GodBossModel> {
    private final ResourceLocation texture;
    private final float scale;

    public GodBossRenderer(EntityRendererProvider.Context context, String god) {
        super(context, new GodBossModel(context.bakeLayer(layer(god)), god), 0.0F);
        this.texture = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/entity/god/" + god + ".png");
        this.scale = GodSizes.SIZES.get(god)[2];
    }

    public static ModelLayerLocation layer(String god) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tcorigenes", "god_" + god), "main");
    }

    @Override
    public ResourceLocation getTextureLocation(GodBossEntity entity) {
        return this.texture;
    }

    @Override
    protected void scale(GodBossEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(this.scale, this.scale, this.scale);
    }

    @Override
    protected boolean shouldShowName(GodBossEntity entity) {
        return false;
    }
}
