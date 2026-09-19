package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.ChooseRacePacket;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Carrusel simple (</>) para elegir una de las 8 razas; consume 1 Orbe de Orígenes al confirmar. */
public class RaceSelectionScreen extends Screen {
    private final List<Race> availableRaces = List.of(Race.values());
    private int currentIndex = 0;
    private final InteractionHand hand;

    public RaceSelectionScreen(InteractionHand hand) {
        super(Component.literal("Elige tu Origen"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal("<"),
                        button -> this.currentIndex = (this.currentIndex - 1 + this.availableRaces.size()) % this.availableRaces.size())
                .bounds(centerX - 125, centerY, 20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(">"),
                        button -> this.currentIndex = (this.currentIndex + 1) % this.availableRaces.size())
                .bounds(centerX + 105, centerY, 20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Seleccionar"), button -> {
            Networking.sendToServer(new ChooseRacePacket(this.availableRaces.get(this.currentIndex)));
            this.onClose();
        }).bounds(centerX - 50, centerY + 50, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        Race currentRace = this.availableRaces.get(this.currentIndex);
        guiGraphics.drawCenteredString(this.font, Component.literal(currentRace.getDisplayName()), this.width / 2, this.height / 2 - 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
