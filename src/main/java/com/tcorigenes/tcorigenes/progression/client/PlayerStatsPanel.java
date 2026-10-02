// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.client;

import com.tcorigenes.tcorigenes.core.Tr;

import com.tudominio.elementaldamage.ModAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

/**
 * Panel de stats finales del jugador dentro del arbol de habilidades (pedido de alejandr0): cada numero es el
 * resultado de todos los modificadores juntos (items, raza, clase, arbol...) y al pasar el mouse muestra de que
 * esta formado (ej. +7 de espada, +2 de anillo). Se puede arrastrar con el mouse (ver SkillTreeScreen) para que no
 * tape otros overlays (ej. HUDs de rendimiento en la esquina), y el fondo tiene el tono sangre/oscuro del pack
 * en vez de un cuadro negro liso.
 */
public final class PlayerStatsPanel {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private static final int ROW_HEIGHT = 11;
    private static final int WIDTH = 150;
    private static final int BORDER = 0xFF5A0A0A;

    /** Posicion actual del panel (se arrastra con el mouse); se resetea sola si queda fuera de una pantalla chica. */
    private static int panelX = 8;
    private static int panelY = 50;
    private static int lastScreenW = -1;
    private static int lastScreenH = -1;

    private record Row(String label, Supplier<Attribute> attribute, boolean percent) {
    }

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();

    private static void row(String label, Supplier<Attribute> attribute, boolean percent) {
        ROWS.put(label, new Row(label, attribute, percent));
    }

    static {
        row("Vida máxima", () -> Attributes.MAX_HEALTH, false);
        row("Armadura", () -> Attributes.ARMOR, false);
        row("Dureza de armadura", () -> Attributes.ARMOR_TOUGHNESS, false);
        row("Daño de ataque", () -> Attributes.ATTACK_DAMAGE, false);
        row("Velocidad de ataque", () -> Attributes.ATTACK_SPEED, false);
        row("Alcance", () -> ForgeMod.ENTITY_REACH.get(), false);
        row("Velocidad de movimiento", () -> Attributes.MOVEMENT_SPEED, false);
        row("Prob. de crítico", () -> ModAttributes.CRIT_CHANCE.get(), true);
        row("Daño crítico", () -> ModAttributes.CRIT_DAMAGE.get(), false);
        row("Esquive", () -> ModAttributes.DODGE_CHANCE.get(), true);
        row("Resist. luz", () -> ModAttributes.RESIST_LIGHT.get(), true);
        row("Resist. fuego", () -> ModAttributes.RESIST_FIRE.get(), true);
        row("Resist. agua", () -> ModAttributes.RESIST_WATER.get(), true);
        row("Resist. lunar", () -> ModAttributes.RESIST_LUNAR.get(), true);
        row("Resist. ender", () -> ModAttributes.RESIST_ENDER.get(), true);
    }

    private PlayerStatsPanel() {
    }

    private static String value(double v, boolean percent) {
        return percent ? FORMAT.format(v * 100.0) + "%" : FORMAT.format(v);
    }

    /** Alto total del panel con las filas actuales (para saber si el mouse esta encima, ver isOver). */
    private static int height() {
        return ROWS.size() * ROW_HEIGHT + 16;
    }

    /** true si el punto esta sobre el panel (para poder arrastrarlo, ver SkillTreeScreen). */
    public static boolean isOver(double mouseX, double mouseY) {
        return mouseX >= panelX - 3 && mouseX <= panelX + WIDTH && mouseY >= panelY - 3 && mouseY <= panelY + height();
    }

    public static void moveBy(double dx, double dy, int screenW, int screenH) {
        panelX = (int) Math.round(Math.max(0, Math.min(screenW - WIDTH, panelX + dx)));
        panelY = (int) Math.round(Math.max(0, Math.min(screenH - height(), panelY + dy)));
    }

    /** Dibuja el panel y devuelve las lineas del tooltip de la fila bajo el mouse (null si no hay ninguna). Los
     *  x/y que recibe son solo la posicion INICIAL (primera vez que se abre en una pantalla de este tamaño). */
    public static List<Component> render(GuiGraphics g, Font font, int mouseX, int mouseY, int defaultX, int defaultY) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (screenW != lastScreenW || screenH != lastScreenH) {
            lastScreenW = screenW;
            lastScreenH = screenH;
            panelX = defaultX;
            panelY = defaultY;
        }
        int x = panelX;
        int y = panelY;
        int height = height();
        // Fondo con el tono sangre/oscuro del pack (degrade), no un cuadro negro liso, mas un borde.
        g.fillGradient(x - 3, y - 3, x + WIDTH, y + height, 0xE02A0505, 0xF0120202);
        g.fill(x - 3, y - 3, x + WIDTH, y - 2, BORDER);
        g.fill(x - 3, y + height - 1, x + WIDTH, y + height, BORDER);
        g.fill(x - 3, y - 3, x - 2, y + height, BORDER);
        g.fill(x + WIDTH - 1, y - 3, x + WIDTH, y + height, BORDER);
        g.drawString(font, "Stats finales", x, y, 0xFFD700, false);
        g.fill(x, y + 9, x + WIDTH - 6, y + 10, 0x665A0A0A);
        List<Component> tooltip = null;
        int rowY = y + 12;
        for (Row row : ROWS.values()) {
            AttributeInstance instance = player.getAttribute(row.attribute().get());
            if (instance != null) {
                g.drawString(font, Tr.s(row.label()), x, rowY, 0xBBBBBB, false);
                String shown = value(instance.getValue(), row.percent());
                g.drawString(font, shown, x + WIDTH - 6 - font.width(shown), rowY, 0xFFFFFF, false);
                if (mouseX >= x - 3 && mouseX <= x + WIDTH && mouseY >= rowY - 1 && mouseY < rowY + ROW_HEIGHT - 1) {
                    tooltip = breakdown(row, instance);
                }
            }
            rowY += ROW_HEIGHT;
        }
        return tooltip;
    }

    private static List<Component> breakdown(Row row, AttributeInstance instance) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(Tr.s(row.label()) + ": " + value(instance.getValue(), row.percent())).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("pa.msg.389a80ba02", value(instance.getBaseValue(), row.percent())).withStyle(ChatFormatting.GRAY));
        for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) {
            for (AttributeModifier modifier : instance.getModifiers(operation)) {
                double amount = modifier.getAmount();
                String text;
                if (operation == AttributeModifier.Operation.ADDITION) {
                    text = (amount >= 0 ? "+" : "") + value(amount, row.percent());
                } else if (operation == AttributeModifier.Operation.MULTIPLY_BASE) {
                    text = (amount >= 0 ? "+" : "") + FORMAT.format(amount * 100.0) + "% (base)";
                } else {
                    text = (amount >= 0 ? "+" : "") + FORMAT.format(amount * 100.0) + "% (total)";
                }
                lines.add(Component.literal(" " + text + "  " + modifier.getName())
                        .withStyle(amount >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }
        return lines;
    }
}
