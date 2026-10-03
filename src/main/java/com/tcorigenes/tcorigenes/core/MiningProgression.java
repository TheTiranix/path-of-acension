// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Progresion de herramientas (pedido de alejandr0): el pico de dark metal mina un nivel por encima del diamante, el de fiery uno
 * mas, y el Arcane Debris (de donde sale el arcane salvage) solo se puede minar con un pico de fiery o uno mejor: arcane, black
 * steel, dragonsteel, Void Forge o Infernal Forge. Con cualquier otra herramienta el bloque no suelta nada, aunque sea de netherite.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class MiningProgression {
    private static final ResourceLocation ARCANE_DEBRIS = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_debris");
    private static final Set<String> ARCANE_DEBRIS_TOOLS = Set.of(
            "twilightforest:fiery_pickaxe", "testamentodelacarne:wintry_pickaxe",
            "testamentodelacarne:arcane_pickaxe",
            "cataclysm:black_steel_pickaxe",
            "iceandfire:dragonsteel_fire_pickaxe", "iceandfire:dragonsteel_ice_pickaxe", "iceandfire:dragonsteel_lightning_pickaxe",
            "cataclysm:void_forge", "cataclysm:infernal_forge");

    private MiningProgression() {
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!ARCANE_DEBRIS.equals(ForgeRegistries.BLOCKS.getKey(event.getTargetBlock().getBlock()))) {
            return;
        }
        ItemStack tool = event.getEntity().getMainHandItem();
        ResourceLocation id = tool.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(tool.getItem());
        event.setCanHarvest(id != null && ARCANE_DEBRIS_TOOLS.contains(id.toString()));
    }
}
