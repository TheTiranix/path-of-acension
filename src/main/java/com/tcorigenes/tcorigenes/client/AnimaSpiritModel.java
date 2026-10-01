// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tcorigenes.tcorigenes.ability.AnimaSpiritEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Genio sin piernas: cabeza con turbante, torso en tres tramos que se afinan hacia abajo (la "cola" de humo del genio) y dos
 * brazos con brazaletes. Se tiñe con el color de la raza (el color viene de la entidad, ver renderToBuffer).
 */
public class AnimaSpiritModel extends EntityModel<AnimaSpiritEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart body;
    private float tintR = 1.0F;
    private float tintG = 1.0F;
    private float tintB = 1.0F;

    public AnimaSpiritModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.body = root.getChild("body");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(0, 48).addBox(-4.5F, -10.5F, -4.5F, 9.0F, 3.0F, 9.0F)
                        .texOffs(40, 48).addBox(-1.0F, -13.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 6.0F, 4.0F)
                        .texOffs(24, 16).addBox(-3.0F, 6.0F, -1.5F, 6.0F, 5.0F, 3.0F)
                        .texOffs(44, 16).addBox(-2.0F, 11.0F, -1.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(44, 24).addBox(-1.0F, 15.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.0F, -1.5F, 3.0F, 10.0F, 3.0F)
                        .texOffs(24, 32).addBox(-3.5F, 5.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(-4.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(12, 32).addBox(0.0F, -2.0F, -1.5F, 3.0F, 10.0F, 3.0F)
                        .texOffs(24, 40).addBox(-0.5F, 5.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(4.0F, 2.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AnimaSpiritEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        int rgb = entity.color();
        this.tintR = ((rgb >> 16) & 255) / 255.0F;
        this.tintG = ((rgb >> 8) & 255) / 255.0F;
        this.tintB = (rgb & 255) / 255.0F;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        // brazos cruzados hacia adelante y arriba, meciendose (pose clasica del genio)
        float sway = Mth.sin(ageInTicks * 0.12F) * 0.06F;
        this.rightArm.xRot = -1.0F + sway;
        this.leftArm.xRot = -1.0F - sway;
        this.rightArm.zRot = 0.25F;
        this.leftArm.zRot = -0.25F;
        this.rightArm.yRot = 0.55F;
        this.leftArm.yRot = -0.55F;
        // la cola de humo se mece
        this.body.zRot = Mth.sin(ageInTicks * 0.1F) * 0.05F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, r * this.tintR, g * this.tintG, b * this.tintB, a);
    }
}
