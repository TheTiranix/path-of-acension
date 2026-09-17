package com.tcorigenes.tcorigenes.block;

import com.tcorigenes.tcorigenes.favor.AltarOfferings;
import com.tcorigenes.tcorigenes.favor.Deity;
import com.tcorigenes.tcorigenes.favor.FavorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Click derecho con una ofrenda valida (ver AltarOfferings) la consume y da favor de ese dios. */
public class AltarBlock extends Block {
    private final Deity deity;

    public AltarBlock(Deity deity, Properties properties) {
        super(properties);
        this.deity = deity;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        Integer favorAmount = AltarOfferings.getFavorValue(deity, stack.getItem());
        if (favorAmount == null) {
            player.displayClientMessage(Component.literal("El altar de " + deity.getDisplayName() + " no acepta esta ofrenda."), true);
            return InteractionResult.FAIL;
        }

        stack.shrink(1);
        FavorManager.addFavor(serverPlayer, deity, favorAmount);
        int newValue = FavorManager.getFavor(serverPlayer, deity);
        player.displayClientMessage(Component.literal(
                deity.getDisplayName() + " acepta tu ofrenda. (+" + favorAmount + ", ahora " + newValue + ")"), true);

        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        return InteractionResult.CONSUME;
    }
}
