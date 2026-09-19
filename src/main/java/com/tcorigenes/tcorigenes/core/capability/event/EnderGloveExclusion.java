package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.compat.CuriosCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import top.theillusivec4.curios.api.event.CurioEquipEvent;

/**
 * Ender Warrior: el par de guantes normales (slot "hands", izquierda y derecha) y los guantes del
 * Aether (slot "aether_gloves") son alternativas para el combo de nado, no se acumulan: no se
 * puede equipar uno mientras se lleva puesto el otro.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class EnderGloveExclusion {
    private static final String HANDS = "hands";
    private static final String AETHER_GLOVES = "aether_gloves";

    private EnderGloveExclusion() {
    }

    @SubscribeEvent
    public static void onEquip(CurioEquipEvent event) {
        if (!(event.getSlotContext().entity() instanceof Player player) || !EnderWarriorSize.isEnderWarrior(player)) {
            return;
        }
        String slot = event.getSlotContext().identifier();
        String message = null;
        if (slot.equals(HANDS) && CuriosCompat.hasAnyInSlot(player, AETHER_GLOVES)) {
            message = "Ya llevás los guantes del Aether: no podés equipar otro par de guantes.";
        } else if (slot.equals(AETHER_GLOVES) && CuriosCompat.hasAnyInSlot(player, HANDS)) {
            message = "Ya llevás un par de guantes: sacalos antes de equipar los del Aether.";
        }
        if (message != null) {
            event.setResult(Event.Result.DENY);
            if (!player.level().isClientSide()) {
                player.displayClientMessage(Component.literal(message), true);
            }
        }
    }
}
