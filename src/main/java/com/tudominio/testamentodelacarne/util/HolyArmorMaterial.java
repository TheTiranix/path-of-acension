// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Tunicas sagradas (pedido de alejandr0): proteccion exacta del diamante, 3 de tenacidad por pieza y la durabilidad del dark metal; el set
 * completo da +50% de poder de hechizos de luz (ver SetSpellPower). Los valores finales los fija WeaponBalance#ARMOR_FIXED.
 */
public final class HolyArmorMaterial implements ArmorMaterial {
    public static final HolyArmorMaterial INSTANCE = new HolyArmorMaterial();
    private static final int[] DEFENSE = {3, 8, 6, 3}; // helmet, chestplate, leggings, boots

    private HolyArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 500;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENSE[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return 20;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.GOLD_INGOT);
    }

    @Override
    public String getName() {
        return "testamentodelacarne:holy";
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
