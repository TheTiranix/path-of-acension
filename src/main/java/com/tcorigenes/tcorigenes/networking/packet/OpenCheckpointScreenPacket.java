// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: abre la pantalla de puntos de guardado apenas el jugador se duerme de
 *  verdad, ANTES de crear ningun punto/copia nueva (eso se decide recien si elige "guardar aca",
 *  ver ChooseCheckpointPacket). */
public class OpenCheckpointScreenPacket {
    /** Un punto activo tal como se le muestra al cliente (nombre de dueño y dimension ya
     *  resueltos del lado server, no hace falta que el cliente sepa nada mas). */
    public record CheckpointEntry(UUID id, String ownerName, String dimensionLabel, BlockPos pos, boolean preferred) {
    }

    private final List<CheckpointEntry> entries;

    public OpenCheckpointScreenPacket(List<CheckpointEntry> entries) {
        this.entries = entries;
    }

    public OpenCheckpointScreenPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<CheckpointEntry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new CheckpointEntry(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readBlockPos(), buf.readBoolean()));
        }
        this.entries = list;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entries.size());
        for (CheckpointEntry entry : this.entries) {
            buf.writeUUID(entry.id());
            buf.writeUtf(entry.ownerName());
            buf.writeUtf(entry.dimensionLabel());
            buf.writeBlockPos(entry.pos());
            buf.writeBoolean(entry.preferred());
        }
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.tcorigenes.tcorigenes.client.gui.CheckpointScreen(this.entries))));
        return true;
    }
}
