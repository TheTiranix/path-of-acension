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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Arbol de habilidades propio: muestra la rama de TU clase. Verde = desbloqueado, amarillo =
 * disponible (click para gastar puntos), gris = bloqueado (falta desbloquear un nodo anterior).
 */
public class SkillTreeScreen extends Screen {
    private static final int CELL = 46;
    private static final int HALF = 13;
    private static final int COLOR_UNLOCKED = 0xFF55FF55;
    private static final int COLOR_AVAILABLE = 0xFFFFD700;
    private static final int COLOR_LOCKED = 0xFF666666;
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    public SkillTreeScreen() {
        super(Component.literal("Árbol de Habilidades"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SkillTreeScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int cx() {
        return this.width / 2 - CELL * 2 + 10;
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
            Networking.sendToServer(new UnlockNodePacket(node.id()));
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

        if (cls == PlayerClass.NINGUNA) {
            g.drawCenteredString(this.font, "Elegí una clase para ver tu árbol de habilidades.", this.width / 2, this.height / 2, 0xAAAAAA);
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
            lines.add(Component.literal("Costo: " + node.cost() + " punto(s) - click para desbloquear").withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.literal("Bloqueado: desbloqueá un nodo anterior (costo " + node.cost() + ")").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
