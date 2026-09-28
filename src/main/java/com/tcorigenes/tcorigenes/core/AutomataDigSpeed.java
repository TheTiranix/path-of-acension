// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** El Autómata mina, tala y cava un 15% mas rapido (no hay atributo de velocidad de mineria en 1.20.1: se
 *  multiplica directo la velocidad calculada, junto con la herramienta/Prisa/etc). */
@EventBusSubscriber(modid = "tcorigenes")
public final class AutomataDigSpeed {
    private static final float MULTIPLIER = 1.15F;

    private AutomataDigSpeed() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Race race = event.getEntity().getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace()).orElse(Race.HUMANO);
        if (race == Race.AUTOMATA) {
            event.setNewSpeed(event.getNewSpeed() * MULTIPLIER);
        }
    }
}
