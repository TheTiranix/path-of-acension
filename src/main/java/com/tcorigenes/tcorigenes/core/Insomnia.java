// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido de alejandr0: 1 de cada 7 dias el jugador tiene insomnio y no puede dormir porque tiene pesadillas. Se decide por
 * jugador y por dia del mundo (siempre da lo mismo dentro de un mismo dia, asi no se arregla tocando la cama otra vez).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class Insomnia {
    private Insomnia() {
    }

    public static boolean hasInsomnia(Player player) {
        long day = player.level().getDayTime() / 24000L;
        Random random = new Random(player.getUUID().getLeastSignificantBits() * 31L + player.getUUID().getMostSignificantBits() + day * 7919L);
        return random.nextInt(7) == 0;
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isCreative() && hasInsomnia(player)) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            player.displayClientMessage(Component.translatable("pa.msg.cd05d77f2a"), true);
        }
    }
}
