// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.TCOrigenes;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Geometria extra de raza: cuernos de Demonio, alas de Angel (ver RaceFeaturesLayer). */
public final class ModModelLayers {
    public static final ModelLayerLocation DEMON_HORNS =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "demon_horns"), "main");
    public static final ModelLayerLocation SIERVO_ANTENNAS =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "siervo_antennas"), "main");
    public static final ModelLayerLocation ANGEL_WINGS =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "angel_wings"), "main");

    private ModModelLayers() {
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DEMON_HORNS, ModModelLayers::createHornsLayer);
        event.registerLayerDefinition(ANGEL_WINGS, ModModelLayers::createWingsLayer);
        event.registerLayerDefinition(SIERVO_ANTENNAS, ModModelLayers::createAntennasLayer);
    }

    private static LayerDefinition createHornsLayer() {
        // Mas grandes que antes (1x3x1 -> 1.5x6x1.5) y un poco mas separados/curvados hacia atras.
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_horn",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -6.0F, -0.75F, 1.5F, 6.0F, 1.5F),
                PartPose.offsetAndRotation(-3.0F, -7.0F, 0.5F, -0.2F, 0.0F, -0.45F));
        root.addOrReplaceChild("right_horn",
                CubeListBuilder.create().texOffs(6, 0).addBox(-0.75F, -6.0F, -0.75F, 1.5F, 6.0F, 1.5F),
                PartPose.offsetAndRotation(3.0F, -7.0F, 0.5F, -0.2F, 0.0F, 0.45F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static LayerDefinition createAntennasLayer() {
        // Dos antenas finas (tallo + bulbo en la punta), levemente abiertas hacia afuera.
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addAntenna(root, "left", -2.0F, -0.25F);
        addAntenna(root, "right", 2.0F, 0.25F);
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static void addAntenna(PartDefinition root, String side, float x, float tilt) {
        PartDefinition antenna = root.addOrReplaceChild(side + "_antenna",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.25F, -5.0F, -0.25F, 0.5F, 5.0F, 0.5F),
                PartPose.offsetAndRotation(x, -8.0F, -1.0F, -0.1F, 0.0F, tilt));
        antenna.addOrReplaceChild(side + "_bulb",
                CubeListBuilder.create().texOffs(0, 8).addBox(-0.75F, -6.5F, -0.75F, 1.5F, 1.5F, 1.5F),
                PartPose.ZERO);
    }

    private static final int FEATHERS_PER_WING = 5;

    private static LayerDefinition createWingsLayer() {
        // En vez de un solo bloque rigido, cada ala es un abanico de "plumas" finas que se van
        // abriendo en angulo y achicando de largo hacia el borde - mucho menos "cuadrado".
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addWing(root, "left", 1.0F, 1.0F);
        addWing(root, "right", -1.0F, -1.0F);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void addWing(PartDefinition root, String side, float xPivotSign, float angleSign) {
        // Pivote subido de y=4.0 a y=2.0 (media espalda alta, no la zona lumbar) por pedido.
        for (int i = 0; i < FEATHERS_PER_WING; i++) {
            float length = 17.0F - i * 2.2F;
            float fanAngle = angleSign * (0.45F + i * 0.22F);
            float droop = 0.1F + i * 0.12F;
            CubeListBuilder builder = CubeListBuilder.create().texOffs(0, 0)
                    .addBox(0.0F, 0.0F, -0.5F, 1.0F, length, 1.0F);
            root.addOrReplaceChild(side + "_feather_" + i, builder,
                    PartPose.offsetAndRotation(xPivotSign * 1.0F, 2.0F, 2.5F, droop, fanAngle, 0.0F));
        }
    }
}
