package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.ChooseClassPacket;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Carrusel (</>) para elegir clase; aparece justo despues de elegir raza. No se cierra con ESC: hay que elegir. */
public class ClassSelectionScreen extends Screen {
    private final List<PlayerClass> classes = new ArrayList<>();
    private int currentIndex = 0;

    public ClassSelectionScreen() {
        super(Component.literal("Elige tu Clase"));
        for (PlayerClass playerClass : PlayerClass.values()) {
            if (playerClass != PlayerClass.NINGUNA) {
                classes.add(playerClass);
            }
        }
    }

    private static String describe(PlayerClass playerClass) {
        return switch (playerClass) {
            case RITUALISTA_ARCANO -> "Magia y hechizos. -10% de daño físico y -20% de vida hasta el Matrimonio de Carne. Límite de destreza 50.";
            case BERSERKER -> "+25% de daño cuerpo a cuerpo, crítico y daño crítico. Habilidad: Furia (te desangra a cambio de +50% de daño).";
            case GUERRERO_ANIMA -> "Solo usa su espada ánima, que evoluciona. +10% velocidad de ataque, daño, crítico y vida. Habilidad: Espíritu Ánima.";
            case ESCUDERO -> "Recibe 20% menos de todo el daño y +20% de vida. Habilidad: Guardia Total (invulnerable 10 s).";
            case ARQUERO -> "Con arco y ballesta: +100% de daño, +50% crítico y daño crítico, +10% velocidad. Habilidad: Ojo de Halcón.";
            default -> "";
        };
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.addRenderableWidget(Button.builder(Component.literal("<"),
                button -> currentIndex = (currentIndex - 1 + classes.size()) % classes.size())
                .bounds(centerX - 145, centerY, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"),
                button -> currentIndex = (currentIndex + 1) % classes.size())
                .bounds(centerX + 125, centerY, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Seleccionar"), button -> {
            Networking.sendToServer(new ChooseClassPacket(classes.get(currentIndex)));
            this.onClose();
        }).bounds(centerX - 50, centerY + 60, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        PlayerClass current = classes.get(currentIndex);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFD700);
        g.drawCenteredString(this.font, Component.literal(current.getDisplayName()), this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        int y = this.height / 2 + 4;
        for (var line : this.font.split(Component.literal(describe(current)), 240)) {
            g.drawCenteredString(this.font, line, this.width / 2, y, 0xBBBBBB);
            y += 10;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
