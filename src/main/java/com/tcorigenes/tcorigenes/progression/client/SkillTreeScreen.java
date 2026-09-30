// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.client;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.progression.SkillNode;
import com.tcorigenes.tcorigenes.progression.SkillTree;
import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import com.tcorigenes.tcorigenes.progression.network.BuyPointPacket;
import com.tcorigenes.tcorigenes.progression.network.UnlockNodesPacket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Arbol de habilidades propio: pagina libre (como el PassiveSkillTree) en vez de una tira que solo se
 * recorre de izquierda a derecha - se arrastra en cualquier direccion y se hace zoom con la rueda.
 * Verde = desbloqueado, celeste = elegido (esperando "Confirmar"), amarillo = disponible, gris =
 * bloqueado. Se pueden elegir VARIOS nodos (incluso encadenados entre si) antes de confirmar de una vez.
 */
public class SkillTreeScreen extends Screen {
    private static final int CELL = 44;
    private static final int HALF = 13;
    private static final int COLOR_UNLOCKED = 0xFF55FF55;
    private static final int COLOR_PENDING = 0xFF55CCFF;
    private static final int COLOR_AVAILABLE = 0xFFFFD700;
    private static final int COLOR_LOCKED = 0xFF666666;
    private static final float MIN_ZOOM = 0.45F;
    private static final float MAX_ZOOM = 2.2F;
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    /** Nodos elegidos que esperan confirmacion (vacio = ninguno). */
    private final Set<SkillNode> pending = new LinkedHashSet<>();
    private Button confirmButton;
    private Button cancelButton;
    private Button buyButton;
    /** Mientras es true, arrastrar el mouse mueve el panel de stats en vez de la camara (ver mouseClicked). */
    private boolean draggingStatsPanel;

    /** Camara: punto del MUNDO (coordenadas de nodo * CELL) que queda en el centro de la pagina. */
    private double camX = 0;
    private double camY = 0;
    private float zoom = 1.0F;

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
            if (!pending.isEmpty()) {
                Networking.sendToServer(new UnlockNodesPacket(pending.stream().map(SkillNode::id).toList()));
            }
            pending.clear();
        }).bounds(centerX - 105, y, 100, 20).build());
        buyButton = this.addRenderableWidget(Button.builder(Component.literal("Comprar punto"),
                button -> Networking.sendToServer(new BuyPointPacket())).bounds(this.width - 176, 8, 168, 20).build());
        cancelButton = this.addRenderableWidget(Button.builder(Component.literal("Cancelar"), button -> pending.clear())
                .bounds(centerX + 5, y, 100, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int canvasCenterY() {
        return this.height / 2 + 12;
    }

    /** Screen -> mundo. */
    private double worldX(double screenX) {
        return (screenX - this.width / 2.0) / zoom + camX;
    }

    private double worldY(double screenY) {
        return (screenY - canvasCenterY()) / zoom + camY;
    }

    /** Mundo -> screen. */
    private double screenX(double worldX) {
        return this.width / 2.0 + (worldX - camX) * zoom;
    }

    private double screenY(double worldY) {
        return canvasCenterY() + (worldY - camY) * zoom;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (draggingStatsPanel) {
                PlayerStatsPanel.moveBy(dragX, dragY, this.width, this.height);
            } else {
                camX -= dragX / zoom;
                camY -= dragY / zoom;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingStatsPanel = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Hace zoom centrado en el mouse: el punto del mundo bajo el cursor queda fijo.
        double beforeX = worldX(mouseX);
        double beforeY = worldY(mouseY);
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * (1.0F + (float) delta * 0.12F)));
        camX = beforeX - (mouseX - this.width / 2.0) / zoom;
        camY = beforeY - (mouseY - canvasCenterY()) / zoom;
        return true;
    }

    private SkillNode hoveredNode(double mouseX, double mouseY) {
        double wx = worldX(mouseX);
        double wy = worldY(mouseY);
        for (SkillNode node : SkillTree.forClass(ClientSkillData.playerClass())) {
            if (Math.abs(wx - node.x() * CELL) <= HALF && Math.abs(wy - node.y() * CELL) <= HALF) {
                return node;
            }
        }
        return null;
    }

    /** true si YA esta desbloqueado. */
    private boolean isUnlocked(SkillNode node) {
        return ClientSkillData.unlocked().contains(node.storageKey());
    }

    /** Disponible teniendo en cuenta lo ya desbloqueado MAS lo elegido en esta tanda (para poder
     *  encadenar varios nodos seguidos sin tener que confirmar uno por uno). */
    private boolean isEffectivelyAvailable(SkillNode node) {
        if (isUnlocked(node) || pending.contains(node)) {
            return false;
        }
        if (node.parents().isEmpty()) {
            return true;
        }
        for (String parentId : node.parents()) {
            SkillNode parent = SkillTree.get(parentId);
            if (parent != null && (isUnlocked(parent) || pending.contains(parent))) {
                return true;
            }
        }
        return false;
    }

    private int colorOf(SkillNode node) {
        if (isUnlocked(node)) {
            return COLOR_UNLOCKED;
        }
        if (pending.contains(node)) {
            return COLOR_PENDING;
        }
        return isEffectivelyAvailable(node) ? COLOR_AVAILABLE : COLOR_LOCKED;
    }

    private int pendingCost() {
        return pending.stream().mapToInt(SkillNode::cost).sum();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && PlayerStatsPanel.isOver(mouseX, mouseY)) {
            draggingStatsPanel = true; // se suelta en mouseReleased
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        SkillNode node = hoveredNode(mouseX, mouseY);
        if (node == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (pending.contains(node)) {
            // Sacarlo de la tanda: si algun OTRO elegido dependia solo de este, tambien se saca (en cadena).
            pending.remove(node);
            pending.removeIf(other -> !isReachable(other));
            return true;
        }
        if (isEffectivelyAvailable(node)) {
            pending.add(node);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Para depurar la tanda al sacar un nodo: true si sigue teniendo como llegar (desbloqueado o algun
     *  padre todavia elegido) sin contarse a si mismo. */
    private boolean isReachable(SkillNode node) {
        if (node.parents().isEmpty()) {
            return true;
        }
        for (String parentId : node.parents()) {
            SkillNode parent = SkillTree.get(parentId);
            if (parent != null && (isUnlocked(parent) || pending.contains(parent))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        // Ambientacion del pack (rojo sangre oscuro) en vez del fondo vanilla liso.
        g.fillGradient(0, 0, this.width, 44, 0x9A2A0505, 0x002A0505);
        g.fill(0, 43, this.width, 44, 0xFF5A0A0A);
        PlayerClass cls = ClientSkillData.playerClass();
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.literal(
                "Clase: " + cls.getDisplayName() + "   |   Puntos: " + ClientSkillData.points()
                        + "   |   XP: " + ClientSkillData.xp()), this.width / 2, 26, 0xFFD700);
        long price = SkillTreeManager.pointPrice(ClientSkillData.bought());
        buyButton.setMessage(Component.literal("Comprar punto (" + price + " XP)"));
        buyButton.active = ClientSkillData.xp() >= price;
        g.drawCenteredString(this.font, "Arrastrá para mover la página, rueda para zoom", this.width / 2, 38, 0x888888);

        if (cls == PlayerClass.NINGUNA) {
            g.drawCenteredString(this.font, "Elegí una clase para ver tu árbol de habilidades.", this.width / 2, this.height / 2, 0xAAAAAA);
            confirmButton.visible = false;
            cancelButton.visible = false;
            super.render(g, mouseX, mouseY, partialTick);
            List<Component> emptyStatTip = PlayerStatsPanel.render(g, this.font, mouseX, mouseY, 8, 50);
            if (emptyStatTip != null) {
                g.renderComponentTooltip(this.font, emptyStatTip, mouseX, mouseY);
            }
            return;
        }

        // Los pendientes que dejaron de tener sentido (se desbloquearon por otra via, o su padre elegido
        // se saco) se descartan solos.
        pending.removeIf(n -> isUnlocked(n) || !isReachable(n));

        List<SkillNode> nodes = SkillTree.forClass(cls);
        g.pose().pushPose();
        g.pose().translate(this.width / 2.0, canvasCenterY(), 0);
        g.pose().scale(zoom, zoom, 1F);
        g.pose().translate(-camX, -camY, 0);
        drawPhaseBands(g);
        for (SkillNode node : nodes) {
            for (String parentId : node.parents()) {
                SkillNode parent = SkillTree.get(parentId);
                if (parent != null) {
                    drawLink(g, parent, node);
                }
            }
        }
        for (SkillNode node : nodes) {
            int x = node.x() * CELL;
            int y = node.y() * CELL;
            int color = colorOf(node);
            g.fill(x - HALF - 2, y - HALF - 2, x + HALF + 2, y + HALF + 2, color);
            g.fill(x - HALF, y - HALF, x + HALF, y + HALF, 0xFF1A0808);
            g.renderItem(new ItemStack(node.icon().get()), x - 8, y - 8);
        }
        g.pose().popPose();

        boolean hasPending = !pending.isEmpty();
        confirmButton.visible = hasPending;
        cancelButton.visible = hasPending;
        if (hasPending) {
            int cost = pendingCost();
            confirmButton.active = ClientSkillData.points() >= cost;
            String question = pending.size() == 1
                    ? "¿Desbloquear \"" + pending.iterator().next().title() + "\" por " + cost + " punto(s)?"
                    : "¿Desbloquear " + pending.size() + " nodos por " + cost + " punto(s) en total?";
            if (!confirmButton.active) {
                question += "  (te faltan puntos)";
            }
            g.drawCenteredString(this.font, question, this.width / 2, this.height - 48, confirmButton.active ? 0xFFFFFF : 0xFF7777);
        }

        SkillNode hovered = hoveredNode(mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
        List<Component> statTip = PlayerStatsPanel.render(g, this.font, mouseX, mouseY, 8, 50);
        if (hovered != null) {
            g.renderComponentTooltip(this.font, tooltipFor(hovered), mouseX, mouseY);
        } else if (statTip != null) {
            g.renderComponentTooltip(this.font, statTip, mouseX, mouseY);
        }
    }

    private void drawLink(GuiGraphics g, SkillNode parent, SkillNode child) {
        int color = isUnlocked(parent) || pending.contains(parent) ? COLOR_UNLOCKED : COLOR_LOCKED;
        int px = parent.x() * CELL;
        int py = parent.y() * CELL;
        int cx = child.x() * CELL;
        int cy = child.y() * CELL;
        g.fill(Math.min(px, cx), py - 1, Math.max(px, cx) + 1, py + 1, color);
        g.fill(cx - 1, Math.min(py, cy), cx + 1, Math.max(py, cy) + 1, color);
    }

    /** Las 3 fases del arbol: franjas verticales con su nombre (ver SkillNode#phase). */
    private void drawPhaseBands(GuiGraphics g) {
        int[][] ranges = {{0, 8}, {9, 16}, {17, 25}};
        int[] colors = {0x2255AAFF, 0x22FFD700, 0x22FF5555};
        for (int i = 0; i < ranges.length; i++) {
            int left = ranges[i][0] * CELL - CELL / 2;
            int right = ranges[i][1] * CELL + CELL / 2;
            g.fill(left, -3 * CELL, right, 3 * CELL, colors[i]);
            g.drawString(this.font, "FASE " + (i + 1), left + 8, -3 * CELL + 6, 0xFFFFFFFF, false);
        }
    }

    private List<Component> tooltipFor(SkillNode node) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(node.title()).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Fase " + node.phase() + " de 3").withStyle(ChatFormatting.DARK_AQUA));
        if (node.attribute() != null && node.attribute().get() == com.tudominio.elementaldamage.ModAttributes.ELEMENT_ABSORB.get()) {
            lines.add(Component.literal("Tus golpes convierten los daños elementales que no son el de tu raza en el de tu raza.")
                    .withStyle(ChatFormatting.GREEN));
            lines.add(Component.literal("Sin elemento racial, todo se absorbe en el elemento mayor del arma.").withStyle(ChatFormatting.GREEN));
            lines.add(Component.literal("El efecto especial solo usa el daño base del elemento que absorbe.").withStyle(ChatFormatting.GREEN));
        } else if (node.attribute() != null) {
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
        } else if (color == COLOR_PENDING) {
            lines.add(Component.literal("Elegido (click para sacarlo) - costo " + node.cost() + " punto(s)").withStyle(ChatFormatting.AQUA));
        } else if (color == COLOR_AVAILABLE) {
            lines.add(Component.literal("Costo: " + node.cost() + " punto(s) - click para elegirlo").withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.literal("Bloqueado: desbloqueá un nodo anterior (costo " + node.cost() + ")").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
