package com.tcorigenes.tcorigenes.ability.network;

import com.tcorigenes.tcorigenes.ability.AbilityCooldownManager;
import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.ability.PlayerAbility;
import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente -> servidor: "intenta activar mi habilidad equipada". Sin payload: el servidor
 *  decide cual es segun la capability, nunca confia en lo que mande el cliente. */
public class ActivateAbilityPacket {
    public ActivateAbilityPacket() {
    }

    public ActivateAbilityPacket(FriendlyByteBuf buf) {
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
            player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY).ifPresent(loadout -> {
                PlayerAbility ability = AbilityRegistry.get(loadout.getEquippedAbilityId());
                if (ability == null) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "No tenés ninguna habilidad equipada: desbloqueá la de tu clase en el árbol (tecla H)."), true);
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
