package com.tcorigenes.tcorigenes.core.capability.event;

import com.tudominio.elementaldamage.ModAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Aplica el atributo DRAW_SPEED a arcos y ballestas. Mientras se carga, la duracion restante del
 * uso baja de a 1 por tick; con +X de velocidad se le resta ademas un tick extra cada 1/X ticks
 * (carga mas rapido), y con velocidad negativa se le devuelve uno (carga mas lento).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class DrawSpeedHandler {
    private static final Map<UUID, Float> ACCUMULATED = new HashMap<>();

    private DrawSpeedHandler() {
    }

    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        var item = event.getItem().getItem();
        if (!(item instanceof BowItem) && !(item instanceof CrossbowItem)) {
            return;
        }
        var attribute = player.getAttribute(ModAttributes.DRAW_SPEED.get());
        if (attribute == null || attribute.getValue() == 0.0) {
            return;
        }
        float acc = ACCUMULATED.getOrDefault(player.getUUID(), 0.0F) + (float) attribute.getValue();
        int duration = event.getDuration();
        while (acc >= 1.0F && duration > 1) {
            duration--;
            acc -= 1.0F;
        }
        while (acc <= -1.0F) {
            duration++;
            acc += 1.0F;
        }
        ACCUMULATED.put(player.getUUID(), acc);
        event.setDuration(duration);
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        ACCUMULATED.remove(event.getEntity().getUUID());
    }
}
