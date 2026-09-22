// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tcorigenes.tcorigenes.checkpoint.Checkpoint;
import com.tcorigenes.tcorigenes.checkpoint.CheckpointManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /respawnpoint list: los puntos de guardado activos ahora mismo (compartidos por todo el mundo).
 * /respawnpoint choose <numero>: cual de esos preferís para tu proximo respawn/revivida (si deja de estar
 * activo, se usa el mas nuevo de todos en su lugar, ver CheckpointManager#pickFor).
 */
public class RespawnPointCommand {
    public RespawnPointCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("respawnpoint")
                        .then(Commands.literal("list").executes(context -> {
                            list(context.getSource());
                            return 1;
                        }))
                        .then(Commands.literal("choose")
                                .then(Commands.argument("numero", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            choose(context.getSource(), IntegerArgumentType.getInteger(context, "numero"));
                                            return 1;
                                        })))
        );
    }

    private static void list(CommandSourceStack source) {
        List<Checkpoint> active = CheckpointManager.active(source.getServer());
        if (active.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No hay ningún punto de guardado activo todavía: coloca una cama.")
                    .withStyle(ChatFormatting.GRAY), false);
            return;
        }
        source.sendSuccess(() -> Component.literal("Puntos de guardado activos:").withStyle(ChatFormatting.GOLD), false);
        for (int i = 0; i < active.size(); i++) {
            Checkpoint checkpoint = active.get(i);
            String owner = source.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> p.getUUID().equals(checkpoint.owner)).findFirst()
                    .map(p -> p.getName().getString()).orElse(checkpoint.owner.toString().substring(0, 8));
            int index = i + 1;
            source.sendSuccess(() -> Component.literal(index + ") " + checkpoint.pos.toShortString()
                    + " en " + checkpoint.dimension.location() + " (de " + owner + ")").withStyle(ChatFormatting.WHITE), false);
        }
    }

    private static void choose(CommandSourceStack source, int number) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<Checkpoint> active = CheckpointManager.active(source.getServer());
        if (number < 1 || number > active.size()) {
            source.sendFailure(Component.literal("No existe ese número: usa /respawnpoint list."));
            return;
        }
        Checkpoint checkpoint = active.get(number - 1);
        CheckpointManager.setPreferred(player, checkpoint.id);
        source.sendSuccess(() -> Component.literal("Tu punto de guardado preferido ahora es el " + number + ".")
                .withStyle(ChatFormatting.GREEN), false);
    }
}
