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
    public static final String RACE_CHOSEN_KEY = "tc_race_chosen";

    /** Guardado en PERSISTED_NBT_TAG para que sobreviva a la muerte. */
    public static boolean isRaceChosen(ServerPlayer player) {
        return player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getBoolean(RACE_CHOSEN_KEY);
    }

    private static void markRaceChosen(ServerPlayer player) {
        var persisted = player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(RACE_CHOSEN_KEY, true);
        player.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Saca 1 Orbe de Origenes del inventario; false si no tenia ninguno. */
    private static boolean consumeOrb(ServerPlayer player) {
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.is(com.tcorigenes.tcorigenes.item.ModItems.ORBE_DE_ORIGENES.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

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
            if (player != null && consumeOrb(player)) {
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
                    markRaceChosen(player);
                    com.tcorigenes.tcorigenes.networking.Networking.sendToPlayer(player,
                            new com.tcorigenes.tcorigenes.networking.packet.OpenClassScreenPacket());
                });
            }
        });
        return true;
    }
}
