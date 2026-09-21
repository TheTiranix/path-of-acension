// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tcorigenes.tcorigenes.attributes.RaceAttributeManager;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.core.capability.RaceSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * /despurificar [jugador]: devuelve a un Malnacido purificado a su estado original (maldito): vuelven la piel
 * deforme, las penalizaciones y se va la Regeneracion II permanente. Solo operadores.
 */
public class UnpurifyCommand {
    public UnpurifyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("despurificar").requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(context -> run(context.getSource(), EntityArgument.getPlayer(context, "jugador"))))
        );
    }

    private static int run(CommandSourceStack source, ServerPlayer target) {
        boolean isMalnacido = target.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace() == Race.MALNACIDO).orElse(false);
        if (!isMalnacido) {
            source.sendFailure(Component.literal(target.getName().getString() + " no es Malnacido."));
            return 0;
        }
        if (!target.getPersistentData().getBoolean("malnacido_purificado")) {
            source.sendFailure(Component.literal(target.getName().getString() + " ya es un Malnacido sin purificar."));
            return 0;
        }
        target.getPersistentData().remove("malnacido_purificado");
        // La Regeneracion II permanente de la purificacion (duracion "infinita"): se quita solo esa.
        MobEffectInstance regen = target.getEffect(MobEffects.REGENERATION);
        if (regen != null && regen.getDuration() > 100000) {
            target.removeEffect(MobEffects.REGENERATION);
        }
        RaceAttributeManager.updateAttributes(target, Race.MALNACIDO);
        RaceSync.broadcast(target, Race.MALNACIDO);
        target.displayClientMessage(Component.literal("La maldición vuelve a ti."), false);
        source.sendSuccess(() -> Component.literal(target.getName().getString() + " volvió a ser un Malnacido maldito."), true);
        return 1;
    }
}
