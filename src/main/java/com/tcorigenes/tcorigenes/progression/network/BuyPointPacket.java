// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.network;

import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: canjear XP por un punto de habilidad. El precio y el saldo se validan en SkillTreeManager. */
public class BuyPointPacket {
    public BuyPointPacket() {
    }

    public BuyPointPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SkillTreeManager.buyPoint(player);
            }
        });
        return true;
    }
}
