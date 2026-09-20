// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;
        g.fill(cx - PANEL_W / 2, 4, cx + PANEL_W / 2, this.height - 4, 0xC0101018);
        g.renderOutline(cx - PANEL_W / 2, 4, PANEL_W, this.height - 8, 0xFF5A4A7A);

        g.drawCenteredString(this.font, this.title, cx, 10, 0xFFD700);

        // fila de iconos
        int sx = stripLeft();
        int sy = 24;
        T hovered = null;
        for (int i = 0; i < this.options.size(); i++) {
            int x = sx + i * STRIP_SLOT;
            boolean over = mouseX >= x && mouseX < x + STRIP_SLOT && mouseY >= sy - 2 && mouseY < sy + STRIP_ICON + 4;
            if (i == this.index) {
                g.fill(x, sy - 3, x + STRIP_SLOT - 1, sy + STRIP_ICON + 3, 0xAA8A6ACA);
            } else if (over) {
                g.fill(x, sy - 3, x + STRIP_SLOT - 1, sy + STRIP_ICON + 3, 0x55FFFFFF);
            }
            g.blit(iconOf(this.options.get(i)), x + (STRIP_SLOT - STRIP_ICON) / 2, sy, STRIP_ICON, STRIP_ICON, 0, 0, 32, 32, 32, 32);
            if (over) {
                hovered = this.options.get(i);
            }
        }

        // ficha
        T current = this.options.get(this.index);
        int left = cx - PANEL_W / 2 + 14;
        g.blit(iconOf(current), left, 46, 32, 32, 0, 0, 32, 32, 32, 32);
        g.pose().pushPose();
        g.pose().translate(left + 40, 49, 0);
        g.pose().scale(1.5F, 1.5F, 1F);
        g.drawString(this.font, nameOf(current), 0, 0, 0xFFFFFF, true);
        g.pose().popPose();
        g.drawString(this.font, taglineOf(current), left + 40, 66, 0xAAAAAA, false);
        g.fill(left, areaTop() - 6, cx + PANEL_W / 2 - 14, areaTop() - 5, 0xFF5A4A7A);

        // descripcion con scroll
        int top = areaTop();
        int bottom = areaBottom();
        g.enableScissor(left, top, cx + PANEL_W / 2 - 10, bottom);
        int y = top - this.scroll;
        for (List<FormattedCharSequence> block : this.blocks) {
            for (FormattedCharSequence line : block) {
                if (y + LINE_H > top && y < bottom) {
                    g.drawString(this.font, line, left, y, 0xFFFFFF, false);
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
            g.fill(bx, top, bx + 3, bottom, 0x55FFFFFF);
            g.fill(bx, barY, bx + 3, barY + barH, 0xFFB79AF0);
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
