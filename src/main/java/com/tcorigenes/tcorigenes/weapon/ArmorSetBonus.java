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
 * Bonus de set completo (pedido de alejandr0): con las 4 piezas puestas cada golpe suma daño elemental FIJO: Valkyrie +3 de luz y +3 natural,
 * Phoenix +3 de fuego y +3 lunar, Neptune +6 de agua. El golpe elemental sale aparte, igual que el de las armas (ver WeaponElemental).
 */
public final class ArmorSetBonus {
    public static final double AMPLIFICATION = 0.20;
    /** prefijo de las piezas -> elemento amplificado */
    public record SetBonus(String prefix, ResourceKey<DamageType> element, double amount, boolean inWaterOnly) {
        public SetBonus(String prefix, ResourceKey<DamageType> element, double amount) {
            this(prefix, element, amount, false);
        }
    }

    /** Daño elemental fijo por golpe con el set completo (ver arriba). */
    public static final java.util.List<SetBonus> SETS = java.util.List.of(
            new SetBonus("aether:neptune_", ModDamageTypes.WATER_ELEMENTAL, 6.0),
            new SetBonus("aether:phoenix_", ModDamageTypes.FIRE_ELEMENTAL, 3.0),
            new SetBonus("aether:phoenix_", ModDamageTypes.LUNAR, 3.0),
            new SetBonus("aether:valkyrie_", ModDamageTypes.LIGHT, 3.0),
            new SetBonus("aether:valkyrie_", ModDamageTypes.NATURAL, 3.0),
            // Depth (Sea Dwellers): +2 de agua solo dentro del agua
            new SetBonus("seadwellers:depth_", ModDamageTypes.WATER_ELEMENTAL, 2.0, true));

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

    /** Golpes elementales fijos que suma el set completo que lleva puesto el jugador (vacio si no lleva ninguno). */
    public static java.util.List<WeaponBalance.Extra> flatExtras(Player player) {
        java.util.List<WeaponBalance.Extra> extras = new java.util.ArrayList<>();
        for (SetBonus bonus : SETS) {
            if (hasFullSet(player, bonus.prefix()) && (!bonus.inWaterOnly() || player.isInWater() || player.isUnderWater())) {
                extras.add(new WeaponBalance.Extra(bonus.element(), (float) bonus.amount()));
            }
        }
        return extras;
    }

    /** 1.0 sin bonus; mas 2.5% por pieza divina (Solar Crystal / Lunar Stone). */
    public static double multiplier(Player player, ResourceKey<DamageType> element) {
        double result = 1.0;
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
