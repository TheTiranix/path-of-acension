// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cooldowns server-autoritativos en memoria, medidos en TIEMPO REAL (System.currentTimeMillis),
 * no en ticks de juego. Con un server a TPS baja (pack pesado, com aca ~9 TPS) un cooldown en
 * ticks tarda mucho mas de lo esperado en tiempo real; en reloj de pared siempre dura lo mismo
 * sin importar la TPS. Se pierden al reiniciar el server (no persistido).
 */
public final class AbilityCooldownManager {
    private static final Map<UUID, Map<String, Long>> READY_AT_EPOCH_MILLIS = new HashMap<>();
    /** Jugadores con /abilitydebug activado: para ellos remainingMillis siempre da 0. No persiste
     *  entre reinicios del server (es solo para probar rapido, no un cheat permanente). */
    private static final Set<UUID> NO_COOLDOWN_DEBUG = new HashSet<>();

    private AbilityCooldownManager() {
    }

    public static boolean toggleDebugNoCooldown(ServerPlayer player) {
        if (NO_COOLDOWN_DEBUG.remove(player.getUUID())) {
            return false;
        }
        NO_COOLDOWN_DEBUG.add(player.getUUID());
        return true;
    }

    /** Milisegundos restantes (0 si ya esta lista, o si tiene /abilitydebug activado). */
    public static long remainingMillis(ServerPlayer player, PlayerAbility ability) {
        if (NO_COOLDOWN_DEBUG.contains(player.getUUID())) {
            return 0;
        }
        Map<String, Long> perPlayer = READY_AT_EPOCH_MILLIS.get(player.getUUID());
        if (perPlayer == null) {
            return 0;
        }
        Long readyAt = perPlayer.get(ability.id());
        if (readyAt == null) {
            return 0;
        }
        return Math.max(0, readyAt - System.currentTimeMillis());
    }

    public static boolean isReady(ServerPlayer player, PlayerAbility ability) {
        return remainingMillis(player, ability) <= 0;
    }

    public static void startCooldown(ServerPlayer player, PlayerAbility ability) {
        long cooldownMillis = ability.cooldownTicks() * 50L;
        long readyAt = System.currentTimeMillis() + cooldownMillis;
        READY_AT_EPOCH_MILLIS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(ability.id(), readyAt);
    }

    public static void clearPlayer(UUID playerId) {
        READY_AT_EPOCH_MILLIS.remove(playerId);
    }
}
