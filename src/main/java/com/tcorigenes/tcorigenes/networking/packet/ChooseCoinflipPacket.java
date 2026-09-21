// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import com.tcorigenes.tcorigenes.coinflip.CoinflipManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: cara (true) o cruz (false). El servidor tira la moneda; nunca se confia en el cliente. */
public class ChooseCoinflipPacket {
    private final boolean heads;

    public ChooseCoinflipPacket(boolean heads) {
        this.heads = heads;
    }

    public ChooseCoinflipPacket(FriendlyByteBuf buf) {
        this.heads = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.heads);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CoinflipManager.onChoice(player, this.heads);
            }
        });
        return true;
    }
}
