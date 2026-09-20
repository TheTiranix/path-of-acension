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

    public static Vec3 of(LivingEntity target) {
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
