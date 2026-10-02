// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tcorigenes.tcorigenes.intro.IntroManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** /intro: vuelve a lanzar la introduccion (cinematica y preguntas) para probarla. Reasigna raza y clase. Solo operadores. */
public class IntroCommand {
    public IntroCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("intro").requires(source -> source.hasPermission(2))
                .executes(context -> {
                    IntroManager.start(context.getSource().getPlayerOrException());
                    return 1;
                }));
    }
}
