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
 * Armadura de diascite (Sculk Horde), a pedido de alejandr0: al nivel del dark metal de Born in Chaos (4 casco y botas,
 * 9 pechera, 7 pantalones, 3 de tenacidad). La durabilidad es la proporcional de WeaponBalance#scaledDurability (que
 * tambien la aplica ArmorDurability sobre este item).
 */
public final class DiasciteArmorMaterial implements ArmorMaterial {
    public static final DiasciteArmorMaterial INSTANCE = new DiasciteArmorMaterial();
    private static final int[] DURABILITY = {1050, 1154, 1200, 1050}; // boots, leggings, chestplate, helmet
    private static final int[] DEFENSE = {4, 7, 9, 4};

    private DiasciteArmorMaterial() {
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
        return 12;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        var ingot = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("sculkhorde", "diascite"));
        return ingot == null ? Ingredient.EMPTY : Ingredient.of(ingot);
    }

    @Override
    public String getName() {
        return "testamentodelacarne:diascite";
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
