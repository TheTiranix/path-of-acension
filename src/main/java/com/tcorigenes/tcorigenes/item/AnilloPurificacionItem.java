package com.tcorigenes.tcorigenes.item;

import com.tcorigenes.tcorigenes.attributes.RaceAttributeManager;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Al usarlo un Malnacido, contrarresta su maldicion para siempre (guardado en persistentData,
 * ver RaceAttributeManager): se van los debuffs de la raza, +15% daño y Regeneracion II
 * permanente (esta ultima se re-aplica en cada login/respawn, ver ModEvents).
 */
public class AnilloPurificacionItem extends Item {
    public AnilloPurificacionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        return player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(raceInfo -> {
            if (raceInfo.getRace() != Race.MALNACIDO) {
                player.displayClientMessage(Component.literal("Este anillo solo responde a los malditos."), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            if (player.getPersistentData().getBoolean("malnacido_purificado")) {
                player.displayClientMessage(Component.literal("Tu maldición ya fue contrarrestada."), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            player.getPersistentData().putBoolean("malnacido_purificado", true);
            RaceAttributeManager.updateAttributes(player, Race.MALNACIDO);
            RaceAttributeManager.applyPurifiedEffects(player);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                com.tcorigenes.tcorigenes.core.capability.RaceSync.broadcast(serverPlayer, Race.MALNACIDO);
            }
            player.displayClientMessage(Component.literal("Tu maldición se disipa. Sos libre."), false);
            ItemStack stack = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }).orElse(InteractionResultHolder.pass(player.getItemInHand(hand)));
    }
}
