package com.tcorigenes.tcorigenes.networking.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Server -> cliente: resultado de /drops (item buscado + ids de los mobs que lo sueltan) para abrir la pantalla en grilla. */
public class DropsResultPacket {
    private final ResourceLocation item;
    private final List<ResourceLocation> entities;

    public DropsResultPacket(ResourceLocation item, List<ResourceLocation> entities) {
        this.item = item;
        this.entities = entities;
    }

    public DropsResultPacket(FriendlyByteBuf buf) {
        this.item = buf.readResourceLocation();
        int size = buf.readVarInt();
        this.entities = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.entities.add(buf.readResourceLocation());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(item);
        buf.writeVarInt(entities.size());
        entities.forEach(buf::writeResourceLocation);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.tcorigenes.tcorigenes.client.DropsScreen(item, entities))));
        return true;
    }
}
