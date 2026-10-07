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
        // cabeza pelada, sin turbante
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // torso musculoso: pecho ancho que se afina en abdomen, cintura y punta (la cola del genio)
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-5.0F, 0.0F, -2.5F, 10.0F, 7.0F, 5.0F)
                        .texOffs(32, 16).addBox(-4.0F, 7.0F, -2.0F, 8.0F, 5.0F, 4.0F)
                        .texOffs(0, 30).addBox(-2.5F, 12.0F, -1.5F, 5.0F, 4.0F, 3.0F)
                        .texOffs(20, 30).addBox(-1.5F, 16.0F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // brazos gruesos con biceps y brazalete
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 40).addBox(-4.0F, -2.0F, -2.0F, 4.0F, 11.0F, 4.0F)
                        .texOffs(16, 40).addBox(-4.5F, 0.0F, -2.5F, 5.0F, 4.0F, 5.0F)
                        .texOffs(36, 40).addBox(-4.5F, 6.0F, -2.5F, 5.0F, 2.0F, 5.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 40).mirror().addBox(0.0F, -2.0F, -2.0F, 4.0F, 11.0F, 4.0F)
                        .texOffs(16, 40).mirror().addBox(-0.5F, 0.0F, -2.5F, 5.0F, 4.0F, 5.0F)
                        .texOffs(36, 40).mirror().addBox(-0.5F, 6.0F, -2.5F, 5.0F, 2.0F, 5.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AnimaSpiritEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        int rgb = entity.color();
        this.tintR = ((rgb >> 16) & 255) / 255.0F;
        this.tintG = ((rgb >> 8) & 255) / 255.0F;
        this.tintB = (rgb & 255) / 255.0F;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD + 0.22F; // cabeza gacha, mirada amenazante
        // brazos cruzados hacia adelante y arriba, meciendose (pose clasica del genio)
        float sway = Mth.sin(ageInTicks * 0.12F) * 0.04F;
        this.rightArm.xRot = -0.75F + sway;
        this.leftArm.xRot = -0.75F - sway;
        this.rightArm.zRot = 0.55F;   // brazos abiertos y tensos, listos para atacar
        this.leftArm.zRot = -0.55F;
        this.rightArm.yRot = 0.2F;
        this.leftArm.yRot = -0.2F;
        // la cola de humo se mece
        this.body.zRot = Mth.sin(ageInTicks * 0.1F) * 0.05F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, r * this.tintR, g * this.tintG, b * this.tintB, a);
    }
}
