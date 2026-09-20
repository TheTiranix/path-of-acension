// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.item.ModItems;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.ChooseRacePacket;
import com.tcorigenes.tcorigenes.networking.packet.OpenClassScreenPacket;
import com.tcorigenes.tcorigenes.playerclass.ClassSelection;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Primera vez que entras al mundo: recibis el Orbe de Origenes (una sola vez, aunque mueras o
 * relogees). Si ya elegiste raza pero cerraste el juego sin elegir clase, la pantalla de clase
 * se vuelve a abrir al entrar.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class FirstJoinRules {
    private static final String ORB_GIVEN_KEY = "tc_orb_given";

    private FirstJoinRules() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // PERSISTED_NBT_TAG sobrevive a la muerte y al cambio de dimension.
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean(ORB_GIVEN_KEY)) {
            ItemStack orb = new ItemStack(ModItems.ORBE_DE_ORIGENES.get());
            if (!player.getInventory().add(orb)) {
                player.drop(orb, false);
            }
            persisted.putBoolean(ORB_GIVEN_KEY, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        if (ChooseRacePacket.isRaceChosen(player)
                && ClassSelection.currentClass(player) == PlayerClass.NINGUNA) {
            Networking.sendToPlayer(player, new OpenClassScreenPacket());
        }
    }
}
