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
 * RaceSyncPacket): cuernos de Demonio, alas de Angel, y el reemplazo de cabeza/brazo agrandados
 * del Malnacido (dibujados con SU PIEL REAL, no una textura nueva). La altura del Ender Warrior
 * se maneja aparte via RenderPlayerEvent.Pre/Post (ver RaceRenderEvents), pura escala de todo el
 * modelo, no hace falta nada aca.
 *
 * El Malnacido es un caso especial: RaceRenderEvents oculta la cabeza/brazo REALES en Pre (asi
 * el pase base no los dibuja), y aca, en un solo metodo (sin dejar hueco para que otro mod de
 * animacion como NotEnoughAnimations/player-animation-lib/ParCool pise el cambio a mitad de
 * camino), se agranda temporalmente esa misma geometria, se dibuja con la textura de piel real
 * del jugador, y se vuelve a dejar en tamaño normal.
 */
public class RaceFeaturesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation HORNS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/demon_horns.png");
    private static final ResourceLocation WINGS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/angel_wings.png");
    private static final float MALNACIDO_HEAD_SCALE = 1.35F;
    private static final float MALNACIDO_ARM_SCALE = 1.45F;

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
        } else if (race == Race.MALNACIDO && !ClientRaceData.isPurified(player.getUUID())) {
            ResourceLocation skin = this.getTextureLocation(player);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
            renderScaledPart(this.getParentModel().head, MALNACIDO_HEAD_SCALE, poseStack, consumer, packedLight);
            renderScaledPart(this.getParentModel().rightArm, MALNACIDO_ARM_SCALE, poseStack, consumer, packedLight);
        }
    }

    /** Agranda una parte, la dibuja, y la vuelve a dejar en 1.0 -- todo en el mismo llamado, sin
     *  ceder el control entre medio a otro codigo que pueda pisar la escala. */
    private static void renderScaledPart(ModelPart part, float scale, PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
        part.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        part.xScale = 1.0F;
        part.yScale = 1.0F;
        part.zScale = 1.0F;
    }
}
