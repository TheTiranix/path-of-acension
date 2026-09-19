package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /grantabilitypoints <cantidad> (a uno mismo) o /grantabilitypoints <jugador> <cantidad>:
 * da puntos para el arbol de habilidades. La variante con jugador sirve para recompensas de
 * quests (FTB Quests puede ejecutar comandos como recompensa).
 */
public class GrantSkillPointsCommand {
    public GrantSkillPointsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("grantabilitypoints").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    grant(context.getSource(), context.getSource().getPlayerOrException(),
                                            IntegerArgumentType.getInteger(context, "cantidad"));
                                    return 1;
                                }))
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            grant(context.getSource(), EntityArgument.getPlayer(context, "jugador"),
                                                    IntegerArgumentType.getInteger(context, "cantidad"));
                                            return 1;
                                        })))
        );
    }

    private static void grant(CommandSourceStack source, ServerPlayer target, int amount) {
        SkillTreeManager.addPoints(target, amount);
        source.sendSuccess(() -> Component.literal("Puntos de habilidad otorgados a " + target.getName().getString() + ": " + amount), true);
    }
}
