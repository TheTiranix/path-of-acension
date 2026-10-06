// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import com.tcorigenes.tcorigenes.core.ProgressionTier;
import com.tcorigenes.tcorigenes.favor.FavorManager;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: la tecla del perfil pide los datos que el cliente no tiene (clase y nivel de progresion). Responde con ProfileDataPacket. */
public class OpenProfilePacket {
    public OpenProfilePacket() {
    }

    public OpenProfilePacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            FavorManager.syncToClient(player);
            PlayerClass playerClass = player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                    .map(data -> data.getPlayerClass()).orElse(PlayerClass.NINGUNA);
            Networking.sendToPlayer(player, new ProfileDataPacket(ProgressionTier.get(player), playerClass.ordinal()));
        });
        return true;
    }
}
