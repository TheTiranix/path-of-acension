// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.core.Race;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pasivas raciales que antes eran MobEffects (y se veian como efectos de pocion): el planeo del Angel (caida lenta) se
 * hace ahora directamente sobre el movimiento del jugador local, sin efecto.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class RacialPassivesClient {
    private static final double GLIDE_MAX_FALL_SPEED = -0.08; // 60% mas rapido que -0.05; por debajo de -0.03125 el servidor no lo toma por vuelo

    private RacialPassivesClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.onGround() || player.getAbilities().flying || player.isInWater()) {
            return;
        }
        // la Capability de raza solo vive en el servidor: en el cliente la raza llega por RaceSyncPacket (ClientRaceData)
        boolean angel = ClientRaceData.get(player.getUUID()) == Race.ANGEL;
        Vec3 motion = player.getDeltaMovement();
        // mantener agacharse (Shift por defecto) = caer sin planear (igual sin daño de caida: eso lo anula el servidor)
        if (angel && !player.isShiftKeyDown() && motion.y < GLIDE_MAX_FALL_SPEED && !player.isFallFlying()) {
            player.setDeltaMovement(motion.x, GLIDE_MAX_FALL_SPEED, motion.z);
            player.fallDistance = 0.0F;
        }
    }
}
