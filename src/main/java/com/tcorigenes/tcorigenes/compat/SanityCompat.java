// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.ModList;

/**
 * Puente con "Sanity: Descent Into Madness" (cordura estilo Don't Starve), por reflexion para no depender del mod al compilar.
 * La cordura va de 0 a 1; addSanity aplica los multiplicadores de la config del mod (positivo y negativo).
 */
public final class SanityCompat {
    private static boolean initialized;
    private static Capability<?> capability;
    private static Method addSanity;

    private SanityCompat() {
    }

    private static boolean init() {
        if (!initialized) {
            initialized = true;
            if (ModList.get().isLoaded("sanitydim")) {
                try {
                    capability = (Capability<?>) Class.forName("croissantnova.sanitydim.capability.SanityProvider").getField("CAP").get(null);
                    Class<?> iface = Class.forName("croissantnova.sanitydim.capability.ISanity");
                    addSanity = Class.forName("croissantnova.sanitydim.SanityProcessor").getMethod("addSanity", iface, float.class, ServerPlayer.class);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    capability = null;
                    addSanity = null;
                }
            }
        }
        return capability != null && addSanity != null;
    }

    /** Suma (o resta, si es negativo) cordura: 0.01 = 1% de la barra. */
    public static void add(ServerPlayer player, float fraction) {
        if (!init()) {
            return;
        }
        player.getCapability(capability).ifPresent(cap -> {
            try {
                addSanity.invoke(null, cap, fraction, player);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // la API del mod cambio: se deja de intentar sin romper nada
            }
        });
    }
}
