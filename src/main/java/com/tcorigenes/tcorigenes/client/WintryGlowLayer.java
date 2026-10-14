// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido del usuario: las partes doradas de la armadura wintry puesta brillan como las de la fiery. Sobre la armadura normal se dibuja de nuevo
 * solo el dorado (texturas wintry_layer_N_glow.png, con el resto transparente) a brillo maximo, sin importar la luz del lugar.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class WintryGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation OUTER = ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "textures/models/armor/wintry_layer_1_glow.png");
    private static final ResourceLocation INNER = ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "textures/models/armor/wintry_layer_2_glow.png");
    private static final int FULL_BRIGHT = 15728880;

    private final HumanoidModel<T> inner;
    private final HumanoidModel<T> outer;

    private WintryGlowLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        this.inner = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        this.outer = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : new String[] {"default", "slim"}) {
            LivingEntityRenderer living = (LivingEntityRenderer) event.getSkin(skin);
            if (living != null && living.getModel() instanceof HumanoidModel) {
                living.addLayer(new WintryGlowLayer(living, event.getEntityModels()));
            }
        }
    }

    private static boolean isWintry(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("testamentodelacarne") && id.getPath().startsWith("wintry_");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, T entity, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!isWintry(entity.getItemBySlot(slot), slot)) {
                continue;
            }
            HumanoidModel<T> model = slot == EquipmentSlot.LEGS ? inner : outer;
            getParentModel().copyPropertiesTo(model);
            model.setAllVisible(false);
            switch (slot) {
                case HEAD -> {
                    model.head.visible = true;
                    model.hat.visible = true;
                }
                case CHEST -> {
                    model.body.visible = true;
                    model.rightArm.visible = true;
                    model.leftArm.visible = true;
                }
                case LEGS -> {
                    model.body.visible = true;
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
                default -> {
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
            }
            var consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(slot == EquipmentSlot.LEGS ? INNER : OUTER));
            model.renderToBuffer(poseStack, consumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
