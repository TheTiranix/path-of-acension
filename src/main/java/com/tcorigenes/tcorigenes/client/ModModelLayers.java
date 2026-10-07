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
    public static final ModelLayerLocation CRUCIFIX =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "crucifix"), "main");
    public static final ModelLayerLocation DEMON_TAIL =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "demon_tail"), "main");
    public static final ModelLayerLocation ANGEL_HALO =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "angel_halo"), "main");
    public static final ModelLayerLocation SIERVO_EYES =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "siervo_eyes"), "main");
    public static final int TAIL_SEGMENTS = 7;
    public static final ModelLayerLocation ANIMA_SPIRIT = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("tcorigenes", "anima_spirit"), "main");

    private ModModelLayers() {
    }

    /** Modelo de la ropa de hielo (humanoide un poco inflado, por debajo de la armadura). */
    public static final ModelLayerLocation ICE_CLOTH =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "ice_cloth"), "main");

    /** Modelo de los guantes individuales (brazos un poco inflados). */
    public static final ModelLayerLocation GLOVE =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "glove"), "main");

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GLOVE, () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.HumanoidModel.createMesh(new net.minecraft.client.model.geom.builders.CubeDeformation(0.35F), 0.0F), 64, 32));
        event.registerLayerDefinition(ICE_CLOTH, () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.HumanoidModel.createMesh(new net.minecraft.client.model.geom.builders.CubeDeformation(0.7F), 0.0F), 64, 32));
        event.registerLayerDefinition(DEMON_HORNS, ModModelLayers::createHornsLayer);
        event.registerLayerDefinition(ANGEL_WINGS, ModModelLayers::createWingsLayer);
        event.registerLayerDefinition(SIERVO_ANTENNAS, ModModelLayers::createAntennasLayer);
        event.registerLayerDefinition(AUTOMATA_GEARS, ModModelLayers::createGearsLayer);
        event.registerLayerDefinition(AUTOMATA_CORE, ModModelLayers::createCoreLayer);
        event.registerLayerDefinition(ANIMA_SPIRIT, AnimaSpiritModel::createLayer);
        event.registerLayerDefinition(CRUCIFIX, ModModelLayers::createCrucifixLayer);
        event.registerLayerDefinition(DEMON_TAIL, ModModelLayers::createTailLayer);
        event.registerLayerDefinition(ANGEL_HALO, ModModelLayers::createHaloLayer);
        event.registerLayerDefinition(SIERVO_EYES, ModModelLayers::createEyesLayer);
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

    /** Crucifijo dorado colgando del cuello: cadena en V y cruz sobre el pecho (cuelga delante de la armadura). */
    private static LayerDefinition createCrucifixLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("chain_left", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 0.5F, 4.0F, 0.5F),
                PartPose.offsetAndRotation(-2.2F, -0.4F, -3.4F, 0.0F, 0.0F, 0.52F));
        root.addOrReplaceChild("chain_right", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 0.0F, 0.0F, 0.5F, 4.0F, 0.5F),
                PartPose.offsetAndRotation(2.2F, -0.4F, -3.4F, 0.0F, 0.0F, -0.52F));
        PartDefinition cross = root.addOrReplaceChild("cross", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.6F, 0.0F, 0.0F, 1.2F, 5.0F, 0.7F)
                        .texOffs(0, 0).addBox(-1.8F, 1.2F, 0.0F, 3.6F, 1.2F, 0.7F),
                PartPose.offset(0.0F, 3.4F, -3.5F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** Cola fina de demonio: segmentos encadenados (cada uno hijo del anterior, asi la onda se acumula) y un corazon en la punta. */
    private static LayerDefinition createTailLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot();
        for (int i = 0; i < TAIL_SEGMENTS; i++) {
            float thickness = 0.9F - i * 0.07F;
            PartPose pose = i == 0 ? PartPose.offsetAndRotation(0.0F, 11.0F, 2.0F, 0.0F, 0.0F, 0.0F) : PartPose.offset(0.0F, 0.0F, 2.4F);
            parent = parent.addOrReplaceChild("tail_" + i, CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-thickness / 2.0F, -thickness / 2.0F, 0.0F, thickness, thickness, 2.6F), pose);
        }
        // corazon: rombo + dos lobulos, plano y vertical en la punta
        PartDefinition heart = parent.addOrReplaceChild("heart", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 2.6F));
        heart.addOrReplaceChild("diamond", CubeListBuilder.create().texOffs(0, 8).addBox(-1.1F, -1.1F, -0.25F, 2.2F, 2.2F, 0.5F),
                PartPose.offsetAndRotation(0.0F, 0.7F, 0.0F, 0.0F, 0.0F, 0.7854F));
        heart.addOrReplaceChild("lobe_left", CubeListBuilder.create().texOffs(0, 8).addBox(-0.7F, -0.7F, -0.25F, 1.4F, 1.4F, 0.5F),
                PartPose.offsetAndRotation(-0.8F, -0.7F, 0.0F, 0.0F, 0.0F, 0.7854F));
        heart.addOrReplaceChild("lobe_right", CubeListBuilder.create().texOffs(0, 8).addBox(-0.7F, -0.7F, -0.25F, 1.4F, 1.4F, 0.5F),
                PartPose.offsetAndRotation(0.8F, -0.7F, 0.0F, 0.0F, 0.0F, 0.7854F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** Aureola del Angel: anillo de 10 tramos sobre la cabeza. */
    private static LayerDefinition createHaloLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int pieces = 10;
        for (int i = 0; i < pieces; i++) {
            double angle = Math.PI * 2.0 * i / pieces;
            float x = (float) (Math.cos(angle) * 4.6);
            float z = (float) (Math.sin(angle) * 4.6);
            root.addOrReplaceChild("halo_" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -0.3F, -0.5F, 3.0F, 0.6F, 1.0F),
                    PartPose.offsetAndRotation(x, -12.5F, z, 0.0F, (float) (-angle + Math.PI / 2.0), 0.0F));
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** Ojos del Siervo de la Luna: dos rendijas que brillan de noche. */
    private static LayerDefinition createEyesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("eye_left", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -4.4F, 2.0F, 1.0F, 0.1F), PartPose.ZERO);
        root.addOrReplaceChild("eye_right", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, -4.0F, -4.4F, 2.0F, 1.0F, 0.1F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
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
