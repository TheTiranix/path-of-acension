// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tcorigenes.tcorigenes.boss.GodBossEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Dioses con modelo de Blockbench (GeckoLib): geo/god_X.geo.json, animations/god_X.animation.json, textures/entity/god/X.png. */
public class GodGeoRenderer extends GeoEntityRenderer<GodBossEntity> {
    public GodGeoRenderer(EntityRendererProvider.Context context, String god) {
        super(context, new Model(god));
        this.shadowRadius = 0.0F;
    }

    private static final class Model extends GeoModel<GodBossEntity> {
        private final ResourceLocation model, texture, animation;

        Model(String god) {
            this.model = ResourceLocation.fromNamespaceAndPath("tcorigenes", "geo/god_" + god + ".geo.json");
            this.texture = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/entity/god/" + god + ".png");
            this.animation = ResourceLocation.fromNamespaceAndPath("tcorigenes", "animations/god_" + god + ".animation.json");
        }

        @Override
        public ResourceLocation getModelResource(GodBossEntity entity) {
            return this.model;
        }

        @Override
        public ResourceLocation getTextureResource(GodBossEntity entity) {
            return this.texture;
        }

        @Override
        public ResourceLocation getAnimationResource(GodBossEntity entity) {
            return this.animation;
        }
    }

    /** Son dioses: se ven igual de iluminados de noche y en cuevas. */
    @Override
    public void render(GodBossEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(entity, yaw, partialTick, poseStack, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    public boolean shouldShowName(GodBossEntity entity) {
        return false;
    }
}
