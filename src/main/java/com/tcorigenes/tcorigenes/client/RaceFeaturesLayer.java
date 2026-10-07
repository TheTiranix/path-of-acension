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
    private static final ResourceLocation WINGS_GOLD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/angel_wings_gold.png");
    private static final ResourceLocation GEARS_WOOD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/automata_gear_wood.png");
    private static final ResourceLocation GEARS_METAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/automata_gear_metal.png");
    private static final ResourceLocation CORE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/automata_core.png");

    private static final ResourceLocation CRUCIFIX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/crucifix.png");
    private static final ResourceLocation TAIL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/demon_tail.png");
    private static final ResourceLocation HALO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/angel_halo.png");
    private static final ResourceLocation EYES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/race/siervo_eyes.png");

    private final ModelPart crucifix;
    private final ModelPart tail;
    private final ModelPart halo;
    private final ModelPart eyes;
    private final ModelPart horns;
    private final ModelPart wings;
    private final ModelPart antennas;
    private final ModelPart gears;
    private final ModelPart core;

    public RaceFeaturesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet modelSet) {
        super(parent);
        this.crucifix = modelSet.bakeLayer(ModModelLayers.CRUCIFIX);
        this.tail = modelSet.bakeLayer(ModModelLayers.DEMON_TAIL);
        this.halo = modelSet.bakeLayer(ModModelLayers.ANGEL_HALO);
        this.eyes = modelSet.bakeLayer(ModModelLayers.SIERVO_EYES);
        this.horns = modelSet.bakeLayer(ModModelLayers.DEMON_HORNS);
        this.wings = modelSet.bakeLayer(ModModelLayers.ANGEL_WINGS);
        this.antennas = modelSet.bakeLayer(ModModelLayers.SIERVO_ANTENNAS);
        this.gears = modelSet.bakeLayer(ModModelLayers.AUTOMATA_GEARS);
        this.core = modelSet.bakeLayer(ModModelLayers.AUTOMATA_CORE);
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
            renderTail(poseStack, buffer, packedLight, ageInTicks, limbSwingAmount);
        } else if (race == Race.SIERVO_DE_LA_LUNA) {
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(ANTENNAS_TEXTURE));
            antennas.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            renderEyes(poseStack, buffer, player, ageInTicks);
        } else if (race == Race.ANGEL) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            poseWings(WingAnimation.openness(player, partialTick), ageInTicks);
            // Translucent (no cutout): la textura tiene un degrade real de transparencia en las
            // puntas para que no se vea como un bloque duro.
            // Con el arbol de habilidades al maximo las alas se vuelven doradas (solo se sabe el arbol del jugador local).
            boolean gold = player == net.minecraft.client.Minecraft.getInstance().player && treeMaxed();
            VertexConsumer consumer = buffer.getBuffer(gold ? RenderType.entityTranslucentEmissive(WINGS_GOLD_TEXTURE) : RenderType.entityTranslucent(WINGS_TEXTURE));
            wings.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            renderHalo(poseStack, buffer, ageInTicks);
        } else if (race == Race.DEVOTO) {
            renderCrucifix(poseStack, buffer, packedLight, limbSwing, limbSwingAmount, ageInTicks);
        } else if (race == Race.MALNACIDO && !ClientRaceData.isPurified(player.getUUID())) {
            renderMalnacido(poseStack, buffer, packedLight, player);
        } else if (race == Race.AUTOMATA) {
            renderGears(poseStack, buffer, packedLight, ageInTicks);
            renderCore(poseStack, buffer, ageInTicks);
        }
    }

    /** Crucifijo dorado que cuelga del cuello y se mece un poco al caminar. */
    private void renderCrucifix(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float limbSwing, float limbSwingAmount, float ageInTicks) {
        ModelPart cross = this.crucifix.getChild("cross");
        cross.xRot = Math.min(0.5F, limbSwingAmount * 0.6F) * (float) Math.sin(limbSwing * 0.6662F) * 0.5F + (float) Math.sin(ageInTicks * 0.05F) * 0.03F;
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(CRUCIFIX_TEXTURE));
        this.crucifix.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** Cola de demonio: la onda recorre los segmentos de la base a la punta y se mueve siempre, aunque este quieto. */
    private void renderTail(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float ageInTicks, float limbSwingAmount) {
        float amplitude = 0.32F + Math.min(1.0F, limbSwingAmount) * 0.25F;
        ModelPart segment = this.tail;
        for (int i = 0; i < ModModelLayers.TAIL_SEGMENTS; i++) {
            segment = segment.getChild("tail_" + i);
            segment.yRot = (float) Math.sin(ageInTicks * 0.13F - i * 0.65F) * amplitude;
            segment.xRot = (i == 0 ? 0.55F : 0.12F) + (float) Math.sin(ageInTicks * 0.09F - i * 0.5F) * 0.07F;
        }
        ModelPart heart = segment.getChild("heart");
        heart.zRot = (float) Math.sin(ageInTicks * 0.13F - ModModelLayers.TAIL_SEGMENTS * 0.65F) * 0.35F;
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TAIL_TEXTURE));
        this.tail.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** Aureola dorada sobre la cabeza: gira despacio, sube y baja apenas y brilla. */
    private void renderHalo(PoseStack poseStack, MultiBufferSource buffer, float ageInTicks) {
        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, (float) Math.sin(ageInTicks * 0.08F) * 0.015F, 0.0F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(ageInTicks * 1.5F));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(HALO_TEXTURE));
        this.halo.render(poseStack, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** Los ojos del Siervo de la Luna brillan de noche (con un pulso suave). */
    private void renderEyes(PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player, float ageInTicks) {
        long time = player.level().getDayTime() % 24000L;
        if (time < 12600L || time > 23400L) {
            return;
        }
        float pulse = 0.75F + 0.25F * (float) Math.sin(ageInTicks * 0.1F);
        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(EYES_TEXTURE));
        this.eyes.render(poseStack, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1.0F);
        poseStack.popPose();
    }

    /** Nucleo de energia en el pecho: brillo real, full-bright, con un leve pulso. Antes usaba
     *  RenderType.eyes (aditivo, como los ojos del Enderman) pero el pack usa Oculus/Sodium, que en
     *  varias versiones rompe ese pipeline especifico y lo deja negro solido en vez de brillar (bug
     *  conocido, no es exclusivo de este mod). entityTranslucentEmissive pasa por el pipeline normal
     *  de renderizado de entidades (con blending real, respeta el degrade de la textura) forzando luz
     *  maxima, que es mucho mas compatible. */
    private void renderCore(PoseStack poseStack, MultiBufferSource buffer, float ageInTicks) {
        float pulse = 0.85F + 0.15F * (float) Math.sin(ageInTicks * 0.1F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(CORE_TEXTURE));
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.scale(pulse, pulse, 1.0F);
        this.core.render(poseStack, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
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
        // Mismo sentido en los 3 (antes el hombro derecho giraba al reves que el izquierdo: el efecto
        // combinado de las dos direcciones opuestas daba la sensacion de que "se cerraban" hacia adentro).
        float spin = ageInTicks * 0.08F;
        this.gears.getChild("left_gear").zRot = spin;
        this.gears.getChild("right_gear").zRot = spin;
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
