// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.crafting.client;

import com.tudominio.testamentodelacarne.crafting.ModCrafting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "testamentodelacarne", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CraftingClientSetup {
    private CraftingClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModCrafting.TIER3_MENU.get(), Tier3CraftingScreen::new));
    }
}
