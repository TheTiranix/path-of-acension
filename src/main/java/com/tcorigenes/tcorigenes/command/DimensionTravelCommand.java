// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/** /heaven y /hell: viajan a las dimensiones Heaven (piso de vidrio en el cielo, nubes alrededor) y Hell (planicie de netherrack). */
public final class DimensionTravelCommand {
    public static final ResourceKey<Level> HEAVEN = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("tcorigenes", "heaven"));
    public static final ResourceKey<Level> HELL = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("tcorigenes", "hell"));

    private DimensionTravelCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // el piso de vidrio de Heaven esta en y=190 (se para en 191, con los ojos dentro de la capa de nubes); el de Hell en y=63
        dispatcher.register(Commands.literal("heaven").requires(s -> s.hasPermission(2)).executes(c -> go(c.getSource(), HEAVEN, 191.0)));
        dispatcher.register(Commands.literal("hell").requires(s -> s.hasPermission(2)).executes(c -> go(c.getSource(), HELL, 63.0)));
    }

    private static int go(CommandSourceStack source, ResourceKey<Level> dimension, double y) {
        ServerPlayer player = source.getPlayer();
        ServerLevel target = source.getServer().getLevel(dimension);
        if (player == null || target == null) {
            source.sendFailure(Component.literal("Esa dimension no esta disponible (falta el datapack de la mod en el mundo)."));
            return 0;
        }
        // si ya esta en esa dimension vuelve a su punto de aparicion; si no, va al centro
        player.teleportTo(target, 0.5, y, 0.5, player.getYRot(), player.getXRot());
        return 1;
    }
}
