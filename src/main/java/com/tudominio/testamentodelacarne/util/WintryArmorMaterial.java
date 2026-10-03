// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Armadura wintry (pedido de alejandr0, "como fiery pero fria"): 2.5 veces la proteccion del diamante, 5 de tenacidad y 2500 usos por pieza.
 * Los valores finales los fija WeaponBalance#ARMOR_FIXED; aca estan los mismos (enteros) para el material.
 */
public final class WintryArmorMaterial implements ArmorMaterial {
    public static final WintryArmorMaterial INSTANCE = new WintryArmorMaterial();
    private static final int[] DEFENSE = {8, 15, 20, 8}; // boots, leggings, chestplate, helmet (diamante 3/6/8/3 x 2.5, redondeado)

    private WintryArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 2500;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENSE[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        var gem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "wintry_gem"));
        return gem == null ? Ingredient.EMPTY : Ingredient.of(gem);
    }

    @Override
    public String getName() {
        return "testamentodelacarne:wintry";
    }

    @Override
    public float getToughness() {
        return 5.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
