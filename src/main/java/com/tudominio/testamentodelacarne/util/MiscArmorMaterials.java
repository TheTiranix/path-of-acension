// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Materiales de las piezas que completan sets de otros mods (pedido de alejandr0): la Naga Scale (Twilight Forest) que solo tenia
 * tunica y pantalones, con casco y botas iguales al diamante; y los pantalones y botas de la Oscuridad (Alex's Caves), que copian los
 * valores de la capa y la capucha.
 */
public final class MiscArmorMaterials {
    private MiscArmorMaterials() {
    }

    /** Naga Scale: igual que el diamante. */
    public static final ArmorMaterial NAGA = new ArmorMaterial() {
        private final int[] durability = {429, 495, 528, 363};
        private final int[] defense = {3, 6, 8, 3};

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return durability[type.ordinal()];
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return defense[type.ordinal()];
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
            var scale = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("twilightforest", "naga_scale"));
            return scale == null ? Ingredient.of(Items.DIAMOND) : Ingredient.of(scale);
        }

        @Override
        public String getName() {
            return "testamentodelacarne:naga";
        }

        @Override
        public float getToughness() {
            return 2.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    /** Oscuridad: toma lo que tengan la capa y la capucha de Alex's Caves (si no estan, valores de cuero). */
    public static final ArmorMaterial DARKNESS = new ArmorMaterial() {
        private ArmorMaterial source() {
            var cloak = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "cloak_of_darkness"));
            return cloak instanceof ArmorItem armor ? armor.getMaterial() : null;
        }

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            ArmorMaterial source = source();
            return source != null ? source.getDurabilityForType(type) : 200;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            ArmorMaterial source = source();
            return source != null ? source.getDefenseForType(type) : 2;
        }

        @Override
        public int getEnchantmentValue() {
            ArmorMaterial source = source();
            return source != null ? source.getEnchantmentValue() : 15;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            var silk = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves", "shadow_silk"));
            return silk == null ? Ingredient.EMPTY : Ingredient.of(silk);
        }

        @Override
        public String getName() {
            return "testamentodelacarne:darkness";
        }

        @Override
        public float getToughness() {
            ArmorMaterial source = source();
            return source != null ? source.getToughness() : 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            ArmorMaterial source = source();
            return source != null ? source.getKnockbackResistance() : 0.0F;
        }
    };
}
