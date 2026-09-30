// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: si el modo pacifico del jugador esta activo (para el boton del inventario). */
public class PacifistSyncPacket {
    private final boolean active;

    public PacifistSyncPacket(boolean active) {
        this.active = active;
    }

    public PacifistSyncPacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.active);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tcorigenes.tcorigenes.client.PacifistButton.active = this.active));
        return true;
    }
}
