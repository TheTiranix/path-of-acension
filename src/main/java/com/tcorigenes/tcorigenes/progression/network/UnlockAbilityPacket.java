package com.tcorigenes.tcorigenes.progression.network;

import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.ability.PlayerAbility;
import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Cliente->servidor: "quiero gastar puntos para desbloquear esta habilidad". El costo y la
 *  validacion de puntos vive acá, nunca se confia en el cliente. */
public class UnlockAbilityPacket {
    private static final int COST = 2;
    private final String abilityId;

    public UnlockAbilityPacket(String abilityId) {
        this.abilityId = abilityId;
    }

    public UnlockAbilityPacket(FriendlyByteBuf buf) {
        this.abilityId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.abilityId);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            PlayerAbility ability = AbilityRegistry.get(this.abilityId);
            if (ability == null) {
                return;
            }
            player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY).ifPresent(loadout -> {
                if (loadout.isUnlocked(this.abilityId)) {
                    return;
                }
                if (loadout.getSkillPoints() < COST) {
                    player.displayClientMessage(Component.literal("No tenés suficientes puntos."), true);
                    return;
                }
                loadout.setSkillPoints(loadout.getSkillPoints() - COST);
                loadout.unlockAbility(this.abilityId);
                player.displayClientMessage(Component.literal("¡Desbloqueaste " + ability.id() + "!"), true);
            });
        });
        return true;
    }
}
