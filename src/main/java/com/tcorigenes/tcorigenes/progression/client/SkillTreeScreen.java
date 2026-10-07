// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.client;

import com.tcorigenes.tcorigenes.core.Tr;

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
    private static final float MIN_ZOOM = 0.15F;
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
    private double camX = 4.0 * 44;
    private double camY = 0;
    private float zoom = 1.0F;

    /** Fase (pagina) que se esta viendo: 1 a 3. Las fases todavia no alcanzadas estan bloqueadas y sus nodos no se muestran. */
    private int page = 1;
    private boolean cameraReady;
    private final Button[] pageButtons = new Button[3];
    private static final String[] PAGE_NAMES = {"I · Despertar", "II · Ascenso", "III · Trascendencia"};
    private static final int THIRD = SkillTree.NODES_PER_PATH / 3;

    public SkillTreeScreen() {
        super(Component.translatable("pa.msg.a29d060190"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SkillTreeScreen());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height - 54;
        confirmButton = this.addRenderableWidget(Button.builder(Component.translatable("pa.msg.81b4b67ff8"), button -> {
            if (!pending.isEmpty()) {
                Networking.sendToServer(new UnlockNodesPacket(pending.stream().map(SkillNode::id).toList()));
            }
            pending.clear();
        }).bounds(centerX - 105, y, 100, 20).build());
        buyButton = this.addRenderableWidget(Button.builder(Component.translatable("pa.msg.7f526a02de"),
                button -> Networking.sendToServer(new BuyPointPacket())).bounds(this.width - 176, 8, 168, 20).build());
        cancelButton = this.addRenderableWidget(Button.builder(Component.translatable("pa.msg.c111e0ab9d"), button -> pending.clear())
                .bounds(centerX + 5, y, 100, 20).build());
        // abajo: una pestaña por fase
        int pageY = this.height - 28;
        for (int i = 0; i < 3; i++) {
            final int target = i + 1;
            pageButtons[i] = this.addRenderableWidget(Button.builder(Component.translatable("pa.msg.fa1beb4896", PAGE_NAMES[i]), button -> setPage(target))
                    .bounds(centerX - 190 + i * 130, pageY, 126, 20).build());
        }
    }

    private void setPage(int newPage) {
        if (!isPhaseUnlocked(newPage)) {
            return;
        }
        this.page = newPage;
        double[] box = SkillTree.bounds(ClientSkillData.playerClass(), newPage);
        this.camX = (box[0] + box[2]) / 2.0 * CELL;
        this.camY = (box[1] + box[3]) / 2.0 * CELL;
        double fitW = (this.width - 60.0) / ((box[2] - box[0] + 4.0) * CELL);
        double fitH = (this.height - 140.0) / ((box[3] - box[1] + 4.0) * CELL);
        this.zoom = (float) Math.max(MIN_ZOOM, Math.min(1.0, Math.min(fitW, fitH)));
    }

    /** La fase 1 siempre esta; para entrar a una fase hay que haber llegado al ultimo nodo de la anterior (en cualquiera de los dos
     *  caminos, contando los elegidos que esperan confirmacion). */
    private boolean isPhaseUnlocked(int phase) {
        if (phase <= 1) {
            return true;
        }
        int gateStep = THIRD * (phase - 1);
        for (SkillNode node : SkillTree.forClass(ClientSkillData.playerClass())) {
            if (node.step() == gateStep && (isUnlocked(node) || pending.contains(node))) {
                return true;
            }
        }
        return false;
    }

    private void refreshPageButtons() {
        if (!isPhaseUnlocked(page)) {
            page = 1;
        }
        for (int i = 0; i < 3; i++) {
            int phase = i + 1;
            boolean unlocked = isPhaseUnlocked(phase);
            pageButtons[i].setMessage(Component.literal((unlocked ? "" : "\uD83D\uDD12 ") + Tr.s("Fase ") + PAGE_NAMES[i]));
            pageButtons[i].active = unlocked && phase != page;
        }
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
            if (node.phase() == page && Math.abs(wx - node.x() * CELL) <= HALF && Math.abs(wy - node.y() * CELL) <= HALF) {
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
        if (!cameraReady && cls != PlayerClass.NINGUNA) {
            cameraReady = true;
            setPage(1);
        }
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("tcorigenes.profile.level", 1 + ClientSkillData.bought()), 8, 12, 0xFF55FF55, true);
        g.drawCenteredString(this.font, Component.translatable("pa.msg.89f3a8977a", cls.getDisplayName(), ClientSkillData.points(), ClientSkillData.xp()), this.width / 2, 26, 0xFFD700);
        long price = SkillTreeManager.pointPrice(ClientSkillData.bought());
        buyButton.setMessage(Component.translatable("pa.msg.d9957f3eac", price));
        buyButton.active = ClientSkillData.xp() >= price;
        g.drawCenteredString(this.font, Tr.s("Arrastrá para mover la página, rueda para zoom"), this.width / 2, 38, 0x888888);

        if (cls == PlayerClass.NINGUNA) {
            g.drawCenteredString(this.font, Tr.s("Elegí una clase para ver tu árbol de habilidades."), this.width / 2, this.height / 2, 0xAAAAAA);
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

        refreshPageButtons();
        List<SkillNode> nodes = SkillTree.forClass(cls).stream().filter(n -> n.phase() == page).toList();
        g.pose().pushPose();
        g.pose().translate(this.width / 2.0, canvasCenterY(), 0);
        g.pose().scale(zoom, zoom, 1F);
        g.pose().translate(-camX, -camY, 0);
        drawPhaseBands(g);
        double viewW = this.width / 2.0 / zoom + CELL;
        double viewH = this.height / 2.0 / zoom + CELL;
        for (SkillNode node : nodes) {
            if (Math.abs(node.x() * CELL - camX) > viewW || Math.abs(node.y() * CELL - camY) > viewH) {
                continue;
            }
            for (String parentId : node.parents()) {
                SkillNode parent = SkillTree.get(parentId);
                if (parent != null && parent.phase() == page) {
                    drawLink(g, parent, node);
                }
            }
        }
        for (SkillNode node : nodes) {
            if (Math.abs(node.x() * CELL - camX) > viewW || Math.abs(node.y() * CELL - camY) > viewH) {
                continue;
            }
            int x = (int) Math.round(node.x() * CELL);
            int y = (int) Math.round(node.y() * CELL);
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
                    ? Tr.f("¿Desbloquear \"%s\" por %s punto(s)?", pending.iterator().next().title(), cost)
                    : Tr.f("¿Desbloquear %s nodos por %s punto(s) en total?", pending.size(), cost);
            if (!confirmButton.active) {
                question += Tr.s("  (te faltan puntos)");
            }
            g.drawCenteredString(this.font, question, this.width / 2, this.height - 68, confirmButton.active ? 0xFFFFFF : 0xFF7777);
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
        int px = (int) Math.round(parent.x() * CELL);
        int py = (int) Math.round(parent.y() * CELL);
        int cx = (int) Math.round(child.x() * CELL);
        int cy = (int) Math.round(child.y() * CELL);
        g.fill(Math.min(px, cx), py - 1, Math.max(px, cx) + 1, py + 1, color);
        g.fill(cx - 1, Math.min(py, cy), cx + 1, Math.max(py, cy) + 1, color);
    }

    /** Las 3 fases del arbol (ver SkillNode#phase): franjas con degradado, marco, brillo en los bordes y un cartel con nombre. */
    private void drawPhaseBands(GuiGraphics g) {
        int[] accent = {0xFF55B8FF, 0xFFFFD24A, 0xFFFF5A4A};
        String[] titles = {Tr.s("I \u00b7 DESPERTAR"), Tr.s("II \u00b7 ASCENSO"), Tr.s("III \u00b7 TRASCENDENCIA")};
        String[] subtitles = {Tr.s("los primeros pasos de tu camino"), Tr.s("el poder se afianza"), Tr.s("la cumbre de la clase")};
        double[] box = SkillTree.bounds(ClientSkillData.playerClass(), page);
        int top = (int) Math.round(box[1] * CELL) - CELL * 2;
        int bottom = (int) Math.round(box[3] * CELL) + CELL * 2;
        for (int i = 0; i < 3; i++) {
            if (i != page - 1) {
                continue;
            }
            int left = (int) Math.round(box[0] * CELL) - CELL * 2;
            int right = (int) Math.round(box[2] * CELL) + CELL * 2;
            int color = accent[i] & 0x00FFFFFF;
            // fondo: degradado vertical (mas claro arriba, oscuro abajo)
            g.fillGradient(left, top, right, bottom, 0x55000000 | color, 0x14000000 | color);
            // puntitos tenues a modo de textura
            int fromX = (int) Math.max(left + 10, camX - this.width / 2.0 / zoom);
            int toX = (int) Math.min(right - 4, camX + this.width / 2.0 / zoom);
            int fromY = (int) Math.max(top + 38, camY - this.height / 2.0 / zoom);
            int toY = (int) Math.min(bottom - 6, camY + this.height / 2.0 / zoom);
            for (int x = fromX - fromX % 44; x < toX; x += 44) {
                for (int y = fromY - fromY % 44; y < toY; y += 44) {
                    g.fill(x, y, x + 1, y + 1, 0x30000000 | color);
                }
            }
            // marco con brillo: linea fuerte al borde y dos mas suaves hacia adentro
            g.fill(left, top, right, top + 2, accent[i]);
            g.fill(left, bottom - 2, right, bottom, accent[i]);
            g.fill(left, top + 2, right, top + 4, 0x66000000 | color);
            g.fill(left, bottom - 4, right, bottom - 2, 0x66000000 | color);
            g.fill(left, top + 4, right, top + 6, 0x30000000 | color);
            g.fill(left, bottom - 6, right, bottom - 4, 0x30000000 | color);
            // divisorias verticales entre fases (la primera y la ultima son el borde del arbol)
            g.fill(left, top, left + 2, bottom, accent[i]);
            g.fill(left + 2, top, left + 5, bottom, 0x40000000 | color);
            g.fill(right - 2, top, right, bottom, accent[i]);
            g.fill(right - 5, top, right - 2, bottom, 0x40000000 | color);
            // esquinas con rombo
            for (int[] corner : new int[][] {{left, top}, {right, top}, {left, bottom}, {right, bottom}}) {
                g.fill(corner[0] - 4, corner[1] - 1, corner[0] + 4, corner[1] + 1, 0xFFFFFFFF);
                g.fill(corner[0] - 1, corner[1] - 4, corner[0] + 1, corner[1] + 4, 0xFFFFFFFF);
            }
            // cartel con el nombre de la fase
            int bannerW = Math.min(right - left - 24, 190);
            int bannerX = (left + right) / 2 - bannerW / 2;
            int bannerY = top + 8;
            g.fillGradient(bannerX, bannerY, bannerX + bannerW, bannerY + 26, 0xE0000000 | (color & 0x303030), 0xE0101010);
            g.fill(bannerX, bannerY, bannerX + bannerW, bannerY + 1, accent[i]);
            g.fill(bannerX, bannerY + 25, bannerX + bannerW, bannerY + 26, accent[i]);
            g.fill(bannerX, bannerY, bannerX + 1, bannerY + 26, accent[i]);
            g.fill(bannerX + bannerW - 1, bannerY, bannerX + bannerW, bannerY + 26, accent[i]);
            g.pose().pushPose();
            g.pose().translate((left + right) / 2.0, bannerY + 4, 0);
            g.pose().scale(1.25F, 1.25F, 1F);
            g.drawCenteredString(this.font, "\u2726 FASE " + titles[i] + " \u2726", 0, 0, accent[i]);
            g.pose().popPose();
            g.drawCenteredString(this.font, subtitles[i], (left + right) / 2, bannerY + 16, 0xFFAAAAAA);
        }
    }

    private List<Component> tooltipFor(SkillNode node) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(node.title()).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("pa.msg.a8560a918d", node.phase()).withStyle(ChatFormatting.DARK_AQUA));
        if (node.attribute() != null && node.attribute().get() == com.tudominio.elementaldamage.ModAttributes.ELEMENT_ABSORB.get()) {
            lines.add(Component.translatable("pa.msg.17021bac90")
                    .withStyle(ChatFormatting.GREEN));
            lines.add(Component.translatable("pa.msg.3effbd9ca1").withStyle(ChatFormatting.GREEN));
            lines.add(Component.translatable("pa.msg.15474e4c4d").withStyle(ChatFormatting.GREEN));
        } else if (node.attribute() != null) {
            String value = node.operation() == AttributeModifier.Operation.ADDITION
                    ? "+" + FORMAT.format(node.amount())
                    : "+" + FORMAT.format(node.amount() * 100) + "%";
            lines.add(Component.literal(value + " ").append(Component.translatable(node.attribute().get().getDescriptionId()))
                    .withStyle(ChatFormatting.GREEN));
        }
        if (node.abilityId() != null) {
            lines.add(Component.translatable("pa.msg.121297af80").withStyle(ChatFormatting.AQUA));
        }
        int color = colorOf(node);
        if (color == COLOR_UNLOCKED) {
            lines.add(Component.translatable("pa.msg.6cea108677").withStyle(ChatFormatting.GREEN));
        } else if (color == COLOR_PENDING) {
            lines.add(Component.translatable("pa.msg.9ff975e204", node.cost()).withStyle(ChatFormatting.AQUA));
        } else if (color == COLOR_AVAILABLE) {
            lines.add(Component.translatable("pa.msg.811cbcfbcc", node.cost()).withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.translatable("pa.msg.59bbbba193", node.cost()).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
