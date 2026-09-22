// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.compat.TanCompat;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Calor extremo: duplica el consumo de agua (la sed base ya es lenta, ver exhaustion_threshold en el
 * config de Tough As Nails). Antes multiplicaba x3 sobre una base MUY lenta (threshold 70): el desierto
 * (casi siempre "HOT" en Tough As Nails) se sentia vaciar rapidisimo mientras que en biomas normales la
 * sed no bajaba nunca de forma perceptible. Bajamos el threshold base (mas presente en todos lados) y
 * este extra a x2, asi el desierto sigue siendo claramente mas duro sin ser una vaciada instantanea.
 * Ender Warrior no tiene sed (su regla de hambre x2 esta en ModEvents) y el Demonio ignora por completo
 * este estado.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ExtremeHeatRules {
    private static final float EXTRA_FACTOR = 1.0F;
    private static final Map<UUID, Float> LAST_EXHAUSTION = new HashMap<>();

    private ExtremeHeatRules() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !TanCompat.isLoaded()) {
            return;
        }
        Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
        float current = TanCompat.thirstExhaustion(player);
        float last = LAST_EXHAUSTION.getOrDefault(player.getUUID(), current);
        if (race != Race.ENDER_WARRIOR && race != Race.DEMONIO && current > last && TanCompat.isExtremeHeat(player)) {
            TanCompat.addThirstExhaustion(player, (current - last) * EXTRA_FACTOR);
            current = TanCompat.thirstExhaustion(player);
        }
        LAST_EXHAUSTION.put(player.getUUID(), current);
    }
}
