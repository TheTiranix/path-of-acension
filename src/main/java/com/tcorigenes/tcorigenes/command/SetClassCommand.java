// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
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
                                        com.tcorigenes.tcorigenes.playerclass.ClassSelection.apply(player, selectedClass);
                                        context.getSource().sendSuccess(
                                                () -> Component.translatable("pa.msg.b653946d17", selectedClass.getDisplayName()), true);
                                        return 1;
                                    } catch (IllegalArgumentException e) {
                                        context.getSource().sendFailure(Component.translatable("pa.msg.60e2d8b10d", className));
                                        return 0;
                                    }
                                })
                        )
        );
    }
}
