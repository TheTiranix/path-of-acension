// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Botones del menu principal: caja transparente con borde rojo y la etiqueta en Cinzel (licencia OFL, assets/tcorigenes/font) dibujada como imagen
 * (tools/gen_menu_labels.py), asi no depende del sistema de fuentes del juego. Son botones propios: los cinco de vanilla se ocultan desde el layout de
 * FancyMenu (is_hidden) y cada boton nuevo ejecuta la accion del de vanilla (o la equivalente si no se lo encuentra). Idiomas con etiqueta: es y en;
 * en otros se usa la letra normal del juego. Si algo falla al crearlos, quedan botones comunes en las mismas posiciones (nunca un menu sin botones).
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class MenuButtons {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuButtons.class);
    private static final int X = 30;
    private static final int Y = 96;
    private static final int STEP = 26;
    private static final int W = 140;
    private static final int H = 20;
    private static final String[] IDS = {"sp", "mp", "opt", "mods", "quit"};
    private static final String[] KEYS = {"menu.singleplayer", "menu.multiplayer", "menu.options", "fml.menu.mods", "menu.quit"};

    private MenuButtons() {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) {
            return;
        }
        String lang = labelLanguage();
        for (int i = 0; i < IDS.length; i++) {
            Button vanilla = find(screen, KEYS[i]);
            Component fallback = vanilla != null ? vanilla.getMessage() : Component.translatable(KEYS[i]);
            int index = i;
            Button.OnPress press = b -> act(screen, index, vanilla);
            int y = Y + STEP * i;
            try {
                event.addListener(new MenuButton(X, y, W, H, fallback, label(IDS[i], lang), press));
            } catch (Throwable t) {
                LOGGER.error("[tcorigenes] boton del menu {} sin estilo por un error", IDS[i], t);
                event.addListener(Button.builder(fallback, press).bounds(X, y, W, H).build());
            }
        }
    }

    private static Button find(Screen screen, String key) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals(key)) {
                return b;
            }
        }
        return null;
    }

    private static void act(TitleScreen screen, int index, Button vanilla) {
        if (vanilla != null) {
            vanilla.onPress();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        switch (index) {
            case 0 -> mc.setScreen(new SelectWorldScreen(screen));
            case 1 -> mc.setScreen(mc.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(screen) : new SafetyScreen(screen));
            case 2 -> mc.setScreen(new OptionsScreen(screen, mc.options));
            case 3 -> mc.setScreen(new ModListScreen(screen));
            default -> mc.stop();
        }
    }

    /** Marco de 1 pixel real de pantalla (renderOutline dibuja 1 unidad de GUI = 3 pixeles con la escala de siempre). */
    private static void thinOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        double s = Minecraft.getInstance().getWindow().getGuiScale();
        int px = (int) Math.round(x * s);
        int py = (int) Math.round(y * s);
        int pw = (int) Math.round(w * s);
        int ph = (int) Math.round(h * s);
        g.pose().pushPose();
        g.pose().scale((float) (1.0 / s), (float) (1.0 / s), 1.0F);
        g.fill(px, py, px + pw, py + 1, color);
        g.fill(px, py + ph - 1, px + pw, py + ph, color);
        g.fill(px, py + 1, px + 1, py + ph - 1, color);
        g.fill(px + pw - 1, py + 1, px + pw, py + ph - 1, color);
        g.pose().popPose();
    }

    private static String labelLanguage() {
        String code = Minecraft.getInstance().getLanguageManager().getSelected();
        return code.startsWith("es") ? "es" : code.startsWith("en") ? "en" : null;
    }

    /** Imagen de la etiqueta, o null si no hay para ese idioma. */
    private static ResourceLocation label(String id, String lang) {
        if (lang == null) {
            return null;
        }
        ResourceLocation tex = new ResourceLocation("tcorigenes", "textures/gui/menu/btn_" + id + "_" + lang + ".png");
        return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent() ? tex : null;
    }

    private static final class MenuButton extends Button {
        private static final int LABEL_W = 1200;
        private static final int LABEL_H = 96;
        private final ResourceLocation label;

        MenuButton(int x, int y, int w, int h, Component message, ResourceLocation label, OnPress onPress) {
            super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
            this.label = label;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hot = active && isHoveredOrFocused();
            if (hot) {
                g.fill(getX(), getY(), getX() + width, getY() + height, 0x50801018); // fondo rojo apagado solo al pasar el mouse
            }
            thinOutline(g, getX(), getY(), width, height, !active ? 0xFF6A6A6A : hot ? 0xFFFFFFFF : 0xFFE6E6E6);
            float red = !active ? 0.54F : hot ? 1.0F : 0.88F;
            float green = !active ? 0.47F : hot ? 1.0F : 0.81F;
            float blue = green;
            if (label != null) {
                int drawH = 14;
                int drawW = drawH * LABEL_W / LABEL_H;
                g.setColor(red, green, blue, 1.0F);
                g.blit(label, getX() + (width - drawW) / 2, getY() + (height - drawH) / 2, drawW, drawH, 0.0F, 0.0F, LABEL_W, LABEL_H, LABEL_W, LABEL_H);
                g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                int color = 0xFF000000 | ((int) (red * 255) << 16) | ((int) (green * 255) << 8) | (int) (blue * 255);
                g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
            }
        }
    }
}
