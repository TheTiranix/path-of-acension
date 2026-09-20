// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.networking;

import com.tcorigenes.tcorigenes.ability.network.AbilityCooldownSyncPacket;
import com.tcorigenes.tcorigenes.ability.network.ActivateAbilityPacket;
import com.tcorigenes.tcorigenes.ability.network.ActivateRacialAbilityPacket;
import com.tcorigenes.tcorigenes.favor.network.FavorSyncPacket;
import com.tcorigenes.tcorigenes.networking.packet.ChooseClassPacket;
import com.tcorigenes.tcorigenes.networking.packet.ChooseRacePacket;
import com.tcorigenes.tcorigenes.networking.packet.OpenClassScreenPacket;
import com.tcorigenes.tcorigenes.networking.packet.DropsResultPacket;
import com.tcorigenes.tcorigenes.networking.packet.RaceSyncPacket;
import com.tcorigenes.tcorigenes.progression.network.SkillSyncPacket;
import com.tcorigenes.tcorigenes.progression.network.UnlockNodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Networking {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath("tcorigenes", "messages"))
                .networkProtocolVersion(() -> "1.1")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ChooseRacePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ChooseRacePacket::new)
                .encoder(ChooseRacePacket::toBytes)
                .consumerMainThread(ChooseRacePacket::handle)
                .add();

        net.messageBuilder(ActivateAbilityPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ActivateAbilityPacket::new)
                .encoder(ActivateAbilityPacket::toBytes)
                .consumerMainThread(ActivateAbilityPacket::handle)
                .add();

        net.messageBuilder(AbilityCooldownSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(AbilityCooldownSyncPacket::new)
                .encoder(AbilityCooldownSyncPacket::toBytes)
                .consumerMainThread(AbilityCooldownSyncPacket::handle)
                .add();

        net.messageBuilder(UnlockNodePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UnlockNodePacket::new)
                .encoder(UnlockNodePacket::toBytes)
                .consumerMainThread(UnlockNodePacket::handle)
                .add();

        net.messageBuilder(SkillSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SkillSyncPacket::new)
                .encoder(SkillSyncPacket::toBytes)
                .consumerMainThread(SkillSyncPacket::handle)
                .add();

        net.messageBuilder(ActivateRacialAbilityPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ActivateRacialAbilityPacket::new)
                .encoder(ActivateRacialAbilityPacket::toBytes)
                .consumerMainThread(ActivateRacialAbilityPacket::handle)
                .add();

        net.messageBuilder(FavorSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FavorSyncPacket::new)
                .encoder(FavorSyncPacket::toBytes)
                .consumerMainThread(FavorSyncPacket::handle)
                .add();

        net.messageBuilder(RaceSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RaceSyncPacket::new)
                .encoder(RaceSyncPacket::toBytes)
                .consumerMainThread(RaceSyncPacket::handle)
                .add();

        net.messageBuilder(ChooseClassPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ChooseClassPacket::new)
                .encoder(ChooseClassPacket::toBytes)
                .consumerMainThread(ChooseClassPacket::handle)
                .add();

        net.messageBuilder(OpenClassScreenPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenClassScreenPacket::new)
                .encoder(OpenClassScreenPacket::toBytes)
                .consumerMainThread(OpenClassScreenPacket::handle)
                .add();

        net.messageBuilder(DropsResultPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(DropsResultPacket::new)
                .encoder(DropsResultPacket::toBytes)
                .consumerMainThread(DropsResultPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), message);
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
