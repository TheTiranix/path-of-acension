// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Trackea, por victima y por elemento, cuanto daño de ese tipo recibio recientemente (decae
 * linealmente a 0 en su propia ventana si no la vuelven a golpear con ese elemento: 8s para
 * hielo y agua, 5s para ender, segun el documento de diseño v2-1). Sirve para los elementos cuyo
 * efecto no es instantaneo (fuego/lunar/natural se resuelven solos con mecanicas vanilla) sino
 * que se lee en otro momento: agua (al curar), ender y tierra (al calcular critico), aire (al
 * tirar esquive), hielo (que ademas necesita un modifier de velocidad que hay que ir re-bajando
 * con el tiempo).
 */
public final class ElementalStackManager {
    private static final long ICE_WATER_DECAY_WINDOW_MS = 8000L;
    private static final long DEFAULT_DECAY_WINDOW_MS = 5000L;
    private static final UUID ICE_SPEED_MODIFIER_ID = UUID.fromString("11111111-2222-4333-8444-555555550001");
    private static final UUID ICE_ATTACK_SPEED_MODIFIER_ID = UUID.fromString("11111111-2222-4333-8444-555555550002");
    private static final UUID ICE_DRAW_SPEED_MODIFIER_ID = UUID.fromString("11111111-2222-4333-8444-555555550004");
    private static final UUID ENDER_ARMOR_MODIFIER_ID = UUID.fromString("11111111-2222-4333-8444-555555550003");

    private record Stack(double accumulatedDamage, long lastHitMillis) {
    }

    private static final Map<UUID, Map<ResourceKey<DamageType>, Stack>> STACKS = new HashMap<>();
    private static final Map<UUID, LivingEntity> ICE_TRACKED = new HashMap<>();
    private static final Map<UUID, LivingEntity> ENDER_TRACKED = new HashMap<>();

    private ElementalStackManager() {
    }

    /** Ventana de decaimiento de cada elemento (ver documento v2-1): hielo y agua 8s, el resto 5s. */
    private static long decayWindowFor(ResourceKey<DamageType> element) {
        return (element.equals(ModDamageTypes.ICE) || element.equals(ModDamageTypes.WATER_ELEMENTAL))
                ? ICE_WATER_DECAY_WINDOW_MS : DEFAULT_DECAY_WINDOW_MS;
    }

    private static double decayedValue(Stack stack, long now, long window) {
        if (stack == null) {
            return 0.0;
        }
        long elapsed = now - stack.lastHitMillis();
        if (elapsed >= window) {
            return 0.0;
        }
        return stack.accumulatedDamage() * (1.0 - (double) elapsed / window);
    }

    /** Registra un golpe de "element" por "damage" puntos (ya decayendo lo anterior) y devuelve el acumulado resultante. */
    public static double registerHit(LivingEntity victim, ResourceKey<DamageType> element, float damage) {
        long now = System.currentTimeMillis();
        Map<ResourceKey<DamageType>, Stack> perEntity = STACKS.computeIfAbsent(victim.getUUID(), k -> new HashMap<>());
        double decayed = decayedValue(perEntity.get(element), now, decayWindowFor(element));
        double updated = decayed + damage;
        perEntity.put(element, new Stack(updated, now));
        return updated;
    }

    /** Lee el acumulado actual (decayendo segun el tiempo transcurrido) sin registrar un golpe nuevo. */
    public static double peek(LivingEntity victim, ResourceKey<DamageType> element) {
        Map<ResourceKey<DamageType>, Stack> perEntity = STACKS.get(victim.getUUID());
        if (perEntity == null) {
            return 0.0;
        }
        return decayedValue(perEntity.get(element), System.currentTimeMillis(), decayWindowFor(element));
    }

    private static double icePercent(double accumulatedDamage, boolean isPlayerTarget) {
        double cap = isPlayerTarget ? 0.20 : 0.30;
        return Math.min(0.02 * (accumulatedDamage / 10.0), cap);
    }

    /** Aplica (o refuerza) el enlentecimiento de hielo de la victima segun su stack actual. */
    public static void applyIceSlow(LivingEntity victim, double accumulatedAfterHit) {
        double percent = icePercent(accumulatedAfterHit, victim instanceof Player);
        setIceModifier(victim, percent);
        if (percent > 0.0) {
            ICE_TRACKED.put(victim.getUUID(), victim);
        }
    }

    private static void setIceModifier(LivingEntity victim, double percent) {
        applyModifier(victim.getAttribute(Attributes.MOVEMENT_SPEED), ICE_SPEED_MODIFIER_ID, "Elemental Ice Slow", -percent);
        applyModifier(victim.getAttribute(Attributes.ATTACK_SPEED), ICE_ATTACK_SPEED_MODIFIER_ID, "Elemental Ice Attack Slow", -percent);
        // La velocidad de carga de arco tambien baja (solo existe en jugadores); es aditiva, no MULTIPLY_TOTAL.
        var draw = victim.getAttribute(ModAttributes.DRAW_SPEED.get());
        if (draw != null) {
            draw.removeModifier(ICE_DRAW_SPEED_MODIFIER_ID);
            if (percent != 0.0) {
                draw.addTransientModifier(new AttributeModifier(ICE_DRAW_SPEED_MODIFIER_ID, "Elemental Ice Draw Slow", -percent, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    private static double enderArmorPercent(double accumulatedDamage, boolean isPlayerTarget) {
        double cap = isPlayerTarget ? 0.20 : 0.30;
        return Math.min(0.02 * (accumulatedDamage / 10.0), cap);
    }

    /** Aplica (o refuerza) la reduccion de armadura por daño elemental ender acumulado. */
    public static void applyEnderArmorReduction(LivingEntity victim, double accumulatedAfterHit) {
        double percent = enderArmorPercent(accumulatedAfterHit, victim instanceof Player);
        setEnderArmorModifier(victim, percent);
        if (percent > 0.0) {
            ENDER_TRACKED.put(victim.getUUID(), victim);
        }
    }

    private static void setEnderArmorModifier(LivingEntity victim, double percent) {
        applyModifier(victim.getAttribute(Attributes.ARMOR), ENDER_ARMOR_MODIFIER_ID, "Elemental Ender Armor Reduction", -percent);
    }

    private static void applyModifier(AttributeInstance instance, UUID id, String name, double value) {
        if (instance == null) {
            return;
        }
        if (instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
        if (value != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, name, value, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** Llamar cada ~1 segundo: la lista es chica (solo entidades golpeadas con hielo recientemente). */
    public static void tickIceDecay() {
        if (ICE_TRACKED.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, LivingEntity>> it = ICE_TRACKED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = it.next();
            LivingEntity victim = entry.getValue();
            if (!victim.isAlive()) {
                it.remove();
                continue;
            }
            Map<ResourceKey<DamageType>, Stack> perEntity = STACKS.get(entry.getKey());
            double accumulated = perEntity == null ? 0.0 : decayedValue(perEntity.get(ModDamageTypes.ICE), now, decayWindowFor(ModDamageTypes.ICE));
            double percent = icePercent(accumulated, victim instanceof Player);
            setIceModifier(victim, percent);
            if (percent <= 0.0) {
                it.remove();
            }
        }
    }

    /** Igual que tickIceDecay, pero para la reduccion de armadura de daño ender. */
    public static void tickEnderDecay() {
        if (ENDER_TRACKED.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, LivingEntity>> it = ENDER_TRACKED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = it.next();
            LivingEntity victim = entry.getValue();
            if (!victim.isAlive()) {
                it.remove();
                continue;
            }
            Map<ResourceKey<DamageType>, Stack> perEntity = STACKS.get(entry.getKey());
            double accumulated = perEntity == null ? 0.0 : decayedValue(perEntity.get(ModDamageTypes.ENDER_ELEMENTAL), now, decayWindowFor(ModDamageTypes.ENDER_ELEMENTAL));
            double percent = enderArmorPercent(accumulated, victim instanceof Player);
            setEnderArmorModifier(victim, percent);
            if (percent <= 0.0) {
                it.remove();
            }
        }
    }
}
