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

/** Click derecho con una ofrenda valida (ver AltarOfferings) la consume y da favor de ese dios.
 *  Click derecho con la mano vacia y agachado en el altar de Pater es "rezar": da un poco de
 *  favor sin consumir nada, limitado por un cooldown para que no se pueda spamear. */
public class AltarBlock extends Block {
    private static final int PRAYER_FAVOR = 2;
    private static final int PRAYER_COOLDOWN_TICKS = 20 * 60 * 5;
    private static final java.util.Map<java.util.UUID, Long> LAST_PRAYER = new java.util.HashMap<>();

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
        if (stack.isEmpty() && player.isCrouching()) {
            return handlePrayer(serverPlayer, level, pos);
        }

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

    private InteractionResult handlePrayer(ServerPlayer player, Level level, BlockPos pos) {
        if (deity != Deity.PATER) {
            player.displayClientMessage(Component.literal("Solo se reza en el altar de Pater."), true);
            return InteractionResult.FAIL;
        }
        long now = level.getGameTime();
        Long last = LAST_PRAYER.get(player.getUUID());
        if (last != null && now - last < PRAYER_COOLDOWN_TICKS) {
            player.displayClientMessage(Component.literal("Pater ya escuchó tu plegaria por ahora."), true);
            return InteractionResult.FAIL;
        }
        LAST_PRAYER.put(player.getUUID(), now);
        FavorManager.addFavor(player, Deity.PATER, PRAYER_FAVOR);
        player.displayClientMessage(Component.literal(
                "Pater escucha tu plegaria. (+" + PRAYER_FAVOR + " favor)"), true);
        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.01);
        return InteractionResult.CONSUME;
    }
}
