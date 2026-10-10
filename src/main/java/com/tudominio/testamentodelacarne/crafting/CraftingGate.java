// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.crafting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Nivel de mesa que pide cada receta de crafteo (pedido de alejandr0):
 * - nivel 3 (mesa de 5x5): las recetas propias de ese tipo (Tier3ShapedRecipe) y cualquiera que fabrique armas, herramientas o
 *   armaduras de arcane, black steel, dragonsteel, fiery o wintry (las recetas viejas de 3x3 de esos items se sacan con KubeJS);
 * - nivel 2: todo crafteo que use dark metal, gravitite, zanite o netherite como ingrediente (un ingrediente cuenta solo si TODOS
 *   sus items lo son: una etiqueta generica de lingotes no);
 * - nivel 1: el resto (la mesa comun).
 * El nivel de la mesa que se esta usando lo fija CraftingMenuMixin mientras se calcula el resultado.
 */
@EventBusSubscriber(modid = "testamentodelacarne")
public final class CraftingGate {
    /** Nivel de la mesa que esta calculando un resultado ahora mismo (-1 = ninguna, no se filtra). Lo pone CraftingMenuMixin. */
    public static int currentTier = -1;

    private static final String[] TIER2_MARKERS = {"dark_metal", "darkmetal", "gravitite", "zanite", "netherite"};
    private static final List<String> TIER3_PREFIXES = List.of(
            "testamentodelacarne:arcane_", "testamentodelacarne:black_steel_", "cataclysm:black_steel_",
            "iceandfire:dragonsteel_", "testamentodelacarne:fiery_", "testamentodelacarne:wintry_", "twilightforest:fiery_");
    private static final Map<ResourceLocation, Integer> CACHE = new HashMap<>();

    private CraftingGate() {
    }

    @SubscribeEvent
    public static void onTags(TagsUpdatedEvent event) {
        CACHE.clear();
    }

    private static boolean isTier2Material(ResourceLocation id) {
        String path = id.getPath();
        for (String marker : TIER2_MARKERS) {
            if (path.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static final List<String> GEAR_SUFFIXES = List.of("_sword", "_axe", "_pickaxe", "_shovel", "_hoe", "_helmet", "_chestplate",
            "_leggings", "_boots", "_dagger", "_longsword", "_katana", "_greatsword", "_battleaxe", "_halberd", "_glaive", "_hammer",
            "_quarterstaff", "_spear");

    /** Armas, herramientas y armaduras de arcane, black steel y dragonsteel (y la Amethyst Rapier y la Spellbreaker). */
    public static boolean isTier3Gear(ResourceLocation id) {
        String s = id.toString();
        if (s.equals("irons_spellbooks:amethyst_rapier") || s.equals("irons_spellbooks:spellbreaker")) {
            return true;
        }
        for (String prefix : TIER3_PREFIXES) {
            if (s.startsWith(prefix)) {
                for (String suffix : GEAR_SUFFIXES) {
                    if (s.endsWith(suffix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int requiredTier(CraftingRecipe recipe, RegistryAccess access) {
        if (recipe instanceof Tier3ShapedRecipe) {
            return 3;
        }
        Integer cached = CACHE.get(recipe.getId());
        if (cached != null) {
            return cached;
        }
        int tier = 1;
        ItemStack result;
        try {
            result = recipe.getResultItem(access);
        } catch (RuntimeException e) {
            result = ItemStack.EMPTY; // recetas especiales que no saben decir su resultado sin contexto
        }
        ResourceLocation resultId = result.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(result.getItem());
        if (resultId != null && isTier3Gear(resultId)) {
            tier = 3;
        } else {
            for (Ingredient ingredient : recipe.getIngredients()) {
                ItemStack[] items = ingredient.getItems();
                if (items.length == 0) {
                    continue;
                }
                boolean all = true;
                for (ItemStack stack : items) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (id == null || !isTier2Material(id)) {
                        all = false;
                        break;
                    }
                }
                if (all) {
                    tier = 2;
                    break;
                }
            }
        }
        CACHE.put(recipe.getId(), tier);
        return tier;
    }
}
