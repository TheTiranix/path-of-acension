// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.PacifistSyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido de alejandr0: boton a la izquierda del inventario que pone al personaje en modo pacifico, o sea incapaz de hacer
 * daño. El estado vive en los datos persistentes del jugador (sobrevive a la muerte y a reconectarse) y se le avisa al
 * cliente para que el boton muestre como esta.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class PacifistMode {
    private static final String KEY = "tc_pacifist";

    private PacifistMode() {
    }

    public static boolean isPacifist(Player player) {
        return player.getPersistentData().getBoolean(KEY);
    }

    public static void toggle(ServerPlayer player) {
        boolean now = !isPacifist(player);
        player.getPersistentData().putBoolean(KEY, now);
        sync(player);
        player.displayClientMessage(Component.translatable(now ? "pa.m.pacifist_on" : "pa.m.pacifist_off"), true);
    }

    public static void sync(ServerPlayer player) {
        Networking.sendToPlayer(player, new PacifistSyncPacket(isPacifist(player)));
    }

    /** Ningun daño que venga de un jugador pacifico (golpes, proyectiles, habilidades, elementales) llega a nadie. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Player player && isPacifist(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isPacifist(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(KEY)) {
            event.getEntity().getPersistentData().putBoolean(KEY, true);
        }
    }
}
