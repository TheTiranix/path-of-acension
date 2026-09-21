// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /resetskilltree: borra todos los nodos desbloqueados y devuelve los puntos gastados (no se pierde ni se
 * gana nada). Cualquiera puede resetearse a si mismo; resetear a otro jugador requiere ser operador.
 */
public class ResetSkillTreeCommand {
    public ResetSkillTreeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("resetskilltree")
                        .executes(context -> {
                            reset(context.getSource(), context.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    reset(context.getSource(), EntityArgument.getPlayer(context, "jugador"));
                                    return 1;
                                }))
        );
    }

    private static void reset(CommandSourceStack source, ServerPlayer target) {
        int refunded = SkillTreeManager.resetAll(target);
        target.displayClientMessage(Component.literal("Árbol de habilidades reiniciado: recuperaste " + refunded + " punto(s)."), false);
        if (source.getEntity() != target) {
            source.sendSuccess(() -> Component.literal("Árbol reiniciado para " + target.getName().getString()
                    + " (" + refunded + " punto(s) devueltos)."), true);
        }
    }
}
