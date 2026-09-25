// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.network;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: puntos, clase y ids desbloqueados, para dibujar la pantalla del arbol. */
public class SkillSyncPacket {
    private final int points;
    private final long xp;
    private final int bought;
    private final String className;
    private final Set<String> unlocked;

    public SkillSyncPacket(int points, long xp, int bought, String className, Set<String> unlocked) {
        this.points = points;
        this.xp = xp;
        this.bought = bought;
        this.className = className;
        this.unlocked = unlocked;
    }

    public SkillSyncPacket(FriendlyByteBuf buf) {
        this.points = buf.readVarInt();
        this.xp = buf.readVarLong();
        this.bought = buf.readVarInt();
        this.className = buf.readUtf();
        int size = buf.readVarInt();
        this.unlocked = new HashSet<>();
        for (int i = 0; i < size; i++) {
            this.unlocked.add(buf.readUtf());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(points);
        buf.writeVarLong(xp);
        buf.writeVarInt(bought);
        buf.writeUtf(className);
        buf.writeVarInt(unlocked.size());
        unlocked.forEach(buf::writeUtf);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tcorigenes.tcorigenes.progression.client.ClientSkillData.set(points, xp, bought, className, unlocked)));
        return true;
    }
}
