// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
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

    /** Fija la raza de un jugador (la usan el Orbe y la introduccion); openClassScreen = abrir la pantalla de clase al terminar. */
    public static void applyRace(ServerPlayer player, Race race, boolean openClassScreen) {
        player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(playerRace -> {
            playerRace.setRace(race);
            RaceAttributeManager.updateAttributes(player, race);
            com.tcorigenes.tcorigenes.core.capability.RaceSync.broadcast(player, race);
            RaceSkillGrant.grant(player, race);
            com.tcorigenes.tcorigenes.core.RaceItemGrant.grant(player, race);
            com.tcorigenes.tcorigenes.favor.FavorManager.grantRaceStartingFavor(player, race);
            if (race == Race.HEREJE) {
                com.tcorigenes.tcorigenes.favor.FavorManager.clampHerejeFavor(player);
            }
            player.displayClientMessage(Component.translatable("pa.msg.e0a0c531f3", race.getDisplayName()), false);
            markRaceChosen(player);
            // Cambiar de origen reinicia la clase: se vuelve a elegir.
            com.tcorigenes.tcorigenes.playerclass.ClassSelection.apply(player, com.tcorigenes.tcorigenes.playerclass.PlayerClass.NINGUNA);
            if (openClassScreen) {
                com.tcorigenes.tcorigenes.networking.Networking.sendToPlayer(player,
                        new com.tcorigenes.tcorigenes.networking.packet.OpenClassScreenPacket());
            }
        });
    }

    /** Saca 1 Orbe de Origenes del inventario; false si no tenia ninguno. */
    public static boolean consumeOrb(ServerPlayer player) {
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
                applyRace(player, this.race, true);
            }
        });
        return true;
    }
}
