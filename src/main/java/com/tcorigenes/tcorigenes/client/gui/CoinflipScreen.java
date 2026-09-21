// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.ChooseCoinflipPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Cara o cruz al abrir un cofre de botin por primera vez. El jugador elige, el servidor tira la moneda y esta
 * pantalla solo la anima; cuando termina, el propio servidor abre el cofre (y esta pantalla se reemplaza).
 */
public class CoinflipScreen extends Screen {
    private static final ResourceLocation HEADS = new ResourceLocation("tcorigenes", "textures/gui/coinflip/coin_heads.png");
    private static final ResourceLocation TAILS = new ResourceLocation("tcorigenes", "textures/gui/coinflip/coin_tails.png");
    private static final int FLIP_TICKS = 44;
    private static final int COIN = 96;

    private enum Phase { CHOOSE, WAITING, FLIPPING, RESULT }

    private Phase phase = Phase.CHOOSE;
    private boolean choseHeads;
    private boolean landedHeads;
    private boolean won;
    private int flipAge = 0;
    private int idleAge = 0;
    private int resultAge = 0;
    private Button closeButton;
    private Button headsButton;
    private Button tailsButton;

    public CoinflipScreen() {
        super(Component.literal("Cara o Cruz"));
    }

    /** Llamado desde CoinflipResultPacket con el resultado que decidio el servidor. */
    public static void onResult(boolean landedHeads, boolean won) {
        if (Minecraft.getInstance().screen instanceof CoinflipScreen screen) {
            screen.landedHeads = landedHeads;
            screen.won = won;
            screen.phase = Phase.FLIPPING;
            screen.flipAge = 0;
        }
    }

    /** Cierra la pantalla si sigue abierta (el servidor avisa cuando el cofre no llego a abrirse). */
    public static void closeIfOpen() {
        if (Minecraft.getInstance().screen instanceof CoinflipScreen screen) {
            screen.onClose();
        }
    }

    /** Salida de emergencia: solo si el servidor no abrio el cofre a tiempo (antes la pantalla podia quedar colgada). */
    private boolean canLeave() {
        return (this.phase == Phase.RESULT && this.resultAge >= 60) || this.idleAge >= 400;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 62;
        this.headsButton = this.addRenderableWidget(Button.builder(Component.literal("Cara"), b -> choose(true))
                .bounds(cx - 105, y, 100, 20).build());
        this.tailsButton = this.addRenderableWidget(Button.builder(Component.literal("Cruz"), b -> choose(false))
                .bounds(cx + 5, y, 100, 20).build());
        this.closeButton = this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(cx - 50, y, 100, 20).build());
        this.closeButton.visible = false;
    }

    private void choose(boolean heads) {
        if (this.phase != Phase.CHOOSE) {
            return;
        }
        this.choseHeads = heads;
        this.phase = Phase.WAITING;
        Networking.sendToServer(new ChooseCoinflipPacket(heads));
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F);
    }

    @Override
    public void tick() {
        this.idleAge++;
        boolean choosing = this.phase == Phase.CHOOSE;
        this.headsButton.visible = choosing;
        this.tailsButton.visible = choosing;
        if (this.phase == Phase.RESULT) {
            this.resultAge++;
        }
        this.closeButton.visible = canLeave();
        if (this.phase == Phase.RESULT && this.resultAge >= 200) {
            this.onClose(); // el cofre no se abrio: no dejar la pantalla colgada
        }
        if (this.phase == Phase.FLIPPING) {
            this.flipAge++;
            if (this.flipAge % 5 == 0 && this.flipAge < FLIP_TICKS) {
                play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F + (this.flipAge % 10) * 0.05F);
            }
            if (this.flipAge >= FLIP_TICKS) {
                this.phase = Phase.RESULT;
                if (this.won) {
                    play(SoundEvents.PLAYER_LEVELUP, 1.0F);
                } else {
                    play(SoundEvents.WARDEN_HEARTBEAT, 0.8F);
                }
            }
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.fill(0, 0, this.width, this.height, 0x99000000);
        int cx = this.width / 2;
        int cy = this.height / 2 - 20;

        g.drawCenteredString(this.font, Component.literal("CARA O CRUZ"), cx, cy - 92, 0xD4AF37);
        g.drawCenteredString(this.font, Component.literal("El cofre exige una apuesta: lo mejor... o nada."), cx, cy - 78, 0x998888);

        // moneda
        float scaleX = 1.0F;
        float lift = 0.0F;
        boolean showHeads = true;
        if (this.phase == Phase.CHOOSE || this.phase == Phase.WAITING) {
            lift = (float) Math.sin((this.idleAge + partialTick) * 0.12) * 3.0F; // flota apenas
        } else {
            float p = Math.min(1.0F, (this.flipAge + partialTick) / FLIP_TICKS);
            int halfTurns = 6 + (this.landedHeads ? 0 : 1);
            float eased = 1.0F - (1.0F - p) * (1.0F - p);
            double angle = Math.PI * halfTurns * eased;
            scaleX = (float) Math.abs(Math.cos(angle));
            showHeads = ((int) (halfTurns * eased)) % 2 == 0;
            lift = (float) (-58.0 * Math.sin(Math.PI * p));
            if (this.phase == Phase.RESULT) {
                scaleX = 1.0F;
                lift = 0.0F;
                showHeads = this.landedHeads;
            }
        }
        g.pose().pushPose();
        g.pose().translate(cx, cy + lift, 0);
        g.pose().scale(Math.max(0.04F, scaleX), 1.0F, 1.0F);
        g.blit(showHeads ? HEADS : TAILS, -COIN / 2, -COIN / 2, COIN, COIN, 0, 0, 32, 32, 32, 32);
        g.pose().popPose();

        // sombra bajo la moneda
        g.fill(cx - 30, cy + COIN / 2 + 8, cx + 30, cy + COIN / 2 + 10, 0x66000000);

        switch (this.phase) {
            case CHOOSE -> g.drawCenteredString(this.font, Component.literal("¿Cara o cruz?"), cx, cy + COIN / 2 + 22, 0xFFFFFF);
            case WAITING -> g.drawCenteredString(this.font, Component.literal("Elegiste " + (this.choseHeads ? "CARA" : "CRUZ") + "..."),
                    cx, cy + COIN / 2 + 22, 0xAAAAAA);
            case FLIPPING -> g.drawCenteredString(this.font, Component.literal("Elegiste " + (this.choseHeads ? "CARA" : "CRUZ") + "..."),
                    cx, cy + COIN / 2 + 22, 0xAAAAAA);
            case RESULT -> {
                g.drawCenteredString(this.font, Component.literal("Salió " + (this.landedHeads ? "CARA" : "CRUZ")), cx, cy + COIN / 2 + 22, 0xFFFFFF);
                if (this.won) {
                    g.drawCenteredString(this.font, Component.literal("El destino te sonríe."), cx, cy + COIN / 2 + 36, 0x55FF55);
                } else {
                    g.drawCenteredString(this.font, Component.literal("El cofre no te dará nada."), cx, cy + COIN / 2 + 36, 0xFF4444);
                }
            }
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return canLeave();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
