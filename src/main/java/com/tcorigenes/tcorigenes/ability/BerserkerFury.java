// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability;

import com.tudominio.elementaldamage.ModAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Furia del Berserker: 20s de +50% daño y +20% critico. Durante ese tiempo se desangra en
 * BLEED_TICKS pulsos repartidos parejo, cada uno el 3% de la vida maxima, restados con
 * setHealth directo: NO usa hurt(), asi no dispara la ventana de invulnerabilidad y el jugador
 * sigue siendo vulnerable a los monstruos mientras dura. Nunca lo mata (deja minimo 1 de vida).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class BerserkerFury {
    public static final int DURATION_TICKS = 20 * 20;
    public static final int COOLDOWN_TICKS = 20 * 60 * 3;
    private static final int BLEED_TICKS = 5;
    private static final double BLEED_FRACTION = 0.03;
    private static final UUID DAMAGE_ID = UUID.fromString("b3a5e000-0001-4001-8001-0000000000f1");
    private static final UUID CRIT_ID = UUID.fromString("b3a5e000-0002-4002-8002-0000000000f2");

    private static final Map<UUID, Integer> ELAPSED = new HashMap<>();

    private BerserkerFury() {
    }

    public static void start(ServerPlayer player) {
        end(player);
        ELAPSED.put(player.getUUID(), 0);
        // +50% de daño: se suma con el % de raza y clase (ver OriginBonuses), no se multiplica.
        com.tcorigenes.tcorigenes.attributes.OriginBonuses.set(player, "fury", Attributes.ATTACK_DAMAGE, 0.5);
        addModifier(player.getAttribute(ModAttributes.CRIT_CHANCE.get()), CRIT_ID, "Furia Berserker crit", 0.2, AttributeModifier.Operation.ADDITION);
    }

    private static void addModifier(AttributeInstance attribute, UUID id, String name, double amount, AttributeModifier.Operation op) {
        if (attribute != null && attribute.getModifier(id) == null) {
            attribute.addTransientModifier(new AttributeModifier(id, name, amount, op));
        }
    }

    private static void end(ServerPlayer player) {
        ELAPSED.remove(player.getUUID());
        com.tcorigenes.tcorigenes.attributes.OriginBonuses.set(player, "fury", Attributes.ATTACK_DAMAGE, 0.0);
        remove(player.getAttribute(ModAttributes.CRIT_CHANCE.get()), CRIT_ID);
    }

    private static void remove(AttributeInstance attribute, UUID id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Integer elapsed = ELAPSED.get(player.getUUID());
        if (elapsed == null) {
            return;
        }
        elapsed++;
        if (elapsed >= DURATION_TICKS) {
            end(player);
            return;
        }
        ELAPSED.put(player.getUUID(), elapsed);

        int interval = DURATION_TICKS / BLEED_TICKS;
        if (elapsed % interval == 0) {
            float damage = (float) (player.getMaxHealth() * BLEED_FRACTION);
            player.setHealth(Math.max(1.0F, player.getHealth() - damage));
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.5, 0.3, 0.1);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }
}
