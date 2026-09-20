// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Solo-cliente: cuenta regresiva en tiempo real (reloj de pared), resincronizada por el
 *  servidor en cada respuesta. No depende de la TPS del server ni del framerate del cliente. */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public class ClientAbilityCooldowns {
    private static final Map<String, Long> READY_AT_CLIENT_MILLIS = new HashMap<>();
    private static String lastAbilityId = null;

    public static void onSync(String abilityId, int remainingOrTotalMillis) {
        lastAbilityId = abilityId;
        READY_AT_CLIENT_MILLIS.put(abilityId, System.currentTimeMillis() + remainingOrTotalMillis);
    }

    public static int getRemainingMillis(String abilityId) {
        Long readyAt = READY_AT_CLIENT_MILLIS.get(abilityId);
        if (readyAt == null) {
            return 0;
        }
        return (int) Math.max(0, readyAt - System.currentTimeMillis());
    }

    public static String getLastAbilityId() {
        return lastAbilityId;
    }
}
