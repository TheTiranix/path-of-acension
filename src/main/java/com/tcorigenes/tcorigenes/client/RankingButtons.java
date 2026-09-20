// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/**
 * Dos botones pegados al borde derecho de la ventana del inventario (supervivencia y creativo)
 * que abren en JEI las listas ordenadas de daño y de armadura. Van ahi y no en la esquina de la
 * pantalla porque esa la usan los botones de FTB Quests / FTB Teams. La zona se registra en JEI
 * (ver ModJeiPlugin) para que su lista de items no los tape.
 */
public final class RankingButtons {
    public static final int WIDTH = 52;
    public static final int HEIGHT = 16;
    private static final int GAP = 4;

    private RankingButtons() {
    }

    public static boolean supports(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }

    /** Rectangulo que ocupan los dos botones apilados, para reservarlo en JEI. */
    public static Rect2i area(AbstractContainerScreen<?> screen) {
        return new Rect2i(screen.getGuiLeft() + screen.getXSize() + GAP, screen.getGuiTop() + 4, WIDTH, HEIGHT * 2 + 4);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!supports(event.getScreen()) || !ModList.get().isLoaded("jei")) {
            return;
        }
        Rect2i area = area((AbstractContainerScreen<?>) event.getScreen());
        event.addListener(Button.builder(Component.literal("Daño"),
                button -> com.tcorigenes.tcorigenes.compat.jei.ModJeiPlugin.showDamage())
                .bounds(area.getX(), area.getY(), WIDTH, HEIGHT).build());
        event.addListener(Button.builder(Component.literal("Armadura"),
                button -> com.tcorigenes.tcorigenes.compat.jei.ModJeiPlugin.showArmor())
                .bounds(area.getX(), area.getY() + HEIGHT + 4, WIDTH, HEIGHT).build());
    }
}
