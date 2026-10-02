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
