// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tcorigenes.tcorigenes.favor.Deity;
import com.tcorigenes.tcorigenes.favor.FavorManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /favor add|get|list: admin (nivel 2), para probar/otorgar favor hasta que haya una fuente organica (altares, misiones). */
public class FavorCommand {
    public FavorCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("favor").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("deity", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (Deity deity : Deity.values()) {
                                                        builder.suggest(deity.name().toLowerCase());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(context -> {
                                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                                            Deity deity = Deity.valueOf(StringArgumentType.getString(context, "deity").toUpperCase());
                                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                                            FavorManager.addFavor(target, deity, amount);
                                                            int newValue = FavorManager.getFavor(target, deity);
                                                            context.getSource().sendSuccess(() -> Component.literal(
                                                                    target.getName().getString() + " ahora tiene " + newValue + " de favor de " + deity.getDisplayName()), true);
                                                            return 1;
                                                        })))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            StringBuilder message = new StringBuilder(target.getName().getString() + ": ");
                                            for (Deity deity : Deity.values()) {
                                                message.append(deity.getDisplayName()).append("=").append(FavorManager.getFavor(target, deity)).append("  ");
                                            }
                                            context.getSource().sendSuccess(() -> Component.literal(message.toString()), false);
                                            return 1;
                                        })))
        );
    }
}
