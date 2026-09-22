// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Ubica el punto debil sobre el MODELO real del mob (no sobre su hitbox): hace falta para mobs grandes
 * cuyo modelo es bastante mas grande que su hitbox (el punto por hitbox quedaria adentro del cuerpo).
 * Formula (pedida explicitamente, simplificada a proposito):
 * - en X: siempre en el MEDIO del ancho del modelo.
 * - en Y/Z: lo mas arriba Y lo mas adelante posible, pero como el punto mas alto y el mas adelantado no
 *   siempre coinciden (una cabeza puede estar mas adelante pero mas abajo que un lomo, por ejemplo), se
 *   prioriza uno de los dos al azar (fijo por marca, ver WeakPointEntity#isPriorityHeight): si se prioriza
 *   la altura, se toma el punto mas alto y, ENTRE los que estan cerca de esa altura, el mas adelantado; si
 *   se prioriza la profundidad, al reves.
 * - SIEMPRE con margen: el punto nunca se genera en el limite exacto del modelo (se mete hacia el centro
 *   un porcentaje fijo), para que no quede flotando afuera de la geometria.
 * - el radio (mismo que el visual) se escala con el tamaño total del modelo: mobs muy chicos (<= 0.5
 *   bloques en sus 3 dimensiones) lo tienen un poco mas chico en proporcion; mobs muy grandes (>= 6
 *   bloques) lo tienen mas grande en proporcion.
 * Solo cliente: el servidor no tiene modelos (ver WeakPointAimSync, que le manda este resultado).
 */
public final class WeakPointModelAnchor {
    private static final Map<Class<?>, List<Field>> PART_FIELDS = new HashMap<>();

    /** Cuanto se mete el punto hacia el centro del modelo, para no quedar en el borde exacto. */
    private static final double INSET = 0.80;
    /** "Cerca de esa altura/profundidad": banda de tolerancia como fraccion del alto/profundidad total. */
    private static final double BAND_FRACTION = 0.22;

    private static final double SMALL_THRESHOLD = 0.5;
    private static final double LARGE_THRESHOLD = 6.0;
    private static final double SMALL_RADIUS = 0.30;
    private static final double MID_RADIUS_LOW = 0.35;
    private static final double MID_RADIUS_HIGH = 0.9;
    private static final double LARGE_RADIUS_CAP = 2.4;

    private WeakPointModelAnchor() {
    }

    public record Result(Vec3 offset, double radius) {
    }

    /** Un punto en la geometria del modelo, ya en espacio "local" (ver compute): X/Y/Z en bloques. */
    private record Point(double x, double y, double z) {
    }

    /**
     * Desplazamiento (en ejes del mundo, desde markerPos) hasta el punto debil sobre el modelo, y el radio
     * de acierto que le corresponde por tamaño; o null si el modelo no se pudo leer o el resultado no tiene
     * sentido (modelos raros de otros mods). En ese caso se usa la posicion/radio por hitbox de siempre.
     */
    @SuppressWarnings("unchecked")
    public static Result compute(LivingEntity target, Vec3 markerPos, float partialTick,
                                 EntityRenderDispatcher dispatcher, boolean priorityHeight) {
        try {
            EntityRenderer<?> renderer = dispatcher.getRenderer(target);
            if (!(renderer instanceof LivingEntityRenderer<?, ?> living)) {
                return null;
            }
            EntityModel<LivingEntity> model = (EntityModel<LivingEntity>) living.getModel();

            float bodyYaw = Mth.rotLerp(partialTick, target.yBodyRotO, target.yBodyRot);
            float headYaw = Mth.rotLerp(partialTick, target.yHeadRotO, target.yHeadRot);
            float netHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw);
            float pitch = Mth.lerp(partialTick, target.xRotO, target.getXRot());
            float limbSwing = target.walkAnimation.position(partialTick);
            float limbAmount = Math.min(1.0F, target.walkAnimation.speed(partialTick));
            model.prepareMobModel(target, limbSwing, limbAmount, partialTick);
            model.setupAnim(target, limbSwing, limbAmount, target.tickCount + partialTick, netHeadYaw, pitch);

            // Pose "local" (sin el giro por yaw ni el traslado al mundo): mete la pose del frame (cabeza,
            // patas, etc) pero X/Z quedan en la orientacion propia del modelo, no la del mundo. Ahi X es
            // ancho, Z es adelante/atras (minZ = frente, igual que en el modelo crudo) e Y ya esta en
            // convencion "arriba = mayor" gracias al mismo flip que usa el renderer de verdad.
            PoseStack psLocal = new PoseStack();
            psLocal.scale(-1.0F, -1.0F, 1.0F);
            psLocal.translate(0.0F, -1.501F, 0.0F);

            List<Point> corners = new ArrayList<>();
            for (Field field : partFields(model.getClass())) {
                Object value = field.get(model);
                if (value instanceof ModelPart part) {
                    part.visit(psLocal, (pose, path, index, cube) -> {
                        float w = cube.maxX - cube.minX;
                        float h = cube.maxY - cube.minY;
                        float d = cube.maxZ - cube.minZ;
                        if (w <= 0 || h <= 0 || d <= 0) {
                            return;
                        }
                        Matrix4f m = pose.pose();
                        for (float cx : new float[] {cube.minX, cube.maxX}) {
                            for (float cy : new float[] {cube.minY, cube.maxY}) {
                                for (float cz : new float[] {cube.minZ, cube.maxZ}) {
                                    Vector4f v = new Vector4f(cx / 16F, cy / 16F, cz / 16F, 1F);
                                    v.mul(m);
                                    corners.add(new Point(v.x, v.y, v.z));
                                }
                            }
                        }
                    });
                }
            }
            if (corners.size() < 8) {
                return null;
            }

            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            for (Point p : corners) {
                minX = Math.min(minX, p.x());
                maxX = Math.max(maxX, p.x());
                minY = Math.min(minY, p.y());
                maxY = Math.max(maxY, p.y());
                minZ = Math.min(minZ, p.z());
                maxZ = Math.max(maxZ, p.z());
            }
            double centerX = (minX + maxX) / 2.0;
            double centerY = (minY + maxY) / 2.0;
            double centerZ = (minZ + maxZ) / 2.0;
            double widthSpan = maxX - minX;
            double heightSpan = maxY - minY;
            double depthSpan = maxZ - minZ;
            double overallSize = Math.max(widthSpan, Math.max(heightSpan, depthSpan));
            if (overallSize < 1.0E-4) {
                return null;
            }

            double localY;
            double localZ;
            if (priorityHeight) {
                // Mas arriba posible; entre lo que esta cerca de esa altura, lo mas adelante.
                double bandY = maxY - heightSpan * BAND_FRACTION;
                double frontAtTop = corners.stream().filter(p -> p.y() >= bandY)
                        .mapToDouble(Point::z).min().orElse(minZ);
                localY = centerY + (maxY - centerY) * INSET;
                localZ = centerZ + (frontAtTop - centerZ) * INSET;
            } else {
                // Mas adelante posible; entre lo que esta cerca de ese frente, lo mas arriba.
                double bandZ = minZ + depthSpan * BAND_FRACTION;
                double topAtFront = corners.stream().filter(p -> p.z() <= bandZ)
                        .mapToDouble(Point::y).max().orElse(maxY);
                localZ = centerZ + (minZ - centerZ) * INSET;
                localY = centerY + (topAtFront - centerY) * INSET;
            }
            double localX = centerX; // siempre centrado en el ancho

            // El resto de la cadena (rotar por el yaw del cuerpo y trasladar al mundo), sobre ese unico punto.
            Vec3 rel = target.getPosition(partialTick).subtract(markerPos);
            PoseStack psTail = new PoseStack();
            psTail.translate(rel.x, rel.y, rel.z);
            psTail.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            Vector4f chosen = new Vector4f((float) localX, (float) localY, (float) localZ, 1F);
            chosen.mul(psTail.last().pose());
            Vec3 offset = new Vec3(chosen.x, chosen.y, chosen.z);

            // Red de seguridad: si el resultado se fue muy lejos del hitbox (modelo roto/mal leido), no se usa.
            // El margen es proporcional al tamaño del modelo, para no rechazar mobs grandes con hitbox chica.
            Vec3 worldPoint = markerPos.add(offset);
            double allowance = Math.max(1.0, overallSize * 0.9);
            if (worldPoint.distanceTo(target.getBoundingBox().getCenter()) > allowance) {
                return null;
            }

            return new Result(offset, radiusFor(overallSize));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** El radio (visual y de acierto) crece con el tamaño del modelo: mas chico si es diminuto, mas grande si es enorme. */
    private static double radiusFor(double overallSize) {
        if (overallSize <= SMALL_THRESHOLD) {
            return SMALL_RADIUS * Math.max(0.5, overallSize / SMALL_THRESHOLD);
        }
        if (overallSize >= LARGE_THRESHOLD) {
            double extra = Math.min(1.0, (overallSize - LARGE_THRESHOLD) / LARGE_THRESHOLD);
            return Math.min(LARGE_RADIUS_CAP, MID_RADIUS_HIGH + 1.0 + extra * (LARGE_RADIUS_CAP - MID_RADIUS_HIGH - 1.0));
        }
        double t = (overallSize - SMALL_THRESHOLD) / (LARGE_THRESHOLD - SMALL_THRESHOLD);
        return Mth.lerp(t, MID_RADIUS_LOW, MID_RADIUS_HIGH);
    }

    private static List<Field> partFields(Class<?> modelClass) {
        return PART_FIELDS.computeIfAbsent(modelClass, type -> {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (ModelPart.class.isAssignableFrom(f.getType()) && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        try {
                            f.setAccessible(true);
                            fields.add(f);
                        } catch (RuntimeException ignored) {
                            // campo inaccesible: se ignora
                        }
                    }
                }
            }
            return fields;
        });
    }
}
