// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Dibuja los guanteletes equipables (hoy solo la Endersoul Hand) en las manos del personaje, proporcionados al brazo (el item a escala chica,
 * del ancho de la mano, pegado a la punta; el slot 0 es la mano derecha y el 1 la izquierda).
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GauntletRenderers {
    public static final String[] ITEMS = {"mutantmonsters:endersoul_hand"};

    private GauntletRenderers() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        for (String id : ITEMS) {
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
            if (item != null) {
                CuriosRendererRegistry.register(item, HandRenderer::new);
            }
        }
    }

    private static final class HandRenderer implements ICurioRenderer {
        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
                PoseStack poseStack, RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light,
                float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(parent.getModel() instanceof HumanoidModel<?> model)) {
                return;
            }
            boolean right = slotContext.index() % 2 == 0;
            poseStack.pushPose();
            (right ? model.rightArm : model.leftArm).translateAndRotate(poseStack);
            poseStack.translate(right ? -0.06 : 0.06, 0.6, 0.0); // punta del brazo (mano)
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            // el item mide 1 bloque de lado y el brazo 0.25: a esta escala queda del ancho de la mano y no sobresale
            // por los costados de la manga
            poseStack.scale(0.26F, 0.26F, 0.26F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                    OverlayTexture.NO_OVERLAY, poseStack, buffer, slotContext.entity().level(), 0);
            poseStack.popPose();
        }
    }
}
