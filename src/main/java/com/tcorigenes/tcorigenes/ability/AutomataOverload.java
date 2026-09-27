// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Habilidad racial del Autómata: 20s de +50% daño elemental de aire (el multiplicador real se
 * aplica en RacialElemental, que ya convierte un 10% del golpe en aire) a cambio de -35% de
 * velocidad de movimiento. Cooldown de 60s (se activa con la tecla J, ver ActivateRacialAbilityPacket).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class AutomataOverload {
    public static final int DURATION_TICKS = 20 * 20;
    public static final int COOLDOWN_TICKS = 20 * 60;
    private static final float AIR_MULTIPLIER = 1.5F;

    /** UUID -> ticks restantes. */
    private static final Map<java.util.UUID, Integer> ACTIVE = new HashMap<>();

    private AutomataOverload() {
    }

    public static void start(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), DURATION_TICKS);
        com.tcorigenes.tcorigenes.attributes.OriginBonuses.set(player, "overload", Attributes.MOVEMENT_SPEED, -0.35);
    }

    private static void end(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        com.tcorigenes.tcorigenes.attributes.OriginBonuses.set(player, "overload", Attributes.MOVEMENT_SPEED, 0.0);
    }

    /** true mientras la sobrecarga esta activa (para el multiplicador de daño de aire, ver RacialElemental). */
    public static boolean isActive(net.minecraft.world.entity.player.Player player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static float airMultiplier(net.minecraft.world.entity.player.Player player) {
        return isActive(player) ? AIR_MULTIPLIER : 1.0F;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Integer remaining = ACTIVE.get(player.getUUID());
        if (remaining == null) {
            return;
        }
        remaining--;
        if (remaining <= 0) {
            end(player);
            return;
        }
        ACTIVE.put(player.getUUID(), remaining);
        if (remaining % 10 == 0 && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.4, 0.3, 0.4, 0.02);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }
}
