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
}
