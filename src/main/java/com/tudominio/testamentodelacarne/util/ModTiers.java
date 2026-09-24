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
