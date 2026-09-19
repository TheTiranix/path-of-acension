package com.tcorigenes.tcorigenes.compat;

import com.google.common.collect.Multimap;
import com.tcorigenes.tcorigenes.compat.jei.StatEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Escanea TODOS los items del pack por daño/armadura y los ordena de menor a mayor, una sola
 * vez (se cachea). Esto NO depende de JEI: lo usan tanto ModJeiPlugin (para las categorias de
 * JEI) como el comando /rankings (para poder verlo en el juego sin abrir JEI).
 */
public final class ItemStatRanking {
    private static List<StatEntry> damageCache;
    private static List<StatEntry> armorCache;
    private static final java.util.EnumMap<ArmorCategory, List<StatEntry>> CATEGORY_CACHE = new java.util.EnumMap<>(ArmorCategory.class);

    /** Categorias en que se separa la lista de armadura (el orden es el de las pestañas de JEI). */
    public enum ArmorCategory {
        HELMET("Cascos"), CHESTPLATE("Pecheras"), LEGGINGS("Pantalones"), BOOTS("Botas"), OTHER("Otros");

        private final String label;

        ArmorCategory(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private ItemStatRanking() {
    }

    public static synchronized List<StatEntry> damageRanking() {
        ensureComputed();
        return damageCache;
    }

    public static synchronized List<StatEntry> armorRanking() {
        ensureComputed();
        return armorCache;
    }

    public static synchronized List<StatEntry> armorRanking(ArmorCategory category) {
        ensureComputed();
        return CATEGORY_CACHE.get(category);
    }

    private static void ensureComputed() {
        if (damageCache != null) {
            return;
        }
        List<StatEntry> damage = new ArrayList<>();
        List<StatEntry> armor = new ArrayList<>();
        java.util.EnumMap<ArmorCategory, List<StatEntry>> byCategory = new java.util.EnumMap<>(ArmorCategory.class);
        for (ArmorCategory category : ArmorCategory.values()) {
            byCategory.put(category, new ArrayList<>());
        }
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            // Con miles de items de ~180 mods, alguno raro puede tirar excepcion al calcular sus
            // modifiers; se aisla item por item para que uno roto no tire abajo toda la lista.
            try {
                ItemStack stack = new ItemStack(item);
                if (stack.isEmpty()) {
                    continue;
                }
                float dmg = sumModifiers(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
                if (dmg > 0.0F) {
                    damage.add(new StatEntry(stack, dmg));
                }
                float arm = sumModifiers(stack, EquipmentSlot.HEAD, Attributes.ARMOR)
                        + sumModifiers(stack, EquipmentSlot.CHEST, Attributes.ARMOR)
                        + sumModifiers(stack, EquipmentSlot.LEGS, Attributes.ARMOR)
                        + sumModifiers(stack, EquipmentSlot.FEET, Attributes.ARMOR);
                if (arm > 0.0F) {
                    armor.add(new StatEntry(stack, arm));
                }
                addToCategory(byCategory, ArmorCategory.HELMET, stack, sumModifiers(stack, EquipmentSlot.HEAD, Attributes.ARMOR));
                addToCategory(byCategory, ArmorCategory.CHESTPLATE, stack, sumModifiers(stack, EquipmentSlot.CHEST, Attributes.ARMOR));
                addToCategory(byCategory, ArmorCategory.LEGGINGS, stack, sumModifiers(stack, EquipmentSlot.LEGS, Attributes.ARMOR));
                addToCategory(byCategory, ArmorCategory.BOOTS, stack, sumModifiers(stack, EquipmentSlot.FEET, Attributes.ARMOR));
                // Otros: lo que da armadura pero en la mano (escudos de mods, etc.), fuera de las 4 piezas.
                addToCategory(byCategory, ArmorCategory.OTHER, stack,
                        sumModifiers(stack, EquipmentSlot.MAINHAND, Attributes.ARMOR) + sumModifiers(stack, EquipmentSlot.OFFHAND, Attributes.ARMOR));
            } catch (Exception ignored) {
            }
        }
        damage.sort(Comparator.comparingDouble(StatEntry::value));
        armor.sort(Comparator.comparingDouble(StatEntry::value));
        damageCache = Collections.unmodifiableList(damage);
        armorCache = Collections.unmodifiableList(armor);
        for (var entry : byCategory.entrySet()) {
            entry.getValue().sort(Comparator.comparingDouble(StatEntry::value));
            CATEGORY_CACHE.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
    }

    private static void addToCategory(java.util.EnumMap<ArmorCategory, List<StatEntry>> byCategory, ArmorCategory category, ItemStack stack, float value) {
        if (value > 0.0F) {
            byCategory.get(category).add(new StatEntry(stack, value));
        }
    }

    /** Suma solo los modifiers de tipo ADDITION (el "+X" que ya se ve en el tooltip vanilla). */
    private static float sumModifiers(ItemStack stack, EquipmentSlot slot, Attribute attribute) {
        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
        float total = 0.0F;
        for (AttributeModifier modifier : modifiers.get(attribute)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                total += (float) modifier.getAmount();
            }
        }
        return total;
    }
}
