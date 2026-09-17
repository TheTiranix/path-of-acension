package com.tcorigenes.tcorigenes.compat.jei;

import net.minecraft.world.item.ItemStack;

/** Un item + el valor (daño o armadura) por el que se ordena la lista (ver ModJeiPlugin). */
public record StatEntry(ItemStack stack, float value) {
}
