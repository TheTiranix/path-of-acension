package com.tcorigenes.tcorigenes.favor.network;

import com.tcorigenes.tcorigenes.favor.Deity;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: el estado completo de favor del jugador, para la HUD. Se manda en
 *  login/respawn y cada vez que el favor cambia (ver FavorManager). */
public class FavorSyncPacket {
    private final Map<Deity, Integer> favor;

    public FavorSyncPacket(Map<Deity, Integer> favor) {
        this.favor = favor;
    }

    public FavorSyncPacket(FriendlyByteBuf buf) {
        Map<Deity, Integer> map = new EnumMap<>(Deity.class);
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            Deity deity = Deity.values()[buf.readVarInt()];
            map.put(deity, buf.readVarInt());
        }
        this.favor = map;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.favor.size());
        this.favor.forEach((deity, value) -> {
            buf.writeVarInt(deity.ordinal());
            buf.writeVarInt(value);
        });
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> com.tcorigenes.tcorigenes.favor.client.ClientFavorData.onSync(this.favor)));
        return true;
    }
}
