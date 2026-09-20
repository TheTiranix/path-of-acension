// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tcorigenes.tcorigenes.ability.AbilityCooldownManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /abilitydebug: prende/apaga que tus habilidades no tengan cooldown, para probar rapido. */
public class AbilityDebugCommand {
    public AbilityDebugCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abilitydebug").requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            boolean nowOn = AbilityCooldownManager.toggleDebugNoCooldown(player);
                            context.getSource().sendSuccess(
                                    () -> Component.literal(nowOn ? "Cooldowns de habilidades DESACTIVADOS (modo prueba)." : "Cooldowns de habilidades reactivados."),
                                    true);
                            return 1;
                        })
        );
    }
}
