package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tcorigenes.tcorigenes.core.WeakPointEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Dibuja el punto debil como la carga ignea de vanilla (mismo sprite), pero extremadamente
 * roja, con 10% de translucidez, mas grande que una bola de fuego comun y a maximo brillo.
 * Solo se dibuja para el jugador dueño del marcador.
 */
public class WeakPointRenderer extends EntityRenderer<WeakPointEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/item/fire_charge.png");
    private static final float SIZE = 0.9F;
    private static final int FULL_BRIGHT = 15728880;

    public WeakPointRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WeakPointEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getUUID().equals(entity.getOwnerId())) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(consumer, pose.pose(), pose.normal(), 0, 0, 0, 1);
        vertex(consumer, pose.pose(), pose.normal(), 1, 0, 1, 1);
        vertex(consumer, pose.pose(), pose.normal(), 1, 1, 1, 0);
        vertex(consumer, pose.pose(), pose.normal(), 0, 1, 0, 0);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int x, int y, int u, int v) {
        // Rojo puro, alfa 230/255 (~10% translucido).
        consumer.vertex(pose, x - 0.5F, y - 0.25F, 0.0F)
                .color(255, 0, 0, 230)
                .uv((float) u, (float) v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(WeakPointEntity entity) {
        return TEXTURE;
    }
}
