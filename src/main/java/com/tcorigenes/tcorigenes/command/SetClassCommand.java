package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tcorigenes.tcorigenes.playerclass.ClassAttributeManager;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SetClassCommand {
    public SetClassCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setclass").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("clase", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (PlayerClass playerClass : PlayerClass.values()) {
                                        builder.suggest(playerClass.name().toLowerCase());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String className = StringArgumentType.getString(context, "clase").toUpperCase();

                                    try {
                                        PlayerClass selectedClass = PlayerClass.valueOf(className);
                                        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                                                .ifPresent(data -> data.setPlayerClass(selectedClass));
                                        ClassAttributeManager.updateAttributes(player, selectedClass);
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Tu clase ha sido establecida a: " + selectedClass.getDisplayName()), true);
                                        return 1;
                                    } catch (IllegalArgumentException e) {
                                        context.getSource().sendFailure(Component.literal("La clase '" + className + "' no existe."));
                                        return 0;
                                    }
                                })
                        )
        );
    }
}
