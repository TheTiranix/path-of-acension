// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: lado en que cayo la moneda y si el jugador gano (para la animacion). */
public class CoinflipResultPacket {
    private final boolean landedHeads;
    private final boolean won;

    public CoinflipResultPacket(boolean landedHeads, boolean won) {
        this.landedHeads = landedHeads;
        this.won = won;
    }

    public CoinflipResultPacket(FriendlyByteBuf buf) {
        this.landedHeads = buf.readBoolean();
        this.won = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.landedHeads);
        buf.writeBoolean(this.won);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tcorigenes.tcorigenes.client.gui.CoinflipScreen.onResult(this.landedHeads, this.won)));
        return true;
    }
}
