// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * En un mundo de un solo jugador la generacion de terreno corre en la misma PC que el dibujado y le quita procesador (los FPS caen al explorar
 * terreno nuevo). Este hilo, cada pocos segundos, baja a la prioridad minima los hilos de trabajo en segundo plano (los de generacion de
 * Minecraft y los de planificacion de rios de Streams Reflowing) para que el sistema le de siempre preferencia al hilo de dibujado. No modifica
 * ningun mod ajeno: solo cambia la prioridad de planificacion de hilos. La generacion tarda un poco mas, pero sin tirones.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class BackgroundThreadPriority {
    private static final String[] PREFIXES = {"Worker-Main", "streamsreflowing-sampler", "streamsreflowing-prefetch"};
    private static boolean started;

    private BackgroundThreadPriority() {
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (started) {
            return;
        }
        started = true;
        Thread thread = new Thread(BackgroundThreadPriority::loop, "tcorigenes-thread-priority");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static void loop() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        while (true) {
            try {
                Thread[] threads = new Thread[root.activeCount() + 32];
                int count = root.enumerate(threads, true);
                for (int i = 0; i < count; i++) {
                    Thread t = threads[i];
                    if (t != null && t.getPriority() > Thread.MIN_PRIORITY && matches(t.getName())) {
                        t.setPriority(Thread.MIN_PRIORITY);
                    }
                }
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                return;
            } catch (RuntimeException e) {
                // un hilo que desaparece mientras se lo recorre: se vuelve a intentar en la proxima vuelta
            }
        }
    }

    private static boolean matches(String name) {
        for (String prefix : PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
