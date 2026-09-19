package com.tcorigenes.tcorigenes.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/** Dos botones en la esquina del inventario que abren en JEI las listas ordenadas de daño y de armadura. */
public final class RankingButtons {
    private RankingButtons() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) || !ModList.get().isLoaded("jei")) {
            return;
        }
        event.addListener(Button.builder(Component.literal("Daño"),
                button -> com.tcorigenes.tcorigenes.compat.jei.ModJeiPlugin.showDamage()).bounds(4, 4, 52, 16).build());
        event.addListener(Button.builder(Component.literal("Armadura"),
                button -> com.tcorigenes.tcorigenes.compat.jei.ModJeiPlugin.showArmor()).bounds(4, 22, 52, 16).build());
    }
}
