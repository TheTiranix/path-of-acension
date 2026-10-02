// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.intro;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Servidor -> cliente: la raza y la clase asignadas, para la escena final. */
public class IntroResultPacket {
    private final Race race;
    private final PlayerClass playerClass;

    public IntroResultPacket(Race race, PlayerClass playerClass) {
        this.race = race;
        this.playerClass = playerClass;
    }

    public IntroResultPacket(FriendlyByteBuf buf) {
        this.race = buf.readEnum(Race.class);
        this.playerClass = buf.readEnum(PlayerClass.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.race);
        buf.writeEnum(this.playerClass);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tcorigenes.tcorigenes.intro.client.IntroScreen.onResult(this.race, this.playerClass)));
        return true;
    }
}
