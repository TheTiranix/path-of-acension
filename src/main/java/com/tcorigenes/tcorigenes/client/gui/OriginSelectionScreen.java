// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/** Base de las pantallas de eleccion de raza y de clase: fila de iconos clicable arriba, ficha con
 *  icono grande, nombre y frase, y la descripcion completa con scroll (rueda del mouse). */
abstract class OriginSelectionScreen<T> extends Screen {
    private static final int PANEL_W = 320;
    private static final int TEXT_W = 288;
    private static final int STRIP_ICON = 16;
    private static final int STRIP_SLOT = 24;
    private static final int LINE_H = 10;
    private static final int GAP = 3;

    private final List<T> options;
    private int index = 0;
    private int scroll = 0;
    private List<List<FormattedCharSequence>> blocks = List.of();
    private int contentHeight = 0;

    protected OriginSelectionScreen(Component title, List<T> options) {
        super(title);
        this.options = options;
    }

    protected abstract Component nameOf(T option);

    protected abstract ResourceLocation iconOf(T option);

    protected abstract String taglineOf(T option);

    protected abstract List<Component> linesOf(T option);

    protected abstract void choose(T option);

    // ---------------------------------------------------------------- layout
    private int areaTop() {
        return 90;
    }

    private int areaBottom() {
        return this.height - 36;
    }

    private int stripLeft() {
        return this.width / 2 - (this.options.size() * STRIP_SLOT) / 2;
    }

    private void select(int newIndex) {
        this.index = Math.floorMod(newIndex, this.options.size());
        this.scroll = 0;
        rebuild();
    }

    private void rebuild() {
        this.blocks = new ArrayList<>();
        this.contentHeight = 0;
        for (Component line : linesOf(this.options.get(this.index))) {
            List<FormattedCharSequence> wrapped = this.font.split(line, TEXT_W);
            this.blocks.add(wrapped);
            this.contentHeight += wrapped.size() * LINE_H + GAP;
        }
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - (areaBottom() - areaTop()));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int by = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> select(this.index - 1))
                .bounds(cx - 110, by, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> select(this.index + 1))
                .bounds(cx + 86, by, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Seleccionar"), b -> {
            choose(this.options.get(this.index));
            this.onClose();
        }).bounds(cx - 80, by, 160, 20).build());
        rebuild();
    }

    // ---------------------------------------------------------------- render
    private static final ResourceLocation BG = new ResourceLocation("tcorigenes", "textures/gui/origin_bg.png");
    private static final int BLOOD = 0xFF8B1A1A;
    private static final int BLOOD_DARK = 0xFF3A0A0A;

    /** Fondo del menu tetrico: la misma noche de sangre, oscurecida, con cenizas que suben. */
    private void renderAtmosphere(GuiGraphics g, int mouseX, int mouseY) {
        long t = Util.getMillis();
        float parallaxX = (mouseX - this.width / 2F) / this.width * -6F;
        float parallaxY = (mouseY - this.height / 2F) / this.height * -4F;
        g.pose().pushPose();
        g.pose().translate(parallaxX, parallaxY, 0);
        g.pose().translate(-6, -4, 0);
        g.pose().scale((this.width + 12) / (float) this.width, (this.height + 8) / (float) this.height, 1F);
        g.blit(BG, 0, 0, this.width, this.height, 0, 0, 960, 540, 960, 540);
        g.pose().popPose();
        // respiracion lenta de la oscuridad
        int breathe = 0x66 + (int) (0x1C * Math.sin(t / 1800.0));
        g.fill(0, 0, this.width, this.height, breathe << 24);
        g.fillGradient(0, 0, this.width, this.height / 3, 0xD0000000, 0x00000000);
        g.fillGradient(0, this.height * 2 / 3, this.width, this.height, 0x00000000, 0xE0000000);
        // cenizas / brasas que suben
        for (int i = 0; i < 44; i++) {
            double seed = Math.sin(i * 12.9898) * 43758.5453;
            float baseX = (float) (seed - Math.floor(seed)) * this.width;
            float speed = 7F + (i % 5) * 4F;
            float y = this.height - ((t / 1000F * speed + i * 37F) % (this.height + 20F));
            float sway = (float) Math.sin(t / 1500.0 + i) * 7F;
            int alpha = 40 + (int) (50 * (0.5 + 0.5 * Math.sin(t / 420.0 + i * 1.7)));
            int size = i % 7 == 0 ? 2 : 1;
            int x = (int) (baseX + sway);
            g.fill(x, (int) y, x + size, (int) y + size, (alpha << 24) | (i % 3 == 0 ? 0xE05030 : 0x9A8A8A));
        }
    }

    /** Marco del panel: borde de sangre seca, esquinas y goteras colgando del borde superior. */
    private void renderPanel(GuiGraphics g, int cx) {
        int x0 = cx - PANEL_W / 2;
        int x1 = cx + PANEL_W / 2;
        int y0 = 4;
        int y1 = this.height - 4;
        g.fill(x0, y0, x1, y1, 0xD0080405);
        g.renderOutline(x0, y0, PANEL_W, y1 - y0, BLOOD_DARK);
        g.renderOutline(x0 + 2, y0 + 2, PANEL_W - 4, y1 - y0 - 4, 0xFF1A0707);
        for (int[] c : new int[][] {{x0, y0, 1, 1}, {x1, y0, -1, 1}, {x0, y1, 1, -1}, {x1, y1, -1, -1}}) {
            g.fill(Math.min(c[0], c[0] + c[2] * 14), c[3] > 0 ? c[1] : c[1] - 1,
                    Math.max(c[0], c[0] + c[2] * 14), c[3] > 0 ? c[1] + 1 : c[1], BLOOD);
            g.fill(c[2] > 0 ? c[0] : c[0] - 1, Math.min(c[1], c[1] + c[3] * 14),
                    c[2] > 0 ? c[0] + 1 : c[0], Math.max(c[1], c[1] + c[3] * 14), BLOOD);
        }
        for (int i = 0; i < 26; i++) {
            double seed = Math.sin(i * 78.233) * 12345.678;
            double f = seed - Math.floor(seed);
            int x = x0 + 6 + (int) (f * (PANEL_W - 12));
            int len = 3 + (int) (((seed * 7) - Math.floor(seed * 7)) * 16);
            g.fill(x, y0, x + 2, y0 + len, 0xFF4A0808);
            g.fill(x - 1, y0 + len, x + 3, y0 + len + 2, 0xFF4A0808);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderAtmosphere(g, mouseX, mouseY);
        int cx = this.width / 2;
        renderPanel(g, cx);

        g.pose().pushPose();
        g.pose().translate(cx, 10, 0);
        g.pose().scale(1.4F, 1.4F, 1F);
        g.drawCenteredString(this.font, this.title, 0, 0, 0xC0392B);
        g.pose().popPose();

        // fila de iconos
        int sx = stripLeft();
        int sy = 26;
        long t = Util.getMillis();
        T hovered = null;
        for (int i = 0; i < this.options.size(); i++) {
            int x = sx + i * STRIP_SLOT;
            boolean over = mouseX >= x && mouseX < x + STRIP_SLOT && mouseY >= sy - 2 && mouseY < sy + STRIP_ICON + 4;
            if (i == this.index) {
                int flicker = 0x88 + (int) (0x30 * Math.sin(t / 170.0 + i) * Math.sin(t / 530.0));
                g.fill(x, sy - 3, x + STRIP_SLOT - 1, sy + STRIP_ICON + 3, (flicker << 24) | 0x701010);
                g.renderOutline(x, sy - 3, STRIP_SLOT - 1, STRIP_ICON + 6, BLOOD);
            } else if (over) {
                g.fill(x, sy - 3, x + STRIP_SLOT - 1, sy + STRIP_ICON + 3, 0x44A02020);
            }
            float dim = i == this.index || over ? 1.0F : 0.55F;
            RenderSystem.setShaderColor(dim, dim * 0.92F, dim * 0.92F, 1.0F);
            g.blit(iconOf(this.options.get(i)), x + (STRIP_SLOT - STRIP_ICON) / 2, sy, STRIP_ICON, STRIP_ICON, 0, 0, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            if (over) {
                hovered = this.options.get(i);
            }
        }

        // ficha
        T current = this.options.get(this.index);
        int left = cx - PANEL_W / 2 + 14;
        g.fill(left - 3, 47, left + 35, 85, 0x88000000);
        g.renderOutline(left - 3, 47, 38, 38, BLOOD_DARK);
        g.blit(iconOf(current), left, 50, 32, 32, 0, 0, 32, 32, 32, 32);
        g.pose().pushPose();
        g.pose().translate(left + 42, 52, 0);
        g.pose().scale(1.6F, 1.6F, 1F);
        g.drawString(this.font, nameOf(current), 1, 1, 0xFF2A0606, false);
        g.drawString(this.font, nameOf(current), 0, 0, 0xE8D8D0, false);
        g.pose().popPose();
        g.drawString(this.font, Component.literal(taglineOf(current)).withStyle(Style.EMPTY.withItalic(true)), left + 42, 71, 0x9A6A6A, false);
        g.fill(left, areaTop() - 6, cx + PANEL_W / 2 - 14, areaTop() - 5, BLOOD_DARK);
        g.fill(left + 30, areaTop() - 6, cx + PANEL_W / 2 - 44, areaTop() - 5, BLOOD);

        // descripcion con scroll
        int top = areaTop();
        int bottom = areaBottom();
        g.enableScissor(left, top, cx + PANEL_W / 2 - 10, bottom);
        int y = top - this.scroll;
        for (List<FormattedCharSequence> block : this.blocks) {
            for (FormattedCharSequence line : block) {
                if (y + LINE_H > top && y < bottom) {
                    g.drawString(this.font, line, left, y, 0xDDD0C8, false);
                }
                y += LINE_H;
            }
            y += GAP;
        }
        g.disableScissor();

        int max = maxScroll();
        if (max > 0) {
            int trackH = bottom - top;
            int barH = Math.max(14, trackH * trackH / this.contentHeight);
            int barY = top + (trackH - barH) * this.scroll / max;
            int bx = cx + PANEL_W / 2 - 8;
            g.fill(bx, top, bx + 3, bottom, 0x44301010);
            g.fill(bx, barY, bx + 3, barY + barH, BLOOD);
        }

        super.render(g, mouseX, mouseY, partialTick);
        if (hovered != null) {
            g.renderTooltip(this.font, nameOf(hovered), mouseX, mouseY);
        }
    }

    // ----------------------------------------------------------------- input
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int sx = stripLeft();
        if (button == 0 && mouseY >= 21 && mouseY < 24 + STRIP_ICON + 4) {
            int slot = (int) Math.floor((mouseX - sx) / STRIP_SLOT);
            if (mouseX >= sx && slot >= 0 && slot < this.options.size()) {
                select(slot);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - (int) Math.signum(delta) * 14));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            select(this.index - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            select(this.index + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
            this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll + (keyCode == GLFW.GLFW_KEY_DOWN ? 14 : -14)));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
