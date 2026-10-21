// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Botones del menu principal: caja transparente con borde rojo y letra Cinzel (assets/tcorigenes/font/menu.json, licencia OFL en esa carpeta).
 * Los botones de vanilla siguen siendo los que reciben los clics; se dejan invisibles (alfa 0) y encima se dibuja el estilo nuevo. Solo se tocan
 * los botones anchos (los 5 de la columna); los iconos de otros mods no. Si algo falla, se apaga solo y vuelven los botones normales.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class MenuButtons {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuButtons.class);
    private static final Style FONT = Style.EMPTY.withFont(new ResourceLocation("tcorigenes", "menu"));
    private static final int MIN_WIDTH = 100;
    private static boolean failed;

    private MenuButtons() {
    }

    private static List<Button> wide(Screen screen) {
        List<Button> out = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button b && b.visible && b.getWidth() >= MIN_WIDTH) {
                out.add(b);
            }
        }
        return out;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideVanilla(ScreenEvent.Render.Pre event) {
        if (failed || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        try {
            for (Button b : wide(event.getScreen())) {
                b.setAlpha(0.0F);
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drawStyled(ScreenEvent.Render.Post event) {
        if (failed || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        try {
            GuiGraphics g = event.getGuiGraphics();
            Minecraft mc = Minecraft.getInstance();
            for (Button b : wide(event.getScreen())) {
                boolean hot = b.active && b.isHoveredOrFocused();
                int x = b.getX();
                int y = b.getY();
                int w = b.getWidth();
                int h = b.getHeight();
                if (hot) {
                    g.fill(x, y, x + w, y + h, 0x50801018); // fondo rojo apagado solo al pasar el mouse
                }
                g.renderOutline(x, y, w, h, !b.active ? 0xFF4A2024 : hot ? 0xFFE2242C : 0xFFA01018);
                Component label = b.getMessage().copy().withStyle(FONT);
                int color = !b.active ? 0xFF8A7878 : hot ? 0xFFFFFFFF : 0xFFE0CECE;
                g.drawCenteredString(mc.font, label, x + w / 2, y + (h - 8) / 2, color);
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static void fail(Throwable t) {
        failed = true;
        LOGGER.error("[tcorigenes] estilo de botones del menu desactivado por un error", t);
    }
}
