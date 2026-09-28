// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking.packet;

import com.tcorigenes.tcorigenes.playerclass.ClassSelection;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: elige la clase. Solo vale si el jugador ya eligio raza y todavia no tiene clase. */
public class ChooseClassPacket {
    private final PlayerClass playerClass;

    public ChooseClassPacket(PlayerClass playerClass) {
        this.playerClass = playerClass;
    }

    public ChooseClassPacket(FriendlyByteBuf buf) {
        this.playerClass = buf.readEnum(PlayerClass.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.playerClass);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || this.playerClass == PlayerClass.NINGUNA) {
                return;
            }
            if (!ChooseRacePacket.isRaceChosen(player)
                    || ClassSelection.currentClass(player) != PlayerClass.NINGUNA) {
                return;
            }
            if (this.playerClass == PlayerClass.RITUALISTA_ARCANO
                    && player.getCapability(com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                            .map(info -> info.getRace() == com.tcorigenes.tcorigenes.core.Race.AUTOMATA).orElse(false)) {
                player.displayClientMessage(Component.literal("El Autómata tiene prohibido usar magia: no puede ser Ritualista Arcano."), true);
                return;
            }
            ClassSelection.apply(player, this.playerClass);
            player.displayClientMessage(Component.literal("Has elegido la clase: " + this.playerClass.getDisplayName()), false);
        });
        return true;
    }
}
