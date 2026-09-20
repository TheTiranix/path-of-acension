// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.client.gui;

import com.tudominio.testamentodelacarne.networking.packet.ChoosePactPacket;
import com.tudominio.testamentodelacarne.networking.packet.Networking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * GUI de "Forja tu Destino": el jugador elige entre Pacto de Sangre (vida -> lifesteal)
 * o Dogma de Acero (niveles de experiencia -> fuerza), ver ChoosePactPacket.
 */
public class PactSelectionScreen extends Screen {
    public PactSelectionScreen() {
        super(Component.literal("Forja tu Destino"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Pacto de Sangre"), button -> {
            Networking.sendToServer(new ChoosePactPacket("sangre"));
            this.onClose();
        }).bounds(centerX - 100, centerY - 30, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Dogma de Acero"), button -> {
            Networking.sendToServer(new ChoosePactPacket("acero"));
            this.onClose();
        }).bounds(centerX - 100, centerY, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 40, 0xFFFFFF);
    }
}
