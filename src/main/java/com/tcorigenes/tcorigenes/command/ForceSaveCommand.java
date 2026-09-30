// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tcorigenes.tcorigenes.checkpoint.CheckpointManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** /forcesave: abre la interfaz de puntos de guardado sin dormir en una cama y guarda donde estas parado, salteando
 *  todas las restricciones (cama, 1 dia entre saves, 1 dia de espera en camas nuevas). Solo operadores. */
public class ForceSaveCommand {
    public ForceSaveCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("forcesave").requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CheckpointManager.openForcedSaveScreen(context.getSource().getPlayerOrException());
                    return 1;
                }));
    }
}
