package com.tudominio.elementaldamage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

/**
 * Cooldowns por atacante para los procs de Tierra (stun) y Aire (rayo): "la cuenta para poder
 * activarse comienza con el primer golpe acertado, y tenes que esperar x tiempo para que vuelva
 * a activarse" (documento). Tambien guarda que victimas estan actualmente stuneadas por Tierra,
 * para poder bloquearles el ataque (ver ModEvents#onAttackEntity).
 */
public final class ElementalProcCooldowns {
    public enum Kind { EARTH, AIR }

    private static final Map<UUID, Long> EARTH_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> AIR_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> STUNNED_UNTIL = new HashMap<>();

    private ElementalProcCooldowns() {
    }

    public static boolean isReady(LivingEntity attacker, Kind kind, long nowTicks) {
        Long readyAt = cooldownMap(kind).get(attacker.getUUID());
        return readyAt == null || nowTicks >= readyAt;
    }

    public static void startCooldown(LivingEntity attacker, Kind kind, long readyAtTick) {
        cooldownMap(kind).put(attacker.getUUID(), readyAtTick);
    }

    private static Map<UUID, Long> cooldownMap(Kind kind) {
        return kind == Kind.EARTH ? EARTH_COOLDOWN : AIR_COOLDOWN;
    }

    public static void markStunned(LivingEntity target, long untilTick) {
        STUNNED_UNTIL.put(target.getUUID(), untilTick);
    }

    public static boolean isStunned(LivingEntity entity, long nowTicks) {
        Long until = STUNNED_UNTIL.get(entity.getUUID());
        return until != null && nowTicks < until;
    }
}
