// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability.network;

import com.tcorigenes.tcorigenes.ability.AbilityCooldownManager;
import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.ability.PlayerAbility;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente->servidor: "activa mi habilidad racial" (distinta del slot de clase/puntos).
 *  El servidor decide segun la raza real del jugador, nunca confia en el cliente. */
public class ActivateRacialAbilityPacket {
    private static final Map<Race, String> RACE_ABILITIES = Map.of(
            Race.ENDER_WARRIOR, "tcorigenes:teletransporte_ender"
    );

    public ActivateRacialAbilityPacket() {
    }

    public ActivateRacialAbilityPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> {
                String abilityId = RACE_ABILITIES.get(raceInfo.getRace());
                if (abilityId == null) {
                    return;
                }
                PlayerAbility ability = AbilityRegistry.get(abilityId);
                if (ability == null) {
                    return;
                }
                long remainingMillis = AbilityCooldownManager.remainingMillis(player, ability);
                if (remainingMillis <= 0) {
                    ability.activate(player);
                    AbilityCooldownManager.startCooldown(player, ability);
                    com.tcorigenes.tcorigenes.networking.Networking.sendToPlayer(player,
                            new AbilityCooldownSyncPacket(ability.id(), ability.cooldownTicks() * 50, true));
                } else {
                    com.tcorigenes.tcorigenes.networking.Networking.sendToPlayer(player,
                            new AbilityCooldownSyncPacket(ability.id(), (int) remainingMillis, false));
                }
            });
        });
        return true;
    }
}
