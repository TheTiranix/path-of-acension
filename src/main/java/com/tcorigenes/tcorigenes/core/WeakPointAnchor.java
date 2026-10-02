// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Donde aparece el punto debil sobre la hitbox de un enemigo. Se mide cual de las dos
 * dimensiones es mas larga (altura o profundidad):
 * - si la mas larga es la profundidad: de sus dos extremos se toma el mas adelantado, y se
 *   aplica en el punto mas alto y en el medio (lateral);
 * - si la mas larga es la altura: de sus dos extremos se toma el mas alto, y se aplica en el
 *   punto mas adelantado y en el medio (lateral);
 * - con empate: el punto mas alto, mas adelantado y mas en el medio.
 * En los tres casos el resultado es el mismo punto: arriba, al frente y centrado; se calcula
 * igual, explicitando cada rama, para que la regla quede documentada.
 */
public final class WeakPointAnchor {
    /** Un poco por dentro del borde, para que el marcador quede sobre el cuerpo y no flotando afuera. */
    private static final double INSET = 0.9;

    private WeakPointAnchor() {
    }

    /**
     * Un punto del modelo solo vale si cae dentro de la hitbox del mob (con apenas margen): si queda afuera no se le puede pegar al mob
     * apuntandole (el golpe nunca lo alcanza) y el marcador se vería flotando lejos del cuerpo. Si no pasa, se usa el punto por hitbox.
     */
    public static boolean isNearHitbox(LivingEntity target, Vec3 point) {
        return target.getBoundingBox().inflate(0.15).contains(point);
    }

    /** Dragones de Ice and Fire (fire_dragon, ice_dragon, lightning_dragon): su hitbox principal no es el cuerpo visible, hay partes (cabeza, cuello, cola). */
    public static boolean isDragon(LivingEntity target) {
        var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return id != null && id.getNamespace().equals("iceandfire") && id.getPath().endsWith("_dragon");
    }

    private static java.lang.reflect.Method headMethod;
    private static boolean headMethodLooked;

    /** Posicion de la cabeza del dragon (EntityDragonBase#getHeadPosition, por reflexion para no depender del jar); null si no se pudo. */
    private static Vec3 dragonHead(LivingEntity dragon) {
        try {
            if (!headMethodLooked) {
                headMethodLooked = true;
                headMethod = dragon.getClass().getMethod("getHeadPosition");
            }
            if (headMethod != null && headMethod.getDeclaringClass().isInstance(dragon)) {
                Object v = headMethod.invoke(dragon);
                if (v instanceof Vec3 vec && Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z)) {
                    return vec;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            headMethod = null;
        }
        return null;
    }

    public static Vec3 of(LivingEntity target) {
        return of(target, true);
    }

    /**
     * Punto debil. Para los dragones: en la cabeza (head = true) o en la panza/pecho (head = false), que es el centro del cuerpo un poco
     * por debajo de la mitad; para el resto, la regla de siempre.
     */
    public static Vec3 of(LivingEntity target, boolean head) {
        if (isDragon(target)) {
            Vec3 headPos = head ? dragonHead(target) : null;
            if (headPos != null) {
                return headPos;
            }
            var box = target.getBoundingBox();
            return new Vec3((box.minX + box.maxX) / 2.0, box.minY + (box.maxY - box.minY) * 0.4, (box.minZ + box.maxZ) / 2.0);
        }
        double depth = target.getBbWidth();
        double height = target.getBbHeight();
        double forwardReach;
        double up;
        if (depth > height) {
            // Profundidad mas larga: extremo delantero (mas adelantado) + punto mas alto.
            forwardReach = depth * 0.5;
            up = height;
        } else if (height > depth) {
            // Altura mas larga: extremo superior (mas alto) + punto mas adelantado.
            up = height;
            forwardReach = depth * 0.5;
        } else {
            // Empate: mas alto, mas adelante y mas en el medio.
            up = height;
            forwardReach = depth * 0.5;
        }
        double yawRad = Math.toRadians(target.yBodyRot);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        return new Vec3(
                target.getX() + fx * forwardReach * INSET,
                target.getY() + up * INSET,
                target.getZ() + fz * forwardReach * INSET);
    }
}
