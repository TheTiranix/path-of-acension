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
                // el servidor alterna su propio estado (asi nunca quedan desfasados cliente y servidor) y avisa cual quedo
                boolean now = !KeresShadowControl.enabled(player);
                KeresShadowControl.set(player, now);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(now
                        ? "Lanzas negras de la Keres: activadas (2 de vida cada una)" : "Lanzas negras de la Keres: desactivadas"), true);
            }
        });
        return true;
    }
}
