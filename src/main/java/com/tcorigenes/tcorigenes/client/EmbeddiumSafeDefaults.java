// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Embeddium trae por defecto "formato de vertice compacto" y "buffers de staging avanzados", que en algunas graficas (sobre todo AMD) y con el
 * mod de pasto 3D (Grazzier Grass) dejan las texturas rotas. Si el archivo de opciones se recrea con los valores por defecto, esto los vuelve a
 * apagar: se aplica al iniciar el cliente y otra vez cada vez que se entra a un mundo (antes de que Embeddium arme el renderizador de chunks). Por
 * reflexion, para no depender de Embeddium al compilar.
 */
public final class EmbeddiumSafeDefaults {
    private static final Logger LOGGER = LogManager.getLogger();

    private EmbeddiumSafeDefaults() {
    }

    @EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(EmbeddiumSafeDefaults::apply);
        }
    }

    @EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            apply();
        }
    }

    private static void apply() {
        if (!ModList.get().isLoaded("embeddium")) {
            return;
        }
        try {
            Object options = Class.forName("me.jellysquid.mods.sodium.client.SodiumClientMod").getMethod("options").invoke(null);
            Object advanced = options.getClass().getField("advanced").get(options);
            Object performance = options.getClass().getField("performance").get(options);
            boolean changed = force(advanced, "useAdvancedStagingBuffers");
            changed |= force(performance, "useCompactVertexFormat");
            changed |= force(performance, "useNoErrorGLContext");
            if (changed) {
                Method write = options.getClass().getMethod("writeChanges");
                write.invoke(options);
                LOGGER.info("[Embeddium] se apagaron los ajustes que rompen texturas (vertice compacto, staging avanzado, contexto sin errores)");
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.warn("[Embeddium] no se pudieron fijar las opciones seguras", e);
        }
    }

    /** Pone el campo booleano en false; devuelve true si estaba encendido. */
    private static boolean force(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getField(name);
        if (field.getBoolean(target)) {
            field.setBoolean(target, false);
            return true;
        }
        return false;
    }
}
