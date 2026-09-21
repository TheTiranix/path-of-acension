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
 * Cliente -> servidor: el punto del MODELO del mob donde esta el marcador de punto debil (solo el cliente tiene
 * los modelos). El servidor lo valida: tiene que ser del dueño y caer dentro del cuerpo del mob marcado; si no,
 * se ignora y se usa el punto por hitbox.
 */
public class WeakPointAimPacket {
    private final int markerId;
    private final double x;
    private final double y;
    private final double z;

    public WeakPointAimPacket(int markerId, Vec3 point) {
        this.markerId = markerId;
        this.x = point.x;
        this.y = point.y;
        this.z = point.z;
    }

    public WeakPointAimPacket(FriendlyByteBuf buf) {
        this.markerId = buf.readVarInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.markerId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
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
            if (target.getBoundingBox().inflate(1.0).contains(point)) {
                marker.setModelAim(point, player.level().getGameTime());
            }
        });
        return true;
    }
}
