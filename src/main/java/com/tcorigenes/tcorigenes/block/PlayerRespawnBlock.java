// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.block;

import com.tcorigenes.tcorigenes.checkpoint.Checkpoint;
import com.tcorigenes.tcorigenes.checkpoint.CheckpointManager;
import com.tcorigenes.tcorigenes.checkpoint.WaitingManager;
import java.util.Comparator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Punto de reanimacion: click derecho revive al primer jugador que este esperando (ver WaitingManager) en el
 * punto de guardado activo mas cercano a ESTE bloque. Solo se puede colocar a 30 bloques o menos de un punto
 * de guardado activo (ver CheckpointManager, que rechaza la colocacion si no hay ninguno cerca).
 */
public class PlayerRespawnBlock extends Block {
    public static final double MAX_RANGE_TO_CHECKPOINT = 30.0;

    public PlayerRespawnBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer reviver)) {
            return InteractionResult.SUCCESS;
        }
        var server = reviver.getServer();
        if (server == null) {
            return InteractionResult.FAIL;
        }
        var awaiting = server.getPlayerList().getPlayers().stream()
                .filter(sp -> sp != reviver && WaitingManager.isAwaiting(sp))
                .min(Comparator.comparingDouble(sp -> sp.distanceToSqr(reviver)));
        if (awaiting.isEmpty()) {
            reviver.displayClientMessage(Component.literal("No hay nadie esperando ser revivido.")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        Checkpoint nearest = CheckpointManager.active(server).stream()
                .filter(c -> c.dimension.equals(reviver.level().dimension()))
                .min(Comparator.comparingDouble(c -> c.pos.distSqr(pos)))
                .orElse(null);
        WaitingManager.revive(reviver, awaiting.get().getUUID(), nearest);
        return InteractionResult.CONSUME;
    }
}
