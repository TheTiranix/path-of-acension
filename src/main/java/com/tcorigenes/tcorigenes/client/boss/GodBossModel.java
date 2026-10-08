// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tcorigenes.tcorigenes.boss.GodBossEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Modelo de cubos de un dios jefe (la geometria sale de GodModelData). Flota con un vaiven y anima las partes segun el prefijo de su nombre:
 * "wing" aletea, "tent" ondula como un tentaculo, "float" flota por su cuenta y "spin" late (halo, llamas); "breath" respira (el cuerpo se contrae y dilata), "pupil" contrae la pupila, "shake" tiembla; "fix" queda quieto.
 */
public class GodBossModel extends EntityModel<GodBossEntity> {
    private final ModelPart root;
    private final Map<String, ModelPart> parts = new LinkedHashMap<>();
    private final Map<String, float[]> base = new LinkedHashMap<>();

    public GodBossModel(ModelPart root, String god) {
        this.root = root;
        for (String name : GodModelData.PARTS.get(god)) {
            ModelPart part = root.getChild(name);
            this.parts.put(name, part);
            this.base.put(name, new float[] {part.x, part.y, part.z});
        }
    }

    @Override
    public void setupAnim(GodBossEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.y = (float) Math.sin(ageInTicks * 0.05F) * 2.0F;
        for (Map.Entry<String, ModelPart> entry : this.parts.entrySet()) {
            String name = entry.getKey();
            ModelPart part = entry.getValue();
            float[] b = this.base.get(name);
            int underscore = name.indexOf('_');
            String tag = underscore < 0 ? name : name.substring(0, underscore);
            float phase = (name.hashCode() & 255) * 0.07F;
            switch (tag) {
                case "wing" -> part.y = b[1] + (float) Math.sin(ageInTicks * 0.12F + phase) * 1.8F;
                case "tent" -> {
                    part.x = b[0] + (float) Math.sin(ageInTicks * 0.1F + phase) * 2.0F;
                    part.z = b[2] + (float) Math.cos(ageInTicks * 0.1F + phase) * 2.0F;
                }
                case "float" -> part.y = b[1] + (float) Math.sin(ageInTicks * 0.07F + phase) * 3.0F;
                case "spin" -> {
                    float pulse = 1.0F + (float) Math.sin(ageInTicks * 0.15F + phase) * 0.08F;
                    part.xScale = pulse;
                    part.yScale = pulse;
                    part.zScale = pulse;
                }
                case "breath" -> {
                    float s = 1.0F + (float) Math.sin(ageInTicks * 0.05F + phase * 0.15F) * 0.07F;
                    part.xScale = s;
                    part.zScale = s;
                }
                case "pupil" -> part.xScale = 1.0F + (float) Math.sin(ageInTicks * 0.09F) * 0.7F;
                case "shake" -> {
                    part.x = b[0] + (float) Math.sin(ageInTicks * 1.7F + phase) * 0.35F;
                    part.y = b[1] + (float) Math.cos(ageInTicks * 2.1F + phase) * 0.35F;
                }
                default -> {
                }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
