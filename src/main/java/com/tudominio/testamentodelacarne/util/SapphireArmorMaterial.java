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
 * Armadura de zafiro (pedido de alejandr0), crafteada con el sapphire_gem de Ice and Fire: 100 usos mas por pieza
 * que la de diamante (363/528/495/429 + 100) y 3 de tenacidad. La proteccion fraccionaria (3.5 / 8.5 / 6.5 / 3.5) se fija en
 * WeaponBalance#ARMOR_FIXED, porque el material solo admite enteros.
 */
public final class SapphireArmorMaterial implements ArmorMaterial {
    public static final SapphireArmorMaterial INSTANCE = new SapphireArmorMaterial();
    private static final int[] DURABILITY = {429 + 100, 495 + 100, 528 + 100, 363 + 100}; // boots, leggings, chestplate, helmet
    private static final int[] DEFENSE = {3, 6, 8, 3};

    private SapphireArmorMaterial() {
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
        return 10;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        var gem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("iceandfire", "sapphire_gem"));
        return gem == null ? Ingredient.EMPTY : Ingredient.of(gem);
    }

    @Override
    public String getName() {
        return "testamentodelacarne:sapphire";
    }

    @Override
    public float getToughness() {
        return 3.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
