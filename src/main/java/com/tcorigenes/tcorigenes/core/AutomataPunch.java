// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Puños del Autómata: a mano limpia (sin ningun item en la mano), el golpe lanza al enemigo lo
 * bastante alto como para que la caida le haga daño de verdad (vanilla se encarga solo del daño de
 * caida), y al aterrizar queda "paralizado" 1s (aproximado con Lentitud extrema, la unica forma sin
 * mixins de frenar el movimiento de cualquier LivingEntity, jugador o mob).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class AutomataPunch {
    /** Velocidad vertical (bloques/tick) para que la caida sea de mas de 3 bloques (empieza a doler). */
    private static final double LAUNCH_VELOCITY = 1.3;
    private static final int PARALYSIS_TICKS = 20;
    /** Cuantos ticks se espera como maximo a que aterrice (agua, vuelo, etc.): despues se descarta. */
    private static final int MAX_WAIT_TICKS = 100;

    /** uuid -> ya se lo vio en el aire (para no paralizar antes de que llegue a despegar del piso). */
    private static final Map<UUID, Boolean> PENDING_LANDING = new HashMap<>();
    private static final Map<UUID, Integer> WAIT_TICKS = new HashMap<>();

    private AutomataPunch() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker) || event.getSource().getDirectEntity() != attacker) {
            return; // solo golpe directo (nada de flechas, proyectiles o daño elemental diferido)
        }
        if (!attacker.getMainHandItem().isEmpty()) {
            return; // solo a puño limpio
        }
        Race race = attacker.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
        if (race != Race.AUTOMATA) {
            return;
        }
        LivingEntity target = event.getEntity();
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, LAUNCH_VELOCITY, motion.z);
        target.hasImpulse = true; // sincroniza la velocidad al cliente (igual que el knockback normal)
        PENDING_LANDING.put(target.getUUID(), false);
        WAIT_TICKS.put(target.getUUID(), 0);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        UUID id = entity.getUUID();
        Boolean wasAirborne = PENDING_LANDING.get(id);
        if (wasAirborne == null) {
            return;
        }
        int waited = WAIT_TICKS.merge(id, 1, Integer::sum);
        if (waited > MAX_WAIT_TICKS) {
            PENDING_LANDING.remove(id);
            WAIT_TICKS.remove(id);
            return;
        }
        if (!entity.onGround()) {
            PENDING_LANDING.put(id, true);
        } else if (wasAirborne) {
            PENDING_LANDING.remove(id);
            WAIT_TICKS.remove(id);
            // "Paralizado": Lentitud a un nivel que deja la velocidad efectiva en 0 durante 1s.
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PARALYSIS_TICKS, 250, false, false, false));
        }
    }
}
