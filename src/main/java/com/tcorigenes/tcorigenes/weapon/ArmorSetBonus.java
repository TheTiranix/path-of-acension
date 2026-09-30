// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import com.tudominio.elementaldamage.ModDamageTypes;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Bonus de set completo: con las 4 piezas puestas, el daño de un elemento se amplifica 20% (Neptune: agua,
 * Phoenix: fuego, Valkyrie: luz). Su proteccion es la del diamante (ver WeaponBalance#ARMOR_LIKE).
 */
public final class ArmorSetBonus {
    public static final double AMPLIFICATION = 0.20;
    /** prefijo de las piezas -> elemento amplificado */
    public static final Map<String, ResourceKey<DamageType>> SETS = Map.of(
            "aether:neptune_", ModDamageTypes.WATER_ELEMENTAL,
            "aether:phoenix_", ModDamageTypes.FIRE_ELEMENTAL,
            "aether:valkyrie_", ModDamageTypes.LIGHT);

    /** Armaduras divinas (Celestisynth): +2.5% de daño elemental por pieza. */
    public static final double PIECE_BONUS = 0.025;
    public static final Map<String, ResourceKey<DamageType>> PIECE_SETS = Map.of(
            "celestisynth:solar_crystal_", ModDamageTypes.FIRE_ELEMENTAL,
            "celestisynth:lunar_stone_", ModDamageTypes.LUNAR);

    private ArmorSetBonus() {
    }

    private static boolean hasFullSet(Player player, String prefix) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getItemBySlot(slot).getItem());
            if (id == null || !id.toString().startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    /** 1.0 si no hay bonus para ese elemento; 1.2 con el set completo correspondiente; mas 2.5% por pieza divina. */
    public static double multiplier(Player player, ResourceKey<DamageType> element) {
        double result = 1.0;
        for (var entry : SETS.entrySet()) {
            if (entry.getValue().equals(element) && hasFullSet(player, entry.getKey())) {
                result = 1.0 + AMPLIFICATION;
            }
        }
        for (var entry : PIECE_SETS.entrySet()) {
            if (entry.getValue().equals(element)) {
                int pieces = 0;
                for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getItemBySlot(slot).getItem());
                    if (id != null && id.toString().startsWith(entry.getKey())) {
                        pieces++;
                    }
                }
                result += PIECE_BONUS * pieces;
            }
        }
        return result;
    }
}
