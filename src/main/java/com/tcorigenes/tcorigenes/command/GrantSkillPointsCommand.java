package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /grantabilitypoints <cantidad>: da puntos para gastar en nuestro propio arbol de habilidades. */
public class GrantSkillPointsCommand {
    public GrantSkillPointsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("grantabilitypoints").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(context, "cantidad");
                                    player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                                            .ifPresent(loadout -> loadout.setSkillPoints(loadout.getSkillPoints() + amount));
                                    context.getSource().sendSuccess(() -> Component.literal("Puntos de habilidad otorgados: " + amount), true);
                                    return 1;
                                })
                        )
        );
    }
}
