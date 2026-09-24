// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Los efectos especiales de los daños elementales (aturdimiento de tierra, rayo de aire) ya no se
 * calculan con el daño de UN golpe sino con el daño elemental que ese atacante metio sobre ese
 * objetivo durante los ultimos 2 segundos (10 golpes chicos o 1 grande dan lo mismo), para premiar
 * el DPS de un arma rapida aunque pegue menos por golpe (pedido de alejandr0).
 */
public final class ElementalDamageWindow {
    public static final int WINDOW_TICKS = 40;

    private record Hit(long tick, float amount) {
    }

    private record Key(UUID attacker, UUID target, ResourceKey<DamageType> element) {
    }

    private static final Map<Key, Deque<Hit>> HITS = new HashMap<>();

    private ElementalDamageWindow() {
    }

    /** Registra el golpe y devuelve la suma de lo pegado en los ultimos 2 segundos (incluyendo este). */
    public static float record(UUID attacker, UUID target, ResourceKey<DamageType> element, float amount, long now) {
        Deque<Hit> hits = HITS.computeIfAbsent(new Key(attacker, target, element), k -> new ArrayDeque<>());
        hits.addLast(new Hit(now, amount));
        float total = 0.0F;
        for (var it = hits.iterator(); it.hasNext();) {
            Hit hit = it.next();
            if (now - hit.tick() > WINDOW_TICKS) {
                it.remove();
            } else {
                total += hit.amount();
            }
        }
        if (HITS.size() > 4000) {
            HITS.values().removeIf(deque -> deque.isEmpty() || now - deque.peekLast().tick() > WINDOW_TICKS);
        }
        return total;
    }
}
