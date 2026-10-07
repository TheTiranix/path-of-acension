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
            Race.ENDER_WARRIOR, "tcorigenes:teletransporte_ender",
            Race.AUTOMATA, "tcorigenes:sobrecarga_automata",
            Race.ANGEL, "tcorigenes:impulso_angel",
            Race.DEVOTO, "tcorigenes:plegaria_devoto",
            Race.DEMONIO, "tcorigenes:aura_demonio",
            Race.SIERVO_DE_LA_LUNA, "tcorigenes:corrupcion_luna",
            Race.MALNACIDO, "tcorigenes:golpe_malnacido",
            Race.STONE_GIANT, "tcorigenes:bloque_gigante"
    );

    public ActivateRacialAbilityPacket() {
    }

    public ActivateRacialAbilityPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    private static boolean activateAndReport(PlayerAbility ability, ServerPlayer player) {
        ability.activate(player);
        return true;
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
                    // las habilidades que pueden fallar (sin blanco, sin bloque...) no gastan cooldown si no se usaron
                    boolean used = ability instanceof com.tcorigenes.tcorigenes.ability.racial.RacialAbilities.Racial racial
                            ? racial.tryActivate(player) : activateAndReport(ability, player);
                    if (!used) {
                        return;
                    }
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
