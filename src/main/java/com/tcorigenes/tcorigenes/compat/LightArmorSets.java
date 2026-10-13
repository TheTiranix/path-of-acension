// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import com.tcorigenes.tcorigenes.core.HeavyArmor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: apartado del orden de armaduras con solo las armaduras LIVIANAS para mago (cuero, tela, cota de malla, escamas,
 * piel...: todas las que no piden fuerza, ver HeavyArmor), agrupadas por SET ENTERO (casco, pechera, pantalones y botas del mismo set) y
 * ordenadas por la proteccion total del set. Los sets se arman por el nombre del item sin la pieza ("naga_chestplate" + "naga_helmet" =
 * set "naga"), aunque las piezas vengan de mods distintos (el casco y las botas de naga son nuestros).
 */
public final class LightArmorSets {
    private static final String[][] PIECE_SUFFIXES = {
            {"_helmet", "_hat", "_hood", "_cap", "_crown", "_mask", "_head"},
            {"_chestplate", "_tunic", "_robe", "_robes", "_chest", "_cloak", "_jacket", "_body", "_vest", "_gown"},
            {"_leggings", "_pants", "_trousers", "_legs", "_skirt"},
            {"_boots", "_shoes", "_feet", "_sandals"}};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Espacios de nombres de armaduras que no son de aventura / magia (trajes de proteccion, buceo, etc.). */
    private static final java.util.Set<String> EXCLUDED_NAMESPACES = java.util.Set.of("create", "mekanism", "mekanismtools");

    /** Un set: nombre legible, sus piezas (null si el set no tiene esa pieza) y la proteccion total. */
    public record ArmorSet(String name, ItemStack[] pieces, float total) {
    }

    private static List<ArmorSet> cache;

    private LightArmorSets() {
    }

    public static synchronized List<ArmorSet> sets() {
        if (cache == null) {
            cache = compute();
        }
        return cache;
    }

    private static int slotIndexOf(String path) {
        for (int i = 0; i < PIECE_SUFFIXES.length; i++) {
            for (String suffix : PIECE_SUFFIXES[i]) {
                if (path.endsWith(suffix)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String stripSuffix(String path) {
        for (String[] group : PIECE_SUFFIXES) {
            for (String suffix : group) {
                if (path.endsWith(suffix)) {
                    return path.substring(0, path.length() - suffix.length());
                }
            }
        }
        return path;
    }

    private static List<ArmorSet> compute() {
        Map<String, ItemStack[]> sets = new LinkedHashMap<>();
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof ArmorItem armor)) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || stack.isEmpty() || EXCLUDED_NAMESPACES.contains(id.getNamespace())
                    || com.tcorigenes.tcorigenes.weapon.RemovedItems.isRemoved(stack) || HeavyArmor.isHeavy(stack)) {
                continue;
            }
            int index = switch (armor.getType()) {
                case HELMET -> 0;
                case CHESTPLATE -> 1;
                case LEGGINGS -> 2;
                case BOOTS -> 3;
            };
            String path = id.getPath();
            int fromName = slotIndexOf(path);
            if (fromName >= 0 && fromName != index) {
                continue; // nombre y ranura no coinciden: no se arriesga a mezclar sets
            }
            String key = stripSuffix(path);
            ItemStack[] set = sets.get(key);
            if (set != null && set[index] != null && !set[index].is(item)) {
                key = id.getNamespace() + ":" + key; // mismo nombre de set en dos mods con la misma pieza: sets aparte
                set = sets.get(key);
            }
            if (set == null) {
                set = new ItemStack[4];
                sets.put(key, set);
            }
            set[index] = stack;
        }
        List<ArmorSet> result = new ArrayList<>();
        for (var entry : sets.entrySet()) {
            ItemStack[] pieces = entry.getValue();
            int count = 0;
            float total = 0.0F;
            for (int i = 0; i < 4; i++) {
                if (pieces[i] != null) {
                    count++;
                    try {
                        total += ItemStatRanking.sumModifiers(pieces[i], SLOTS[i], Attributes.ARMOR);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (count >= 2 && total > 0.0F) {
                result.add(new ArmorSet(prettify(entry.getKey()), pieces, total));
            }
        }
        result.sort(Comparator.comparingDouble(ArmorSet::total));
        org.slf4j.LoggerFactory.getLogger(LightArmorSets.class).info("[tcorigenes] Sets livianos para magos: {}", result.size());
        return result;
    }

    private static String prettify(String key) {
        String name = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        StringBuilder sb = new StringBuilder();
        for (String word : name.split("_")) {
            if (!word.isEmpty()) {
                sb.append(sb.length() > 0 ? " " : "").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
