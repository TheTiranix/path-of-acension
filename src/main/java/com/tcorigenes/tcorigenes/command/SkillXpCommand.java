// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider;
import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * XP del arbol de habilidades: SOLO se consigue con este comando (lo usan los admins / el sistema de XP que se arme
 * despues). Con la XP cada jugador compra puntos en la pantalla del arbol, y cada punto sale mas caro que el anterior.
 *   /skillxp give <jugador> <cantidad>   suma XP        (permiso 2)
 *   /skillxp take <jugador> <cantidad>   resta XP        (permiso 2)
 *   /skillxp set <jugador> <cantidad>    fija la XP      (permiso 2)
 *   /skillxp                             muestra tu XP y el precio del proximo punto
 */
public class SkillXpCommand {
    public SkillXpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("skillxp")
                        .executes(context -> {
                            info(context.getSource(), context.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("give").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("cantidad", LongArgumentType.longArg(1))
                                                .executes(context -> {
                                                    long amount = LongArgumentType.getLong(context, "cantidad");
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "jugador");
                                                    SkillTreeManager.addXp(target, amount);
                                                    done(context.getSource(), target, "+" + amount + " XP de habilidad");
                                                    return 1;
                                                }))))
                        .then(Commands.literal("take").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("cantidad", LongArgumentType.longArg(1))
                                                .executes(context -> {
                                                    long amount = LongArgumentType.getLong(context, "cantidad");
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "jugador");
                                                    SkillTreeManager.addXp(target, -amount);
                                                    done(context.getSource(), target, "-" + amount + " XP de habilidad");
                                                    return 1;
                                                }))))
                        .then(Commands.literal("set").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .then(Commands.argument("cantidad", LongArgumentType.longArg(0))
                                                .executes(context -> {
                                                    long amount = LongArgumentType.getLong(context, "cantidad");
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "jugador");
                                                    target.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                                                            .ifPresent(loadout -> loadout.setSkillXp(amount));
                                                    SkillTreeManager.sync(target);
                                                    done(context.getSource(), target, "XP de habilidad fijada en " + amount);
                                                    return 1;
                                                }))))
        );
    }

    private static void done(CommandSourceStack source, ServerPlayer target, String what) {
        target.displayClientMessage(Component.literal(what + ". Gastala en el árbol de habilidades (tecla H)."), false);
        source.sendSuccess(() -> Component.literal(target.getName().getString() + ": " + what), true);
    }

    private static void info(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY).ifPresent(loadout ->
                source.sendSuccess(() -> Component.literal("XP de habilidad: " + loadout.getSkillXp()
                        + " | próximo punto: " + SkillTreeManager.pointPrice(loadout.getPointsBought())
                        + " XP (ya compraste " + loadout.getPointsBought() + ")"), false));
    }
}
