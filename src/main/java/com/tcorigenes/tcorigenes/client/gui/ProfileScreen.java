// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.client.ClientRaceData;
import com.tcorigenes.tcorigenes.client.HudConfig;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.favor.Deity;
import com.tcorigenes.tcorigenes.favor.client.ClientFavorData;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Pantalla de perfil del personaje (personalizacion): pestaña "Perfil" con raza, clase, nivel de progresion, favor de cada dios y estadisticas
 * principales; pestaña "Interfaz" con las opciones de la barra de vida y la HUD (ver HudConfig). Se abre con la tecla del perfil (O por defecto).
 */
public class ProfileScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int HEIGHT = 200;
    private static final int BORDER = 0xFF5A0A0A;
    private static final int BACKGROUND = 0xE6140808;
    private static final int FAVOR_RANGE = 100;

    private final int tier;
    private final PlayerClass playerClass;
    private boolean interfaceTab;
    private Button healthColorButton;
    private Button healthTextButton;
    private Button customHudButton;

    public ProfileScreen(int tier, int classOrdinal) {
        super(Component.translatable("tcorigenes.profile.title"));
        this.tier = Mth.clamp(tier, 1, 4);
        PlayerClass[] classes = PlayerClass.values();
        this.playerClass = classOrdinal >= 0 && classOrdinal < classes.length ? classes[classOrdinal] : PlayerClass.NINGUNA;
    }

    public static void open(int tier, int classOrdinal) {
        Minecraft.getInstance().setScreen(new ProfileScreen(tier, classOrdinal));
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return (this.height - HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int x = left();
        int y = top();
        addRenderableWidget(Button.builder(Component.translatable("tcorigenes.profile.tab.profile"), button -> {
            interfaceTab = false;
            rebuildWidgets();
        }).bounds(x + 8, y + 24, 118, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("tcorigenes.profile.tab.interface"), button -> {
            interfaceTab = true;
            rebuildWidgets();
        }).bounds(x + 134, y + 24, 118, 16).build());
        if (interfaceTab) {
            customHudButton = addRenderableWidget(Button.builder(customHudLabel(), button -> {
                HudConfig.toggle(HudConfig.CUSTOM_HUD);
                button.setMessage(customHudLabel());
            }).bounds(x + 20, y + 62, WIDTH - 40, 20).build());
            healthColorButton = addRenderableWidget(Button.builder(healthColorLabel(), button -> {
                HudConfig.nextHealthColor();
                button.setMessage(healthColorLabel());
            }).bounds(x + 20, y + 90, WIDTH - 40, 20).build());
            healthTextButton = addRenderableWidget(Button.builder(healthTextLabel(), button -> {
                HudConfig.toggle(HudConfig.HEALTH_TEXT);
                button.setMessage(healthTextLabel());
            }).bounds(x + 20, y + 118, WIDTH - 40, 20).build());
        }
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "tcorigenes.profile.on" : "tcorigenes.profile.off");
    }

    private static Component customHudLabel() {
        return Component.translatable("tcorigenes.profile.opt.custom_hud", onOff(HudConfig.CUSTOM_HUD.get()));
    }

    private static Component healthTextLabel() {
        return Component.translatable("tcorigenes.profile.opt.health_text", onOff(HudConfig.HEALTH_TEXT.get()));
    }

    private static Component healthColorLabel() {
        return Component.translatable("tcorigenes.profile.opt.health_color",
                Component.translatable("tcorigenes.profile.color." + HudConfig.HEALTH_COLOR.get()));
    }

    private static int godColor(Deity deity) {
        return switch (deity) {
            case PATER -> 0xFFE8C84A;
            case DEIROS -> 0xFFE05A2A;
            case MEIDRIS -> 0xFF4CB85A;
            case FILIS -> 0xFFE88AB4;
            case LUNA -> 0xFF8A8AE8;
            case TEMPO -> 0xFF8A4AE8;
        };
    }

    private static String raceElementKey(Race race) {
        return switch (race) {
            case DEMONIO -> "fire";
            case ANGEL -> "light";
            case SIERVO_DE_LA_LUNA -> "lunar";
            case ENDER_WARRIOR -> "ender";
            case STONE_GIANT -> "earth";
            case AUTOMATA -> "air";
            default -> "none";
        };
    }

    private static String number(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int x = left();
        int y = top();
        g.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, BORDER);
        g.fill(x, y, x + WIDTH, y + HEIGHT, BACKGROUND);
        g.drawCenteredString(font, title, x + WIDTH / 2, y + 8, 0xFFE8C84A);
        Player player = minecraft == null ? null : minecraft.player;
        if (player != null) {
            if (interfaceTab) {
                g.drawCenteredString(font, Component.translatable("tcorigenes.profile.interface_hint"), x + WIDTH / 2, y + 46, 0xFFAAAAAA);
            } else {
                renderProfile(g, player, x, y);
            }
        }
        super.render(g, mouseX, mouseY, partial);
    }

    private void renderProfile(GuiGraphics g, Player player, int x, int y) {
        Race race = ClientRaceData.get(player.getUUID());
        int line = y + 48;
        g.drawString(font, player.getName(), x + 12, line, 0xFFFFFFFF, true);
        line += 13;
        g.drawString(font, Component.translatable("tcorigenes.profile.race", race.getDisplayName()), x + 12, line, 0xFFCCCCCC, false);
        g.drawString(font, Component.translatable("tcorigenes.profile.class", playerClass.getDisplayName()), x + 134, line, 0xFFCCCCCC, false);
        g.drawString(font, Component.translatable("tcorigenes.profile.level", 1 + com.tcorigenes.tcorigenes.progression.client.ClientSkillData.bought())
                .withStyle(ChatFormatting.GREEN), x + 134, line + 12, 0xFFFFFFFF, false);
        line += 12;
        g.drawString(font, Component.translatable("tcorigenes.profile.element",
                Component.translatable("tcorigenes.profile.element." + raceElementKey(race))), x + 12, line, 0xFFCCCCCC, false);
        line += 12;
        g.drawString(font, Component.translatable("tcorigenes.profile.tier", tier, Component.translatable("tcorigenes.profile.tier." + tier)),
                x + 12, line, 0xFFE8C84A, false);
        for (int i = 0; i < 4; i++) {
            g.fill(x + 12 + i * 22, line + 11, x + 12 + i * 22 + 20, line + 15, i < tier ? 0xFFE8C84A : 0xFF3A2A2A);
        }
        line += 24;
        g.drawString(font, Component.translatable("tcorigenes.profile.favor").withStyle(ChatFormatting.BOLD), x + 12, line, 0xFFFFFFFF, false);
        line += 11;
        for (Deity deity : Deity.values()) {
            int favor = ClientFavorData.getFavor(deity);
            if (deity == Deity.TEMPO && favor == 0) {
                continue; // Tempo es un secreto del lore: solo aparece si ya tiene favor con el
            }
            g.drawString(font, deity.getDisplayName(), x + 12, line, godColor(deity), false);
            int barX = x + 70;
            int barW = 130;
            g.fill(barX, line, barX + barW, line + 8, 0xFF2A1A1A);
            int mid = barX + barW / 2;
            int len = Math.round(Mth.clamp(favor, -FAVOR_RANGE, FAVOR_RANGE) / (float) FAVOR_RANGE * (barW / 2));
            g.fill(Math.min(mid, mid + len), line, Math.max(mid, mid + len), line + 8, godColor(deity));
            g.fill(mid, line - 1, mid + 1, line + 9, 0xFFFFFFFF);
            g.drawString(font, (favor > 0 ? "+" : "") + favor, barX + barW + 6, line, 0xFFCCCCCC, false);
            line += 11;
        }
        line += 4;
        g.drawString(font, Component.translatable("tcorigenes.profile.stats",
                number(player.getMaxHealth()), number(player.getArmorValue()), number(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS))),
                x + 12, line, 0xFFCCCCCC, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
