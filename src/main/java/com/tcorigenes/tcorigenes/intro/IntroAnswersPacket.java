// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.intro;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: las respuestas de la introduccion (indice elegido en cada pregunta). El servidor calcula el resultado. */
public class IntroAnswersPacket {
    private final int[] answers;

    public IntroAnswersPacket(int[] answers) {
        this.answers = answers;
    }

    public IntroAnswersPacket(FriendlyByteBuf buf) {
        this.answers = buf.readVarIntArray(32);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarIntArray(this.answers);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                IntroManager.finish(player, this.answers);
            }
        });
        return true;
    }
}
