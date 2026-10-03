// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.util;

import com.tudominio.testamentodelacarne.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.ForgeTier;

public class ModTiers {
    // level=5, uses=0 (irrompible via AnimaSwordItem#isDamageable=false), speed=10, damage=0 (el dano real
    // se aplica via AttributeModifier en AnimaSwordItem), enchantability=25, repair con Engranaje Arcano.
    public static final ForgeTier ANIMA_TIER = new ForgeTier(
            5, 0, 10.0F, 0.0F, 25, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of((ItemLike) ModItems.ENGRANAJE_ARCANO.get())
    );

    /** Herramientas y espada de arcane (pedido de alejandr0): 100000 usos, se reparan con arcane salvage. */
    public static final ForgeTier ARCANE_TIER = new ForgeTier(
            5, 6062, 14.0F, 5.0F, 25, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_salvage")))
    );

    /** Espada de diascite: al nivel del dark metal (1800 usos, +3 de daño base), se repara con el diascite de Sculk Horde. */
    public static final ForgeTier DIASCITE_TIER = new ForgeTier(
            4, 1800, 8.5F, 3.0F, 12, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sculkhorde", "diascite")))
    );

    /** Herramientas de zafiro (pedido de alejandr0): como el diamante (nivel 3, 8 de velocidad, +3 de daño) con 100 usos
     *  mas (1561 + 100); se reparan con el sapphire_gem de Ice and Fire. */
    public static final ForgeTier SAPPHIRE_TIER = new ForgeTier(
            3, 1661, 8.0F, 3.0F, 10, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("iceandfire", "sapphire_gem")))
    );

    /** Herramientas wintry (pedido de alejandr0): "como fiery pero frio", 1000 usos mas que netherite (3031); se reparan con la wintry gem. */
    public static final ForgeTier WINTRY_TIER = new ForgeTier(
            4, 3031, 9.0F, 4.0F, 15, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "wintry_gem")))
    );

    /** Pico de Dark Metal: nivel por encima del diamante (se registra DESPUES del netherite en
     *  TierSortingRegistry, ver TestamentoDeLaCarne#commonSetup), un poco mas rapido y duradero que el diamante. */
    public static final ForgeTier DARK_METAL_TIER = new ForgeTier(
            5, 1800, 8.5F, 3.0F, 12,
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tcorigenes", "needs_dark_metal_tool")),
            () -> Ingredient.of(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("born_in_chaos_v1", "dark_metal_ingot")))
    );
}
