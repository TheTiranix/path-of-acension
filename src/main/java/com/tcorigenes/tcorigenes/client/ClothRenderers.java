// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Dibuja la ropa de hielo de los slots de ropa de Curios: un modelo humanoide inflado un poco (queda debajo de la armadura). */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClothRenderers {
    private static final ResourceLocation MAIN = ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "textures/models/cloth/ice_cloth.png");
    private static final ResourceLocation BOOTS = ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "textures/models/cloth/ice_cloth_boots.png");
    private static HumanoidModel<LivingEntity> model;

    private ClothRenderers() {
    }

    private enum Piece { HEAD, CHEST, LEGS, FEET }

    private static HumanoidModel<LivingEntity> gloveModel;

    private static void registerGloves() {
        for (String material : new String[] {"leather", "chainmail", "iron", "golden", "diamond", "netherite", "zanite", "gravitite", "valkyrie",
                "neptune", "obsidian", "phoenix"}) {
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("testamentodelacarne", material + "_glove"));
            if (item != null) {
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "textures/models/glove/" + material + ".png");
                CuriosRendererRegistry.register(item, () -> new GloveRenderer(texture));
            }
        }
    }

    /** Un guante: el slot 0 es la mano derecha y el 1 la izquierda. */
    private static final class GloveRenderer implements ICurioRenderer {
        private final ResourceLocation texture;

        GloveRenderer(ResourceLocation texture) {
            this.texture = texture;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
                PoseStack poseStack, RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light,
                float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(parent.getModel() instanceof HumanoidModel<?> base)) {
                return;
            }
            if (gloveModel == null) {
                gloveModel = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.GLOVE));
            }
            ((HumanoidModel<LivingEntity>) base).copyPropertiesTo(gloveModel);
            gloveModel.setAllVisible(false);
            boolean right = slotContext.index() % 2 == 0;
            gloveModel.rightArm.visible = right;
            gloveModel.leftArm.visible = !right;
            var consumer = buffer.getBuffer(RenderType.armorCutoutNoCull(texture));
            gloveModel.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        register("hood", Piece.HEAD);
        register("tunic", Piece.CHEST);
        register("leggings", Piece.LEGS);
        register("boots", Piece.FEET);
        registerGloves();
    }

    private static void register(String suffix, Piece piece) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "ice_cloth_" + suffix));
        if (item != null) {
            CuriosRendererRegistry.register(item, () -> new ClothRenderer(piece));
        }
    }

    private static final class ClothRenderer implements ICurioRenderer {
        private final Piece piece;

        ClothRenderer(Piece piece) {
            this.piece = piece;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
                PoseStack poseStack, RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light,
                float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(parent.getModel() instanceof HumanoidModel<?> base)) {
                return;
            }
            if (model == null) {
                model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.ICE_CLOTH));
            }
            ((HumanoidModel<LivingEntity>) base).copyPropertiesTo(model);
            model.setAllVisible(false);
            switch (piece) {
                case HEAD -> model.head.visible = true;
                case CHEST -> {
                    model.body.visible = true;
                    model.rightArm.visible = true;
                    model.leftArm.visible = true;
                }
                default -> {
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
            }
            var consumer = buffer.getBuffer(RenderType.armorCutoutNoCull(piece == Piece.FEET ? BOOTS : MAIN));
            model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
