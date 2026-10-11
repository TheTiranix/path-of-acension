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
 * Cota de malla de dark metal (pedido de alejandr0): cada pieza protege 0.5 menos que la armadura de dark metal y se hace con cadenas de
 * dark metal. Los valores finales (fraccionarios) los fija WeaponBalance#ARMOR_FIXED; aca estan los enteros que pide el material.
 */
public final class DarkChainmailArmorMaterial implements ArmorMaterial {
    public static final DarkChainmailArmorMaterial INSTANCE = new DarkChainmailArmorMaterial();
    private static final int[] DURABILITY = {363 * 2, 528 * 2, 495 * 2, 429 * 2}; // helmet, chestplate, leggings, boots
    private static final int[] DEFENSE = {4, 12, 9, 4};

    private DarkChainmailArmorMaterial() {
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
        return SoundEvents.ARMOR_EQUIP_CHAIN;
    }

    @Override
    public Ingredient getRepairIngredient() {
        var ingot = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("born_in_chaos_v1", "dark_metal_ingot"));
        return ingot == null ? Ingredient.EMPTY : Ingredient.of(ingot);
    }

    @Override
    public String getName() {
        return "testamentodelacarne:dark_chainmail";
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
