package com.tcorigenes.tcorigenes.progression.network;

import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: gastar puntos para desbloquear un nodo. Toda la validacion (clase,
 *  padres, puntos) se hace en SkillTreeManager; nunca se confia en el cliente. */
public class UnlockNodePacket {
    private final String nodeId;

    public UnlockNodePacket(String nodeId) {
        this.nodeId = nodeId;
    }

    public UnlockNodePacket(FriendlyByteBuf buf) {
        this.nodeId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.nodeId);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SkillTreeManager.tryUnlock(player, this.nodeId);
            }
        });
        return true;
    }
}
