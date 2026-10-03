// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.intro;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.ChooseRacePacket;
import com.tcorigenes.tcorigenes.playerclass.ClassSelection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Introduccion (pedido del usuario, estilo Fear and Hunger): al entrar por primera vez, una cinematica de lore y preguntas decide la raza
 * y la clase. Las respuestas llegan al servidor, que calcula el resultado con IntroContent y lo aplica (el cliente no decide nada). Mientras
 * dura, el jugador es invulnerable. Si cierra el juego a la mitad, vuelve a empezar al entrar.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class IntroManager {
    private static final String DONE_KEY = "tc_intro_done";
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private IntroManager() {
    }

    public static boolean isDone(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(DONE_KEY);
    }

    public static void start(ServerPlayer player) {
        ACTIVE.add(player.getUUID());
        player.setInvulnerable(true);
        releaseTargets(player);
        Networking.sendToPlayer(player, new StartIntroPacket());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !isDone(player) && !ChooseRacePacket.isRaceChosen(player)) {
            start(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
        event.getEntity().setInvulnerable(false);
    }

    /** Quita de encima al jugador a todo mob que lo tenga de objetivo (cerca). */
    private static void releaseTargets(ServerPlayer player) {
        for (net.minecraft.world.entity.Mob mob : player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                player.getBoundingBox().inflate(64.0), mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    /** Durante la introduccion nadie puede apuntarle al jugador (antes los bichos lo seguian y lo mataban mientras miraba la cinematica). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTarget(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player && ACTIVE.contains(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && ACTIVE.contains(player.getUUID()) && player.tickCount % 10 == 0) {
            player.setInvulnerable(true);
            player.setHealth(player.getMaxHealth());
            releaseTargets(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && ACTIVE.contains(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    /** Respuestas recibidas: se calcula y se aplica la raza y la clase. */
    public static void finish(ServerPlayer player, int[] answers) {
        if (!ACTIVE.contains(player.getUUID())) {
            return;
        }
        IntroContent.Result result = IntroContent.compute(answers);
        if (result == null) {
            return;
        }
        ACTIVE.remove(player.getUUID());
        player.setInvulnerable(false);
        // El orbe se gasta con esta primera eleccion (despues se puede cambiar con otro).
        ChooseRacePacket.consumeOrb(player);
        ChooseRacePacket.applyRace(player, result.race(), false);
        ClassSelection.apply(player, result.playerClass());
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(DONE_KEY, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        Networking.sendToPlayer(player, new IntroResultPacket(result.race(), result.playerClass()));
    }
}
