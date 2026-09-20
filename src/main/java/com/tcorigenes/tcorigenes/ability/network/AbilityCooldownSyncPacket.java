// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: confirma si la activacion funciono (y con que cooldown real hay que
 *  arrancar la cuenta regresiva del HUD) o si todavia estaba en cooldown (para resincronizar
 *  al cliente si por lag pensaba que ya estaba lista). */
public class AbilityCooldownSyncPacket {
    private final String abilityId;
    private final int cooldownMillis;
    private final boolean activated;

    public AbilityCooldownSyncPacket(String abilityId, int cooldownMillis, boolean activated) {
        this.abilityId = abilityId;
        this.cooldownMillis = cooldownMillis;
        this.activated = activated;
    }

    public AbilityCooldownSyncPacket(FriendlyByteBuf buf) {
        this.abilityId = buf.readUtf();
        this.cooldownMillis = buf.readVarInt();
        this.activated = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.abilityId);
        buf.writeVarInt(this.cooldownMillis);
        buf.writeBoolean(this.activated);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> com.tcorigenes.tcorigenes.ability.client.ClientAbilityCooldowns.onSync(this.abilityId, this.cooldownMillis)));
        return true;
    }
}
