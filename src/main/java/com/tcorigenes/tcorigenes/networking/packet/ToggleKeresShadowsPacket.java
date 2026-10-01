// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import com.tcorigenes.tcorigenes.core.KeresShadowControl;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: activar o desactivar las lanzas negras aereas de la Keres (ver KeresShadowControl). */
public class ToggleKeresShadowsPacket {
    private final boolean enabled;

    public ToggleKeresShadowsPacket(boolean enabled) {
        this.enabled = enabled;
    }

    public ToggleKeresShadowsPacket(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.enabled);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                KeresShadowControl.set(player, this.enabled);
            }
        });
        return true;
    }
}
