// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tcorigenes.tcorigenes.TCOrigenes;
import com.tcorigenes.tcorigenes.core.Race;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Geometria extra segun la raza del jugador (ver ClientRaceData, sincronizada por
 * RaceSyncPacket): cuernos de Demonio, antenas del Siervo, alas de Angel y deformidad del
 * Malnacido. La altura del Ender Warrior se maneja aparte via RenderPlayerEvent.Pre/Post
 * (ver RaceRenderEvents).
 */
public class RaceFeaturesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation HORNS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/demon_horns.png");
    private static final ResourceLocation ANTENNAS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/siervo_antennas.png");
    private static final ResourceLocation WINGS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/angel_wings.png");

    private final ModelPart horns;
    private final ModelPart wings;
    private final ModelPart antennas;

    public RaceFeaturesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet modelSet) {
        super(parent);
        this.horns = modelSet.bakeLayer(ModModelLayers.DEMON_HORNS);
        this.wings = modelSet.bakeLayer(ModModelLayers.ANGEL_WINGS);
        this.antennas = modelSet.bakeLayer(ModModelLayers.SIERVO_ANTENNAS);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                        float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        Race race = ClientRaceData.get(player.getUUID());
        if (race == Race.DEMONIO) {
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(HORNS_TEXTURE));
            horns.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        } else if (race == Race.SIERVO_DE_LA_LUNA) {
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(ANTENNAS_TEXTURE));
            antennas.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        } else if (race == Race.ANGEL) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            // Translucent (no cutout): la textura tiene un degrade real de transparencia en las
            // puntas para que no se vea como un bloque duro.
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WINGS_TEXTURE));
            wings.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        } else if (race == Race.MALNACIDO && !ClientRaceData.isPurified(player.getUUID())) {
            renderMalnacido(poseStack, buffer, packedLight, player);
        }
    }

    /**
     * Deformidad del Malnacido, dibujada como CAPA (igual que cuernos/alas, que sabemos que se ven):
     * cabeza y brazo derecho agrandados, mas un tinte enfermizo sobre toda la piel. Antes se escalaban
     * las partes del modelo en RenderPlayerEvent.Pre, pero otros mods (animaciones, Better Combat)
     * pisan la pose despues de eso y la deformidad no se veia. Aca se aplica ya con el modelo posado,
     * y el cuerpo original queda tapado por la copia mas grande.
     */
    private void renderMalnacido(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player) {
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        ResourceLocation skin = player.getSkinTextureLocation();

        RaceRenderEvents.setMalnacidoScale(model, true);
        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        model.head.render(poseStack, solid, packedLight, OverlayTexture.NO_OVERLAY);
        model.hat.render(poseStack, solid, packedLight, OverlayTexture.NO_OVERLAY);
        model.rightArm.render(poseStack, solid, packedLight, OverlayTexture.NO_OVERLAY);
        model.rightSleeve.render(poseStack, solid, packedLight, OverlayTexture.NO_OVERLAY);

        // Tinte verdoso enfermizo sobre toda la piel.
        VertexConsumer tint = buffer.getBuffer(RenderType.entityTranslucent(skin));
        model.renderToBuffer(poseStack, tint, packedLight, OverlayTexture.NO_OVERLAY, 0.45F, 0.85F, 0.4F, 0.5F);
        RaceRenderEvents.setMalnacidoScale(model, false);
    }
}
