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
 * RaceSyncPacket): cuernos de Demonio, alas de Angel. La altura del Ender Warrior y la
 * deformidad del Malnacido se manejan aparte via RenderPlayerEvent.Pre/Post (ver
 * RaceRenderEvents), pura escala de partes del modelo, no hace falta nada aca.
 */
public class RaceFeaturesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation HORNS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/demon_horns.png");
    private static final ResourceLocation WINGS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/angel_wings.png");

    private final ModelPart horns;
    private final ModelPart wings;

    public RaceFeaturesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet modelSet) {
        super(parent);
        this.horns = modelSet.bakeLayer(ModModelLayers.DEMON_HORNS);
        this.wings = modelSet.bakeLayer(ModModelLayers.ANGEL_WINGS);
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
        } else if (race == Race.ANGEL) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            // Translucent (no cutout): la textura tiene un degrade real de transparencia en las
            // puntas para que no se vea como un bloque duro.
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WINGS_TEXTURE));
            wings.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
