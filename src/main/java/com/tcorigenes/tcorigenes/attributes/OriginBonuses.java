// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.attributes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Jerarquia de bonos porcentuales: la raza y la clase van en el ULTIMO escalon y se SUMAN entre si
 * (20% de raza + 20% de clase = x1.40), en vez de multiplicarse (x1.2 * x1.2 = x1.44).
 *
 *   total = (base + planos) * (1 + suma de % menores: items, arbol) * (1 + raza + clase + pasivo de origen)
 *
 * En atributos de Minecraft: los planos son ADDITION, los % menores MULTIPLY_BASE (se suman entre si) y
 * este modificador unico por atributo es MULTIPLY_TOTAL. Los gestores de raza/clase/pasivo dejan aca su
 * aporte por "fuente" y apply() lo junta en un solo modificador. Los aportes viven en memoria: los
 * gestores se vuelven a ejecutar en login/respawn/cambio de dimension, que es cuando se reconstruyen.
 */
public final class OriginBonuses {
    private static final Map<UUID, Map<String, Map<Attribute, Double>>> DATA = new HashMap<>();

    private OriginBonuses() {
    }

    private static List<Attribute> tracked() {
        return List.of(Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED, Attributes.ATTACK_DAMAGE,
                Attributes.ATTACK_SPEED, ForgeMod.ENTITY_REACH.get());
    }

    private static UUID idOf(Attribute attribute) {
        return UUID.nameUUIDFromBytes(("tcorigenes:origin:" + ForgeRegistries.ATTRIBUTES.getKey(attribute))
                .getBytes(StandardCharsets.UTF_8));
    }

    /** Borra el aporte anterior de una fuente ("race", "class", "racepassive") antes de recalcularlo. */
    public static void clear(Player player, String source) {
        DATA.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).remove(source);
    }

    public static void add(Player player, String source, Attribute attribute, double amount) {
        DATA.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .computeIfAbsent(source, k -> new HashMap<>())
                .merge(attribute, amount, Double::sum);
    }

    /** Aporte de un buff/debuff temporal (Furia, luna, peso): amount 0 lo quita. Reaplica solo si cambio. */
    public static void set(Player player, String source, Attribute attribute, double amount) {
        Map<Attribute, Double> contribution = DATA.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .computeIfAbsent(source, k -> new HashMap<>());
        double old = contribution.getOrDefault(attribute, 0.0);
        if (Math.abs(old - amount) < 1e-9) {
            return;
        }
        if (Math.abs(amount) < 1e-9) {
            contribution.remove(attribute);
        } else {
            contribution.put(attribute, amount);
        }
        apply(player);
    }

    /** Junta los aportes de todas las fuentes en un unico modificador por atributo. */
    public static void apply(Player player) {
        Map<String, Map<Attribute, Double>> sources = DATA.getOrDefault(player.getUUID(), Map.of());
        for (Attribute attribute : tracked()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            instance.removeModifier(idOf(attribute));
            double sum = 0;
            for (Map<Attribute, Double> contribution : sources.values()) {
                sum += contribution.getOrDefault(attribute, 0.0);
            }
            if (Math.abs(sum) > 1e-9) {
                instance.addTransientModifier(new AttributeModifier(idOf(attribute), "Origen (raza + clase)", sum,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }
}
