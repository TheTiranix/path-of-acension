// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.core.Race;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Cuanto tiene abiertas las alas cada Angel (0 = plegadas, 1 = abiertas), para animar que se abren al planear o
 * volar y se pliegan al tocar el suelo. Corre para todos los jugadores del nivel, asi tambien se ve en los demas.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class WingAnimation {
    private static final float OPEN_STEP = 0.14F;
    private static final float CLOSE_STEP = 0.08F;
    /** [actual, anterior] por jugador. */
    private static final Map<UUID, float[]> STATE = new HashMap<>();

    private WingAnimation() {
    }

    public static float openness(AbstractClientPlayer player, float partialTick) {
        float[] state = STATE.get(player.getUUID());
        return state == null ? 0.0F : Mth.lerp(partialTick, state[1], state[0]);
    }

    private static boolean gliding(AbstractClientPlayer player) {
        if (player.getAbilities().flying) {
            return true;
        }
        return !player.onGround() && !player.isInWater() && !player.isPassenger() && !player.isFallFlying()
                && player.getDeltaMovement().y < -0.03;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            STATE.clear();
            return;
        }
        STATE.keySet().removeIf(id -> mc.level.getPlayerByUUID(id) == null);
        for (AbstractClientPlayer player : mc.level.players()) {
            if (ClientRaceData.get(player.getUUID()) != Race.ANGEL) {
                continue;
            }
            float[] state = STATE.computeIfAbsent(player.getUUID(), id -> new float[2]);
            state[1] = state[0];
            float target = gliding(player) ? 1.0F : 0.0F;
            state[0] = Mth.clamp(target, state[0] - CLOSE_STEP, state[0] + OPEN_STEP);
        }
    }
}
