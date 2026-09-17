package com.tcorigenes.tcorigenes.networking.packet;

import com.tcorigenes.tcorigenes.core.Race;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Server -> TODOS los clientes: que raza tiene tal jugador (y si es Malnacido, si ya se
 * purifico). Hace falta para que las cosas visuales de raza (cuernos, alas, altura, piel
 * deformada) se vean en otros clientes en multijugador, no solo en el dueño (la Capability en
 * si, y el flag de purificacion en persistentData, son solo server-side).
 */
public class RaceSyncPacket {
    private final UUID playerId;
    private final Race race;
    private final boolean purified;

    public RaceSyncPacket(UUID playerId, Race race, boolean purified) {
        this.playerId = playerId;
        this.race = race;
        this.purified = purified;
    }

    public RaceSyncPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.race = buf.readEnum(Race.class);
        this.purified = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeEnum(this.race);
        buf.writeBoolean(this.purified);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tcorigenes.tcorigenes.client.ClientRaceData.set(playerId, race, purified)));
        return true;
    }
}
