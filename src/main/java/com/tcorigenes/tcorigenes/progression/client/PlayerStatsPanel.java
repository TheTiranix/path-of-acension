// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.client;

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
 * esta formado (ej. +7 de espada, +2 de anillo).
 */
public final class PlayerStatsPanel {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private static final int ROW_HEIGHT = 11;
    private static final int WIDTH = 150;

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

    /** Dibuja el panel y devuelve las lineas del tooltip de la fila bajo el mouse (null si no hay ninguna). */
    public static List<Component> render(GuiGraphics g, Font font, int mouseX, int mouseY, int x, int y) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        int height = ROWS.size() * ROW_HEIGHT + 16;
        g.fill(x - 3, y - 3, x + WIDTH, y + height, 0xAA000000);
        g.drawString(font, "Stats finales", x, y, 0xFFD700, false);
        List<Component> tooltip = null;
        int rowY = y + 12;
        for (Row row : ROWS.values()) {
            AttributeInstance instance = player.getAttribute(row.attribute().get());
            if (instance != null) {
                g.drawString(font, row.label(), x, rowY, 0xBBBBBB, false);
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
        lines.add(Component.literal(row.label() + ": " + value(instance.getValue(), row.percent())).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Base: " + value(instance.getBaseValue(), row.percent())).withStyle(ChatFormatting.GRAY));
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
