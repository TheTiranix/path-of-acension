// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: clase y nivel de progresion del jugador; al llegar abre la pantalla de perfil. */
public class ProfileDataPacket {
    private final int tier;
    private final int classOrdinal;

    public ProfileDataPacket(int tier, int classOrdinal) {
        this.tier = tier;
        this.classOrdinal = classOrdinal;
    }

    public ProfileDataPacket(FriendlyByteBuf buf) {
        this.tier = buf.readVarInt();
        this.classOrdinal = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.tier);
        buf.writeVarInt(this.classOrdinal);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tcorigenes.tcorigenes.client.gui.ProfileScreen.open(this.tier, this.classOrdinal)));
        return true;
    }
}
