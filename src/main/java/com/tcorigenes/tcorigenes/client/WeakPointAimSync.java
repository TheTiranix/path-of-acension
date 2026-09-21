// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.core.WeakPointEntity;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.WeakPointAimPacket;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Le avisa al servidor donde esta el punto debil sobre el MODELO de cada mob marcado por este jugador (ver
 * WeakPointModelAnchor), porque el servidor no tiene modelos y necesita ese punto para saber si el golpe acerto.
 * Solo manda cuando el punto se movio o cada segundo, para no saturar la red.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class WeakPointAimSync {
    private static final Map<Integer, Vec3> LAST_SENT = new HashMap<>();
    private static final Map<Integer, Long> LAST_TIME = new HashMap<>();

    private WeakPointAimSync() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            LAST_SENT.clear();
            LAST_TIME.clear();
            return;
        }
        if (mc.player.tickCount % 3 != 0) {
            return;
        }
        long now = mc.level.getGameTime();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof WeakPointEntity marker) || !mc.player.getUUID().equals(marker.getOwnerId())) {
                continue;
            }
            if (!(mc.level.getEntity(marker.getTargetId()) instanceof LivingEntity target)) {
                continue;
            }
            Vec3 offset = WeakPointModelAnchor.offset(target, marker.position(), 1.0F, mc.getEntityRenderDispatcher());
            if (offset == null) {
                continue; // modelo ilegible: el servidor usa el punto por hitbox
            }
            Vec3 point = marker.position().add(offset);
            Vec3 last = LAST_SENT.get(marker.getId());
            long lastTime = LAST_TIME.getOrDefault(marker.getId(), -1000L);
            if (last == null || last.distanceToSqr(point) > 0.0064 || now - lastTime >= 20) {
                Networking.sendToServer(new WeakPointAimPacket(marker.getId(), point));
                LAST_SENT.put(marker.getId(), point);
                LAST_TIME.put(marker.getId(), now);
            }
        }
    }
}
