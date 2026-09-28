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
    private static final ResourceLocation GEARS_WOOD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/automata_gear_wood.png");
    private static final ResourceLocation GEARS_METAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/automata_gear_metal.png");

    private final ModelPart horns;
    private final ModelPart wings;
    private final ModelPart antennas;
    private final ModelPart gears;

    public RaceFeaturesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet modelSet) {
        super(parent);
        this.horns = modelSet.bakeLayer(ModModelLayers.DEMON_HORNS);
        this.wings = modelSet.bakeLayer(ModModelLayers.ANGEL_WINGS);
        this.antennas = modelSet.bakeLayer(ModModelLayers.SIERVO_ANTENNAS);
        this.gears = modelSet.bakeLayer(ModModelLayers.AUTOMATA_GEARS);
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
            poseWings(WingAnimation.openness(player, partialTick), ageInTicks);
            // Translucent (no cutout): la textura tiene un degrade real de transparencia en las
            // puntas para que no se vea como un bloque duro.
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WINGS_TEXTURE));
            wings.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        } else if (race == Race.MALNACIDO && !ClientRaceData.isPurified(player.getUUID())) {
            renderMalnacido(poseStack, buffer, packedLight, player);
        } else if (race == Race.AUTOMATA) {
            renderGears(poseStack, buffer, packedLight, ageInTicks);
        }
    }

    /** true si el jugador ya desbloqueo TODOS los nodos del arbol de su clase actual. */
    private static boolean treeMaxed() {
        var cls = com.tcorigenes.tcorigenes.progression.client.ClientSkillData.playerClass();
        var nodes = com.tcorigenes.tcorigenes.progression.SkillTree.forClass(cls);
        if (nodes.isEmpty()) {
            return false;
        }
        var unlocked = com.tcorigenes.tcorigenes.progression.client.ClientSkillData.unlocked();
        return nodes.stream().allMatch(node -> unlocked.contains(node.storageKey()));
    }

    /** Engranajes girando en cada hombro y uno en la espalda; de madera hasta que el arbol de la clase
     *  actual esta al maximo, ahi pasan a ser de metal (pedido de alejandr0). Misma tecnica EXACTA que
     *  las alas del Angel (que sabemos que se ve): la posicion va horneada en el PartPose de cada hijo
     *  (ver ModModelLayers#createGearsLayer), aca solo se gira cada uno y se dibuja la RAIZ una vez. */
    private void renderGears(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float ageInTicks) {
        ResourceLocation texture = treeMaxed() ? GEARS_METAL_TEXTURE : GEARS_WOOD_TEXTURE;
        float spin = ageInTicks * 0.08F;
        this.gears.getChild("left_gear").zRot = spin;
        this.gears.getChild("right_gear").zRot = -spin;
        this.gears.getChild("back_gear").zRot = spin * 0.5F;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.gears.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /**
     * Alas plegadas contra la espalda (open = 0) que se abren en abanico al planear o volar (open = 1); abiertas
     * aletean apenas.
     */
    private void poseWings(float open, float ageInTicks) {
        float eased = open * open * (3.0F - 2.0F * open);
        float flap = eased * (float) Math.sin(ageInTicks * 0.2F) * 0.06F;
        for (int i = 0; i < ModModelLayers.FEATHERS_PER_WING; i++) {
            float fan = ModModelLayers.featherFan(i) * eased + (0.04F + i * 0.02F) * (1.0F - eased);
            float droop = ModModelLayers.featherDroop(i) + 0.18F * (1.0F - eased);
            for (int side = 0; side < 2; side++) {
                float sign = side == 0 ? 1.0F : -1.0F;
                ModelPart feather = this.wings.getChild((side == 0 ? "left_feather_" : "right_feather_") + i);
                feather.xRot = droop;
                feather.yRot = sign * fan;
                feather.zRot = sign * flap;
            }
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
