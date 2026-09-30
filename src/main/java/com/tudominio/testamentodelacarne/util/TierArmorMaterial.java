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
 * Armaduras de arcane y de black steel (pedido de alejandr0): 4 veces la proteccion del diamante (12 / 24 / 32 / 12), 8 de tenacidad por
 * pieza y 60000 usos cada una. Los valores finales los fija WeaponBalance#ARMOR_FIXED; aca estan los mismos para el material.
 */
public final class TierArmorMaterial implements ArmorMaterial {
    public static final TierArmorMaterial ARCANE = new TierArmorMaterial("testamentodelacarne:arcane", "irons_spellbooks:arcane_salvage");
    public static final TierArmorMaterial BLACK_STEEL = new TierArmorMaterial("testamentodelacarne:black_steel", "cataclysm:black_steel_ingot");
    private static final int[] DURABILITY = {60000, 60000, 60000, 60000}; // boots, leggings, chestplate, helmet
    private static final int[] DEFENSE = {12, 24, 32, 12};

    private final String name;
    private final String repairItem;

    private TierArmorMaterial(String name, String repairItem) {
        this.name = name;
        this.repairItem = repairItem;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY[type.ordinal()];
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENSE[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return 25;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(this.repairItem));
        return item == null ? Ingredient.EMPTY : Ingredient.of(item);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public float getToughness() {
        return 8.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
