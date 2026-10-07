// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> clientes que ven al jugador: estado visual de una habilidad racial (rezando, o que bloque lleva el Gigante Rocoso). */
public class RacialStateSyncPacket {
    public static final int KIND_PRAYING = 0;
    public static final int KIND_HELD_BLOCK = 1;

    private final int entityId;
    private final int kind;
    private final int value;

    public RacialStateSyncPacket(int entityId, int kind, int value) {
        this.entityId = entityId;
        this.kind = kind;
        this.value = value;
    }

    public RacialStateSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.kind = buf.readVarInt();
        this.value = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.kind);
        buf.writeVarInt(this.value);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tcorigenes.tcorigenes.client.ClientRacialState.apply(this.entityId, this.kind, this.value)));
        return true;
    }
}
