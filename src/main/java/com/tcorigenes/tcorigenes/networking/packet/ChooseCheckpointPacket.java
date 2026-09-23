// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import com.tcorigenes.tcorigenes.checkpoint.Checkpoint;
import com.tcorigenes.tcorigenes.checkpoint.CheckpointManager;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: respuesta a la pantalla de puntos de guardado (ver OpenCheckpointScreenPacket).
 *  Sin id: "guardar aca" (crea un punto nuevo en la cama donde se durmio). Con id: elegir ese punto
 *  como preferido (reemplaza a /respawnpoint choose). */
public class ChooseCheckpointPacket {
    private final UUID checkpointId;
    /** true = cargar ese save ahora (pantalla de entrada al mundo); false = solo elegirlo como preferido. */
    private final boolean load;

    public ChooseCheckpointPacket(UUID checkpointId, boolean load) {
        this.checkpointId = checkpointId;
        this.load = load;
    }

    public ChooseCheckpointPacket(FriendlyByteBuf buf) {
        this.checkpointId = buf.readBoolean() ? buf.readUUID() : null;
        this.load = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.checkpointId != null);
        if (this.checkpointId != null) {
            buf.writeUUID(this.checkpointId);
        }
        buf.writeBoolean(this.load);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (this.checkpointId == null) {
                player.getSleepingPos().ifPresent(pos -> CheckpointManager.createCheckpoint(player, pos));
                return;
            }
            List<Checkpoint> active = CheckpointManager.active(player.getServer());
            active.stream().filter(c -> c.id.equals(this.checkpointId)).findFirst().ifPresent(checkpoint -> {
                CheckpointManager.setPreferred(player, checkpoint.id);
                if (this.load && player.getServer().isSingleplayer()) {
                    com.tcorigenes.tcorigenes.checkpoint.WorldRestoreManager.beginRestore(player.getServer(), checkpoint,
                            "Cargando el punto de guardado elegido: la partida se corta en 5 segundos, se restaura y podés volver a entrar.");
                }
            });
        });
        return true;
    }
}
