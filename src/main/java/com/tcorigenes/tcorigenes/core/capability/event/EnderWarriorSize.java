package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.client.ClientRaceData;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * El Ender Warrior es 0.5 bloques mas alto de verdad (hitbox y altura de ojos), no solo el modelo.
 * En el servidor la raza sale de la capability; en el cliente, de la copia sincronizada
 * (ClientRaceData). Hay que llamar a refreshDimensions() cuando la raza cambia para que se aplique.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class EnderWarriorSize {
    private static final float EXTRA_HEIGHT = 0.5F;

    private EnderWarriorSize() {
    }

    private static boolean isEnderWarrior(Player player) {
        if (player.level().isClientSide()) {
            return ClientRaceData.get(player.getUUID()) == Race.ENDER_WARRIOR;
        }
        return player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace() == Race.ENDER_WARRIOR).orElse(false);
    }

    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player) || !isEnderWarrior(player)) {
            return;
        }
        float extra = 0.0F;
        if (event.getPose() == Pose.STANDING) {
            extra = EXTRA_HEIGHT;
        } else if (event.getPose() == Pose.CROUCHING) {
            extra = EXTRA_HEIGHT * 0.8F;
        }
        if (extra <= 0.0F) {
            return;
        }
        EntityDimensions old = event.getNewSize();
        event.setNewSize(EntityDimensions.scalable(old.width, old.height + extra));
        event.setNewEyeHeight(event.getNewEyeHeight() + extra * 0.9F);
    }
}
