// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diagnostico de la pantalla de Curios (reportada "vacia" por el usuario): al abrirse y en sus primeros cuadros escribe en el log donde esta
 * y de que tamaño es, que ranuras tiene y si dibuja algo. Solo informa, no cambia nada.
 */
public final class CuriosDebug {
    private static final Logger LOGGER = LoggerFactory.getLogger("pa-curios-debug");
    private static int frames;

    private CuriosDebug() {
    }

    private static boolean isCurios(Screen screen) {
        return screen != null && screen.getClass().getName().startsWith("top.theillusivec4.curios.client.gui");
    }

    private static String describe(AbstractContainerScreen<?> screen) {
        StringBuilder sb = new StringBuilder();
        sb.append(screen.getClass().getSimpleName()).append(" ventana=").append(screen.width).append("x").append(screen.height)
                .append(" gui=").append(screen.getGuiLeft()).append(",").append(screen.getGuiTop()).append(" ")
                .append(screen.getXSize()).append("x").append(screen.getYSize())
                .append(" escala=").append(Minecraft.getInstance().getWindow().getGuiScale())
                .append(" ranuras=").append(screen.getMenu().slots.size());
        try {
            Object menu = screen.getMenu();
            for (String field : new String[] {"panelWidth", "currentPage", "totalPages", "hasCosmetics", "isViewingCosmetics"}) {
                try {
                    sb.append(" ").append(field).append("=").append(menu.getClass().getField(field).get(menu));
                } catch (ReflectiveOperationException e) {
                    // el campo cambia entre versiones
                }
            }
            Object handler = menu.getClass().getField("curiosHandler").get(menu);
            Object curios = handler.getClass().getMethod("getCurios").invoke(handler);
            sb.append(" tiposDeRanura=").append(((java.util.Map<?, ?>) curios).keySet());
        } catch (ReflectiveOperationException | ClassCastException e) {
            sb.append(" (sin datos de Curios: ").append(e.toString()).append(")");
        }
        return sb.toString();
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (isCurios(event.getScreen()) && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            frames = 0;
            LOGGER.info("Se abrio: {}", describe(screen));
        }
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (frames < 3 && isCurios(event.getScreen()) && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            frames++;
            LOGGER.info("Cuadro {}: {} botones={}", frames, describe(screen), screen.children().size());
        }
    }
}
