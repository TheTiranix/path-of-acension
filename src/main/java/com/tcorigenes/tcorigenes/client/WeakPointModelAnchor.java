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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Ubica el punto debil sobre el MODELO real del mob (no sobre su hitbox). Solo cliente: el servidor no tiene
 * modelos. Se pone el modelo en la pose del frame (igual que hace el renderer del mob), se recorren todos sus
 * cubos y, entre los de la parte alta del cuerpo, se elige el mas grande (la cabeza en casi todos los mobs);
 * el marcador queda en el centro de la cara delantera de ese cubo. Si el modelo no se puede leer (modelos
 * raros de otros mods) o el resultado cae fuera del cuerpo, devuelve null y se usa la posicion por hitbox.
 */
public final class WeakPointModelAnchor {
    private static final Map<Class<?>, List<Field>> PART_FIELDS = new HashMap<>();

    private WeakPointModelAnchor() {
    }

    private record Cube(double topY, double volume, Vec3 frontCenter) {
    }

    /** Desplazamiento (en ejes del mundo) desde markerPos hasta el punto sobre el modelo, o null. */
    @SuppressWarnings("unchecked")
    public static Vec3 offset(LivingEntity target, Vec3 markerPos, float partialTick, EntityRenderDispatcher dispatcher) {
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

            // Mismo transform que LivingEntityRenderer#render, pero partiendo de la posicion del marcador.
            Vec3 rel = target.getPosition(partialTick).subtract(markerPos);
            PoseStack ps = new PoseStack();
            ps.translate(rel.x, rel.y, rel.z);
            ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            ps.scale(-1.0F, -1.0F, 1.0F);
            ps.translate(0.0F, -1.501F, 0.0F);

            List<Cube> cubes = new ArrayList<>();
            for (Field field : partFields(model.getClass())) {
                Object value = field.get(model);
                if (value instanceof ModelPart part) {
                    part.visit(ps, (pose, path, index, cube) -> {
                        Matrix4f m = pose.pose();
                        float w = (cube.maxX - cube.minX) / 16F;
                        float h = (cube.maxY - cube.minY) / 16F;
                        float d = (cube.maxZ - cube.minZ) / 16F;
                        if (w <= 0 || h <= 0 || d <= 0) {
                            return;
                        }
                        // La cima del cubo en el mundo es la esquina con menor Y de modelo (el modelo va invertido).
                        Vector4f top = new Vector4f((cube.minX + cube.maxX) / 32F, cube.minY / 16F, (cube.minZ + cube.maxZ) / 32F, 1F);
                        Vector4f front = new Vector4f((cube.minX + cube.maxX) / 32F, (cube.minY + cube.maxY) / 32F,
                                cube.minZ / 16F - 0.02F, 1F);
                        top.mul(m);
                        front.mul(m);
                        cubes.add(new Cube(top.y, w * h * d, new Vec3(front.x, front.y, front.z)));
                    });
                }
            }
            if (cubes.isEmpty()) {
                return null;
            }
            double maxTop = Double.NEGATIVE_INFINITY;
            double minTop = Double.POSITIVE_INFINITY;
            for (Cube c : cubes) {
                maxTop = Math.max(maxTop, c.topY());
                minTop = Math.min(minTop, c.topY());
            }
            double span = Math.max(0.01, maxTop - minTop);
            Cube best = null;
            for (Cube c : cubes) {
                if (c.topY() >= maxTop - 0.35 * span && (best == null || c.volume() > best.volume())) {
                    best = c;
                }
            }
            if (best == null) {
                return null;
            }
            Vec3 anchor = best.frontCenter();
            // Red de seguridad: si cae lejos del cuerpo (mobs escalados por el renderer, etc.), no se usa.
            AABB body = target.getBoundingBox().move(-markerPos.x, -markerPos.y, -markerPos.z).inflate(0.6);
            return body.contains(anchor) ? anchor : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
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
