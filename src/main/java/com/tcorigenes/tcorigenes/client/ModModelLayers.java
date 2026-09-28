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
    public static final ModelLayerLocation AUTOMATA_GEARS =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "automata_gears"), "main");
    public static final ModelLayerLocation AUTOMATA_CORE =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "automata_core"), "main");

    private ModModelLayers() {
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DEMON_HORNS, ModModelLayers::createHornsLayer);
        event.registerLayerDefinition(ANGEL_WINGS, ModModelLayers::createWingsLayer);
        event.registerLayerDefinition(SIERVO_ANTENNAS, ModModelLayers::createAntennasLayer);
        event.registerLayerDefinition(AUTOMATA_GEARS, ModModelLayers::createGearsLayer);
        event.registerLayerDefinition(AUTOMATA_CORE, ModModelLayers::createCoreLayer);
    }

    /** Nucleo de energia en el pecho (pedido de alejandr0): brilla de verdad, ver RaceFeaturesLayer. */
    private static LayerDefinition createCoreLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.6F, -1.6F, -0.6F, 3.2F, 3.2F, 1.2F),
                PartPose.offset(0.0F, 4.0F, -2.3F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** Un engranaje por hombro y uno en la espalda, con la posicion HORNEADA en el propio PartPose
     *  (igual que las plumas del ala, ver addWing): asi se dibujan renderizando la RAIZ una sola vez
     *  (this.gears.render(...)), que es la forma que ya sabemos que funciona para alas/cuernos, en vez
     *  de buscar el hijo y traducir la pose a mano antes de dibujarlo (eso fallaba: no se veian).
     *  Gira sobre si mismo en RaceFeaturesLayer. La textura (madera o metal segun el arbol de
     *  habilidades) dibuja el color base; los dientes son geometria real (ver gearCuboids), no textura,
     *  para que se vea como un engranaje de verdad desde cualquier angulo.
     *  Posicion: Y negativo = por ENCIMA de la linea del hombro (el body vanilla mide y=[0,12], 0 arriba
     *  del todo), apoyados sobre el hombro en vez de enterrados adentro del brazo (que ocupa x=[4,8]/y=[0,12]
     *  del lado derecho, espejado del izquierdo). Antes estaban centrados en y=0.5 (adentro del brazo) y
     *  ahora quedan arriba, con solo el borde inferior tocando el hombro. */
    private static LayerDefinition createGearsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // z NEGATIVO = adelante (mismo lado que la cara): si fuera positivo el brazo (solido, opaco)
        // queda ENTRE la camara y el engranaje y lo tapa por completo desde una vista de frente.
        root.addOrReplaceChild("left_gear",
                gearCuboids(), PartPose.offset(6.5F, -2.0F, -0.3F));
        root.addOrReplaceChild("right_gear",
                gearCuboids(), PartPose.offset(-6.5F, -2.0F, -0.3F));
        root.addOrReplaceChild("back_gear",
                gearCuboids(), PartPose.offset(0.0F, 1.5F, 2.6F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** Engranaje real: un cubo central (buje) mas 8 dientes cuadrados distribuidos en circulo
     *  (trigonometria simple, sin rotar cada caja), asi el contorno tiene muescas de verdad entre
     *  diente y diente en vez de ser solo una plancha lisa con textura pintada. */
    private static CubeListBuilder gearCuboids() {
        float hubHalf = 1.5F;
        float toothHalf = 0.8F;
        float toothOffset = 2.0F;
        float depth = 1.6F;
        CubeListBuilder builder = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-hubHalf, -hubHalf, -depth / 2.0F, hubHalf * 2.0F, hubHalf * 2.0F, depth);
        int teeth = 8;
        for (int i = 0; i < teeth; i++) {
            double angle = Math.PI * 2.0 * i / teeth;
            float cx = (float) (Math.cos(angle) * toothOffset);
            float cy = (float) (Math.sin(angle) * toothOffset);
            builder.addBox(cx - toothHalf, cy - toothHalf, -depth / 2.0F, toothHalf * 2.0F, toothHalf * 2.0F, depth);
        }
        return builder;
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

    public static final int FEATHERS_PER_WING = 5;

    /** Angulo de abanico (rotY, sin signo) de la pluma i con el ala completamente abierta. */
    public static float featherFan(int i) {
        return 0.45F + i * 0.22F;
    }

    /** Inclinacion hacia abajo (rotX) de la pluma i. */
    public static float featherDroop(int i) {
        return 0.1F + i * 0.12F;
    }

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
            float fanAngle = angleSign * featherFan(i);
            float droop = featherDroop(i);
            CubeListBuilder builder = CubeListBuilder.create().texOffs(0, 0)
                    .addBox(0.0F, 0.0F, -0.5F, 1.0F, length, 1.0F);
            root.addOrReplaceChild(side + "_feather_" + i, builder,
                    PartPose.offsetAndRotation(xPivotSign * 1.0F, 2.0F, 2.5F, droop, fanAngle, 0.0F));
        }
    }
}
