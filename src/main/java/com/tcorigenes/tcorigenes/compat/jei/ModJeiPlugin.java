package com.tcorigenes.tcorigenes.compat.jei;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Dos categorias nuevas en JEI: "Ordenado por daño" y "Ordenado por armadura", con TODOS los
 * items del pack que dan ese stat, de menor a mayor. Se accede haciendo click en JEI sobre una
 * espada de netherite (para daño) o un pechera de netherite (para armadura) y viendo sus "usos".
 */
@JeiPlugin
public class ModJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath("tcorigenes", "jei_plugin");
    public static final RecipeType<StatEntry> DAMAGE_SORT = RecipeType.create("tcorigenes", "damage_sort", StatEntry.class);
    public static final RecipeType<StatEntry> ARMOR_SORT = RecipeType.create("tcorigenes", "armor_sort", StatEntry.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new StatSortCategory(DAMAGE_SORT, Component.literal("Ordenado por daño"),
                        new ItemStack(Items.NETHERITE_SWORD), guiHelper),
                new StatSortCategory(ARMOR_SORT, Component.literal("Ordenado por armadura"),
                        new ItemStack(Items.NETHERITE_CHESTPLATE), guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<StatEntry> damageEntries = new ArrayList<>();
        List<StatEntry> armorEntries = new ArrayList<>();

        int errors = 0;
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            // Con miles de items de ~180 mods, alguno raro puede tirar excepcion al calcular sus
            // modifiers (implementaciones custom de getAttributeModifiers). Si no se aisla item
            // por item, UN item roto puede abortar el registro de TODA la lista sin avisar.
            try {
                ItemStack stack = new ItemStack(item);
                if (stack.isEmpty()) {
                    continue;
                }

                float damage = sumModifiers(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
                if (damage > 0.0F) {
                    damageEntries.add(new StatEntry(stack, damage));
                }

                float armor = sumModifiers(stack, EquipmentSlot.HEAD, Attributes.ARMOR)
                        + sumModifiers(stack, EquipmentSlot.CHEST, Attributes.ARMOR)
                        + sumModifiers(stack, EquipmentSlot.LEGS, Attributes.ARMOR)
                        + sumModifiers(stack, EquipmentSlot.FEET, Attributes.ARMOR);
                if (armor > 0.0F) {
                    armorEntries.add(new StatEntry(stack, armor));
                }
            } catch (Exception e) {
                errors++;
            }
        }

        damageEntries.sort(Comparator.comparingDouble(StatEntry::value));
        armorEntries.sort(Comparator.comparingDouble(StatEntry::value));

        org.slf4j.LoggerFactory.getLogger(ModJeiPlugin.class).info(
                "[tcorigenes] JEI: {} items con daño, {} con armadura ({} items tiraron error al leerlos)",
                damageEntries.size(), armorEntries.size(), errors);

        registration.addRecipes(DAMAGE_SORT, damageEntries);
        registration.addRecipes(ARMOR_SORT, armorEntries);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(DAMAGE_SORT, Items.NETHERITE_SWORD);
        registration.addRecipeCatalysts(ARMOR_SORT, Items.NETHERITE_CHESTPLATE);
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
