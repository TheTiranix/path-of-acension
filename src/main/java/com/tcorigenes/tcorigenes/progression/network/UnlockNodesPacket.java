// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.network;

import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: gastar puntos para desbloquear VARIOS nodos de una vez (pedido de alejandr0:
 *  elegir todos los que quiere y confirmar una sola vez). Misma validacion que UnlockNodePacket, ver
 *  SkillTreeManager#tryUnlockBatch: nunca se confia en el cliente. */
public class UnlockNodesPacket {
    private final List<String> nodeIds;

    public UnlockNodesPacket(List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }

    public UnlockNodesPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.nodeIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.nodeIds.add(buf.readUtf());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.nodeIds.size());
        this.nodeIds.forEach(buf::writeUtf);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SkillTreeManager.tryUnlockBatch(player, this.nodeIds);
            }
        });
        return true;
    }
}
