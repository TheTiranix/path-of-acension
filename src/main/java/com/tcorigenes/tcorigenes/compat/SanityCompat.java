// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Puente con "Sanity: Descent Into Madness" (cordura estilo Don't Starve), por reflexion para no depender del mod al compilar.
 * La cordura va de 0 a 1; addSanity aplica los multiplicadores de la config del mod (positivo y negativo).
 */
public final class SanityCompat {
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean loggedFailure;
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
                    LOGGER.info("[Sanity] puente con sanitydim listo");
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    capability = null;
                    addSanity = null;
                    LOGGER.error("[Sanity] no se pudo enlazar con sanitydim, la cordura del Siervo de la Luna no funcionara", e);
                }
            }
        }
        return capability != null && addSanity != null;
    }

    private static void logOnce(String message, Throwable error) {
        if (!loggedFailure) {
            loggedFailure = true;
            if (error != null) {
                LOGGER.error(message, error);
            } else {
                LOGGER.error(message);
            }
        }
    }

    /** Suma (o resta, si es negativo) cordura: 0.01 = 1% de la barra. */
    public static void add(ServerPlayer player, float fraction) {
        if (!init()) {
            return;
        }
        boolean[] found = {false};
        player.getCapability(capability).ifPresent(cap -> {
            found[0] = true;
            try {
                addSanity.invoke(null, cap, fraction, player);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                logOnce("[Sanity] addSanity fallo", e);
            }
        });
        if (!found[0]) {
            logOnce("[Sanity] el jugador no tiene la capability de cordura", null);
        }
    }
}
