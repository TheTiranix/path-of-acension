// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: la apuesta termino sin abrir el cofre (o caduco); cierra la pantalla de cara o cruz. */
public class CloseCoinflipPacket {
    public CloseCoinflipPacket() {
    }

    public CloseCoinflipPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tcorigenes.tcorigenes.client.gui.CoinflipScreen.closeIfOpen()));
        return true;
    }
}
