package com.tcorigenes.tcorigenes.networking.packet;

import com.tcorigenes.tcorigenes.attributes.RaceAttributeManager;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.core.RaceSkillGrant;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class ChooseRacePacket {
    private final Race race;

    public ChooseRacePacket(Race race) {
        this.race = race;
    }

    public ChooseRacePacket(FriendlyByteBuf buf) {
        this.race = buf.readEnum(Race.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.race);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(playerRace -> {
                    playerRace.setRace(this.race);
                    RaceAttributeManager.updateAttributes(player, this.race);
                    com.tcorigenes.tcorigenes.core.capability.RaceSync.broadcast(player, this.race);
                    RaceSkillGrant.grant(player, this.race);
                    com.tcorigenes.tcorigenes.favor.FavorManager.grantRaceStartingFavor(player, this.race);
 if (this.race == Race.HEREJE) {
                        com.tcorigenes.tcorigenes.favor.FavorManager.clampHerejeFavor(player);
                    }
                    player.displayClientMessage(Component.literal("Has elegido el origen: " + this.race.getDisplayName()), false);
                });
            }
        });
        return true;
    }
}
