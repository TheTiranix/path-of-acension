// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.TogglePacifistPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Pedido de alejandr0: boton a la izquierda del inventario que alterna el modo pacifico (incapaz de hacer daño, ver
 * PacifistMode). El estado lo manda el servidor.
 */
public final class PacifistButton {
    public static final int WIDTH = 66;
    public static final int HEIGHT = 20;
    /** Estado que mando el servidor (PacifistSyncPacket). */
    public static boolean active;

    private PacifistButton() {
    }

    private static Component label() {
        return Component.literal(active ? "Pacífico: Sí" : "Pacífico: No");
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof CreativeModeInventoryScreen)) {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        int left = screen.getGuiLeft();
        // con el libro de recetas abierto ocupa ~147 px a la izquierda del inventario
        if (screen instanceof InventoryScreen inventory && inventory.getRecipeBookComponent().isVisible()) {
            left -= 150;
        }
        int x = Math.max(2, left - WIDTH - 4);
        event.addListener(Button.builder(label(), button -> {
            Networking.sendToServer(new TogglePacifistPacket());
            // el servidor contesta con el estado real; mientras tanto se adelanta para que se vea el cambio
            active = !active;
            button.setMessage(label());
        }).bounds(x, screen.getGuiTop() + 4, WIDTH, HEIGHT).build());
    }
}
