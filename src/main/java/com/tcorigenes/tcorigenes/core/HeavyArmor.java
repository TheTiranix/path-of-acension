// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Fuerza y armaduras pesadas (pedido de alejandr0). Las armaduras de metal, roca y gemas piden 80 de fuerza; las de cuero, escamas, piel, cota
 * de malla y tela no piden nada. Con menos de 80 de fuerza se pueden usar, pero con una penalizacion ABSOLUTA (igual que los debuff de raza,
 * ver OriginBonuses) a la velocidad de movimiento y de ataque: -100% con 0 de fuerza, bajando en linea recta hasta -20% con 79.
 * Fuerza base: Arquero 80; Ritualista Arcano 50, y hasta 79 una vez que consume el Matrimonio de Carne; el resto de las clases 100.
 */
public final class HeavyArmor {
    public static final int REQUIREMENT = 80;

    /** Prefijos (modid:nombre_) de las armaduras pesadas. Todo lo que no figura aca (cuero, escamas, piel, cota de malla, tela) no pide fuerza. */
    private static final String[] HEAVY_PREFIXES = {
            "minecraft:iron_", "minecraft:golden_", "minecraft:diamond_", "minecraft:netherite_",
            "aether:gravitite_", "aether:zanite_", "aether:valkyrie_", "aether:phoenix_", "aether:neptune_", "aether:obsidian_",
            "born_in_chaos_v1:dark_metal_armor_",
            "testamentodelacarne:sapphire_", "testamentodelacarne:wintry_", "testamentodelacarne:arcane_", "testamentodelacarne:black_steel_",
            "testamentodelacarne:diascite_", "testamentodelacarne:wrought_",
            "twilightforest:fiery_", "twilightforest:knightmetal_", "twilightforest:steeleaf_", "twilightforest:ironwood_", "twilightforest:phantom_",
            "iceandfire:dragonsteel_", "iceandfire:silver_metal_", "iceandfire:copper_metal_",
            "cataclysm:cursium_", "cataclysm:ignitium_", "mekanismtools:", "mekanism:mekasuit_",
            "celestisynth:solar_crystal_", "celestisynth:lunar_stone_", "eeeabsmobs:ghost_warrior_",
            "alexscaves:diving_", "seadwellers:depth_", "mowziesmobs:wrought_"};

    private HeavyArmor() {
    }

    public static boolean isHeavy(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        String s = id.toString();
        for (String prefix : HEAVY_PREFIXES) {
            if (s.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static int strengthOf(Player player) {
        PlayerClass cls = player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass()).orElse(PlayerClass.NINGUNA);
        return switch (cls) {
            case ARQUERO -> 80;
            case RITUALISTA_ARCANO -> player.getPersistentData().getBoolean("matrimonio_consagrado") ? 79 : 50;
            default -> 100;
        };
    }

    /** 0 sin armadura pesada o con fuerza suficiente; si no, de 1.0 (0 de fuerza) a 0.2 (79 de fuerza). */
    public static double penalty(Player player) {
        boolean heavy = false;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (isHeavy(player.getItemBySlot(slot))) {
                heavy = true;
                break;
            }
        }
        int strength = strengthOf(player);
        if (!heavy || strength >= REQUIREMENT) {
            return 0.0;
        }
        return 1.0 - 0.8 * Math.max(0, strength) / 79.0;
    }
}
