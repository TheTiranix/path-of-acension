// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import com.tcorigenes.tcorigenes.core.WeakPointEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Cliente -> servidor: el punto del MODELO del mob donde esta el marcador de punto debil, y el radio de
 * acierto que le corresponde por su tamaño (solo el cliente tiene los modelos). El servidor lo valida: tiene
 * que ser del dueño y caer cerca del mob marcado; si no, se ignora y se usa el punto/radio por hitbox.
 */
public class WeakPointAimPacket {
    private static final double MAX_RADIUS = 3.0;

    private final int markerId;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;

    public WeakPointAimPacket(int markerId, Vec3 point, double radius) {
        this.markerId = markerId;
        this.x = point.x;
        this.y = point.y;
        this.z = point.z;
        this.radius = radius;
    }

    public WeakPointAimPacket(FriendlyByteBuf buf) {
        this.markerId = buf.readVarInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.radius = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.markerId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeDouble(this.radius);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            Entity entity = player.level().getEntity(this.markerId);
            if (!(entity instanceof WeakPointEntity marker) || !player.getUUID().equals(marker.getOwnerId())) {
                return;
            }
            if (!(player.level().getEntity(marker.getTargetId()) instanceof LivingEntity target)) {
                return;
            }
            Vec3 point = new Vec3(this.x, this.y, this.z);
            double radius = Math.max(0.1, Math.min(MAX_RADIUS, this.radius));
            if (target.getBoundingBox().inflate(3.0).contains(point)) {
                marker.setModelAim(point, radius, player.level().getGameTime());
            }
        });
        return true;
    }
}
