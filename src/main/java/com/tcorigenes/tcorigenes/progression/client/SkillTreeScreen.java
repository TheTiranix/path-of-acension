// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.client;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.progression.SkillNode;
import com.tcorigenes.tcorigenes.progression.SkillTree;
import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import com.tcorigenes.tcorigenes.progression.network.UnlockNodePacket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Arbol de habilidades propio: muestra la rama de TU clase. Verde = desbloqueado, amarillo =
 * disponible (click para gastar puntos), gris = bloqueado (falta desbloquear un nodo anterior).
 */
public class SkillTreeScreen extends Screen {
    private static final int CELL = 44;
    private static final int HALF = 13;
    private static final int COLOR_UNLOCKED = 0xFF55FF55;
    private static final int COLOR_AVAILABLE = 0xFFFFD700;
    private static final int COLOR_LOCKED = 0xFF666666;
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    /** Nodo elegido que espera confirmacion (null = ninguno). */
    private SkillNode pending;
    private Button confirmButton;
    private Button cancelButton;

    public SkillTreeScreen() {
        super(Component.literal("Árbol de Habilidades"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SkillTreeScreen());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height - 34;
        confirmButton = this.addRenderableWidget(Button.builder(Component.literal("Confirmar"), button -> {
            if (pending != null && colorOf(pending) == COLOR_AVAILABLE) {
                Networking.sendToServer(new UnlockNodePacket(pending.id()));
            }
            pending = null;
        }).bounds(centerX - 105, y, 100, 20).build());
        cancelButton = this.addRenderableWidget(Button.builder(Component.literal("Cancelar"), button -> pending = null)
                .bounds(centerX + 5, y, 100, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Desplazamiento horizontal del arbol (arrastrar con el mouse o rueda): las ramas miden 11 columnas. */
    private int panX = 0;

    private int minPan() {
        return Math.min(0, this.width - 40 - (30 + (SkillTree.NODES_PER_PATH + 1) * CELL + 30));
    }

    private int cx() {
        return 40 + panX;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            panX = Math.max(minPan(), Math.min(0, panX + (int) dragX));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        panX = Math.max(minPan(), Math.min(0, panX + (int) (delta * 30)));
        return true;
    }

    private int cy() {
        return this.height / 2 + 6;
    }

    private int nodeX(SkillNode node) {
        return cx() + node.x() * CELL;
    }

    private int nodeY(SkillNode node) {
        return cy() + node.y() * CELL;
    }

    private SkillNode hoveredNode(double mouseX, double mouseY) {
        for (SkillNode node : SkillTree.forClass(ClientSkillData.playerClass())) {
            if (Math.abs(mouseX - nodeX(node)) <= HALF && Math.abs(mouseY - nodeY(node)) <= HALF) {
                return node;
            }
        }
        return null;
    }

    private int colorOf(SkillNode node) {
        if (ClientSkillData.unlocked().contains(node.storageKey())) {
            return COLOR_UNLOCKED;
        }
        return SkillTreeManager.isAvailable(node, ClientSkillData.playerClass(), ClientSkillData.unlocked())
                ? COLOR_AVAILABLE : COLOR_LOCKED;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SkillNode node = hoveredNode(mouseX, mouseY);
        if (node != null && button == 0 && colorOf(node) == COLOR_AVAILABLE) {
            pending = node; // no se gasta nada hasta apretar "Confirmar"
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        PlayerClass cls = ClientSkillData.playerClass();
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.literal(
                "Clase: " + cls.getDisplayName() + "   |   Puntos: " + ClientSkillData.points()), this.width / 2, 26, 0xFFD700);
        if (minPan() < 0) {
            g.drawCenteredString(this.font, "Arrastrá con el mouse o usá la rueda para recorrer la rama", this.width / 2, 38, 0x888888);
        }

        if (cls == PlayerClass.NINGUNA) {
            g.drawCenteredString(this.font, "Elegí una clase para ver tu árbol de habilidades.", this.width / 2, this.height / 2, 0xAAAAAA);
            confirmButton.visible = false;
            cancelButton.visible = false;
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        List<SkillNode> nodes = SkillTree.forClass(cls);
        for (SkillNode node : nodes) {
            for (String parentId : node.parents()) {
                SkillNode parent = SkillTree.get(parentId);
                if (parent != null) {
                    drawLink(g, parent, node);
                }
            }
        }
        for (SkillNode node : nodes) {
            int x = nodeX(node);
            int y = nodeY(node);
            int color = colorOf(node);
            g.fill(x - HALF - 2, y - HALF - 2, x + HALF + 2, y + HALF + 2, color);
            g.fill(x - HALF, y - HALF, x + HALF, y + HALF, 0xFF1E1E1E);
            g.renderItem(new ItemStack(node.icon().get()), x - 8, y - 8);
        }

        // Si el nodo pendiente ya se desbloqueo (o dejo de estar disponible), se descarta.
        if (pending != null && colorOf(pending) != COLOR_AVAILABLE) {
            pending = null;
        }
        boolean hasPending = pending != null;
        confirmButton.visible = hasPending;
        cancelButton.visible = hasPending;
        if (hasPending) {
            confirmButton.active = ClientSkillData.points() >= pending.cost();
            int x = nodeX(pending);
            int y = nodeY(pending);
            g.fill(x - HALF - 4, y - HALF - 4, x + HALF + 4, y + HALF + 4, 0xFFFFFFFF);
            g.fill(x - HALF - 2, y - HALF - 2, x + HALF + 2, y + HALF + 2, colorOf(pending));
            g.fill(x - HALF, y - HALF, x + HALF, y + HALF, 0xFF1E1E1E);
            g.renderItem(new ItemStack(pending.icon().get()), x - 8, y - 8);
            String question = "¿Desbloquear \"" + pending.title() + "\" por " + pending.cost() + " punto(s)?"
                    + (confirmButton.active ? "" : "  (te faltan puntos)");
            g.drawCenteredString(this.font, question, this.width / 2, this.height - 48, confirmButton.active ? 0xFFFFFF : 0xFF7777);
        }

        SkillNode hovered = hoveredNode(mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
        if (hovered != null) {
            g.renderComponentTooltip(this.font, tooltipFor(hovered), mouseX, mouseY);
        }
    }

    private void drawLink(GuiGraphics g, SkillNode parent, SkillNode child) {
        int color = ClientSkillData.unlocked().contains(parent.storageKey()) ? COLOR_UNLOCKED : COLOR_LOCKED;
        int px = nodeX(parent);
        int py = nodeY(parent);
        int cx = nodeX(child);
        int cy = nodeY(child);
        g.fill(Math.min(px, cx), py - 1, Math.max(px, cx) + 1, py + 1, color);
        g.fill(cx - 1, Math.min(py, cy), cx + 1, Math.max(py, cy) + 1, color);
    }

    private List<Component> tooltipFor(SkillNode node) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(node.title()).withStyle(ChatFormatting.GOLD));
        if (node.attribute() != null) {
            String value = node.operation() == AttributeModifier.Operation.ADDITION
                    ? "+" + FORMAT.format(node.amount())
                    : "+" + FORMAT.format(node.amount() * 100) + "%";
            lines.add(Component.literal(value + " ").append(Component.translatable(node.attribute().get().getDescriptionId()))
                    .withStyle(ChatFormatting.GREEN));
        }
        if (node.abilityId() != null) {
            lines.add(Component.literal("Desbloquea la habilidad activa de tu clase").withStyle(ChatFormatting.AQUA));
        }
        int color = colorOf(node);
        if (color == COLOR_UNLOCKED) {
            lines.add(Component.literal("Desbloqueado").withStyle(ChatFormatting.GREEN));
        } else if (color == COLOR_AVAILABLE) {
            lines.add(Component.literal("Costo: " + node.cost() + " punto(s) - click para elegirlo").withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.literal("Bloqueado: desbloqueá un nodo anterior (costo " + node.cost() + ")").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
