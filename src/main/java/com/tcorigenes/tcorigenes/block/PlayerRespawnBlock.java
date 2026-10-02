// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.block;

import com.tcorigenes.tcorigenes.checkpoint.PlayerReviveBridge;
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
 * Punto de reanimacion: click derecho revive de una a TODOS los jugadores caidos del servidor (los que
 * PlayerRevive tiene tirados esperando, ver PlayerReviveBridge) alli donde cayeron, no en el bloque. Solo
 * se puede colocar a 30 bloques o menos de un punto de guardado activo (ver CheckpointManager, que rechaza
 * la colocacion si no hay ninguno cerca).
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
        if (!PlayerReviveBridge.isLoaded()) {
            reviver.displayClientMessage(Component.translatable("pa.msg.bac494f885")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        int revived = PlayerReviveBridge.reviveAllDown(reviver.getServer());
        reviver.displayClientMessage((revived > 0 ? Component.translatable("pa.m.revived", revived) : Component.translatable("pa.m.nobody_down"))
                .withStyle(revived > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        return InteractionResult.CONSUME;
    }
}
