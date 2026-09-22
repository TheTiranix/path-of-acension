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
 * WeakPointModelAnchor), porque el servidor no tiene modelos y necesita ese punto (y su radio) para saber si
 * el golpe acerto. Solo manda cuando el punto se movio, el radio cambio, o cada segundo, para no saturar la red.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class WeakPointAimSync {
    private record Sent(Vec3 point, double radius, long time) {
    }

    private static final Map<Integer, Sent> LAST_SENT = new HashMap<>();

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
            WeakPointModelAnchor.Result result = WeakPointModelAnchor.compute(target, marker.position(), 1.0F,
                    mc.getEntityRenderDispatcher(), marker.isPriorityHeight());
            if (result == null) {
                continue; // modelo ilegible: el servidor usa el punto/radio por hitbox
            }
            Vec3 point = marker.position().add(result.offset());
            Sent last = LAST_SENT.get(marker.getId());
            boolean stale = last == null || last.point().distanceToSqr(point) > 0.0064
                    || Math.abs(last.radius() - result.radius()) > 0.03 || now - last.time() >= 20;
            if (stale) {
                Networking.sendToServer(new WeakPointAimPacket(marker.getId(), point, result.radius()));
                LAST_SENT.put(marker.getId(), new Sent(point, result.radius(), now));
            }
        }
    }
}
