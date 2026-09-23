// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import java.lang.reflect.Field;
import java.util.Collection;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * El mod Burnt guarda tareas pendientes (los ticks diferidos de sus geiseres) en una cola ESTATICA
 * (BurntMod.workQueue) que nunca vacia al cerrar un mundo. Como aca el juego sigue abierto al salir y
 * volver a entrar a un mundo (y con la restauracion de saves), al entrar corria tareas del mundo anterior
 * que pedian un chunk desde el hilo del servidor y lo dejaban colgado ~10 minutos ("Server thread
 * WAITING" en GeyserStartTickSmallProcedure). Se vacia al cerrar y al abrir cada mundo.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class BurntWorkQueueFix {
    private BurntWorkQueueFix() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        clear();
    }

    private static void clear() {
        if (!ModList.get().isLoaded("burnt")) {
            return;
        }
        try {
            Field field = Class.forName("net.pixelbank.burnt.BurntMod").getDeclaredField("workQueue");
            field.setAccessible(true);
            Object queue = field.get(null);
            if (queue instanceof Collection<?> collection) {
                collection.clear();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }
}
