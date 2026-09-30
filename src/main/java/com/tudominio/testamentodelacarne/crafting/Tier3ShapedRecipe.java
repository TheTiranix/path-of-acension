// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** Receta con forma de hasta 5x5 para la mesa de crafteo tier 3 (la de vanilla no pasa de 3x3). */
public class Tier3ShapedRecipe extends ShapedRecipe {
    public Tier3ShapedRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height,
            NonNullList<Ingredient> ingredients, ItemStack result) {
        super(id, group, category, width, height, ingredients, result);
    }

    @Override
    public RecipeType<?> getType() {
        return ModCrafting.TIER3_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModCrafting.TIER3_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<Tier3ShapedRecipe> {
        @Override
        public Tier3ShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null),
                    CraftingBookCategory.MISC);
            Map<String, Ingredient> key = keyFromJson(GsonHelper.getAsJsonObject(json, "key"));
            JsonArray rows = GsonHelper.getAsJsonArray(json, "pattern");
            if (rows.size() < 1 || rows.size() > 5) {
                throw new JsonSyntaxException("El patron tiene que tener entre 1 y 5 filas");
            }
            String[] pattern = new String[rows.size()];
            for (int i = 0; i < pattern.length; i++) {
                pattern[i] = GsonHelper.convertToString(rows.get(i), "pattern[" + i + "]");
                if (pattern[i].length() > 5) {
                    throw new JsonSyntaxException("Cada fila del patron puede tener hasta 5 columnas");
                }
            }
            pattern = shrink(pattern);
            int width = pattern[0].length();
            int height = pattern.length;
            NonNullList<Ingredient> ingredients = dissolvePattern(pattern, key, width, height);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new Tier3ShapedRecipe(id, group, category, width, height, ingredients, result);
        }

        private static Map<String, Ingredient> keyFromJson(JsonObject json) {
            Map<String, Ingredient> map = new java.util.HashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                if (entry.getKey().length() != 1) {
                    throw new JsonSyntaxException("Clave invalida: '" + entry.getKey() + "' (tiene que ser 1 caracter)");
                }
                if (" ".equals(entry.getKey())) {
                    throw new JsonSyntaxException("La clave ' ' esta reservada para el espacio vacio");
                }
                map.put(entry.getKey(), Ingredient.fromJson(entry.getValue(), false));
            }
            map.put(" ", Ingredient.EMPTY);
            return map;
        }

        /** Saca las filas y columnas vacias de los bordes (la receta se puede poner en cualquier lugar de la grilla). */
        private static String[] shrink(String[] pattern) {
            int firstCol = Integer.MAX_VALUE;
            int lastCol = 0;
            int firstRow = 0;
            int emptyTail = 0;
            for (int row = 0; row < pattern.length; row++) {
                String line = pattern[row];
                firstCol = Math.min(firstCol, firstNonSpace(line));
                int last = lastNonSpace(line);
                lastCol = Math.max(lastCol, last);
                if (last < 0) {
                    if (firstRow == row) {
                        firstRow++;
                    }
                    emptyTail++;
                } else {
                    emptyTail = 0;
                }
            }
            if (pattern.length == emptyTail) {
                return new String[0];
            }
            String[] out = new String[pattern.length - emptyTail - firstRow];
            for (int i = 0; i < out.length; i++) {
                out[i] = pattern[i + firstRow].substring(firstCol, lastCol + 1);
            }
            return out;
        }

        private static int firstNonSpace(String s) {
            int i = 0;
            while (i < s.length() && s.charAt(i) == ' ') {
                i++;
            }
            return i;
        }

        private static int lastNonSpace(String s) {
            int i = s.length() - 1;
            while (i >= 0 && s.charAt(i) == ' ') {
                i--;
            }
            return i;
        }

        private static NonNullList<Ingredient> dissolvePattern(String[] pattern, Map<String, Ingredient> key, int width, int height) {
            NonNullList<Ingredient> list = NonNullList.withSize(width * height, Ingredient.EMPTY);
            java.util.Set<String> unused = new java.util.HashSet<>(key.keySet());
            unused.remove(" ");
            for (int row = 0; row < pattern.length; row++) {
                for (int col = 0; col < pattern[row].length(); col++) {
                    String symbol = pattern[row].substring(col, col + 1);
                    Ingredient ingredient = key.get(symbol);
                    if (ingredient == null) {
                        throw new JsonSyntaxException("El patron usa '" + symbol + "' pero la receta no lo define");
                    }
                    unused.remove(symbol);
                    list.set(col + width * row, ingredient);
                }
            }
            if (!unused.isEmpty()) {
                throw new JsonSyntaxException("La receta define claves que el patron no usa: " + unused);
            }
            return list;
        }

        @Override
        public Tier3ShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readUtf();
            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buf));
            ItemStack result = buf.readItem();
            return new Tier3ShapedRecipe(id, group, category, width, height, ingredients, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, Tier3ShapedRecipe recipe) {
            buf.writeVarInt(recipe.getWidth());
            buf.writeVarInt(recipe.getHeight());
            buf.writeUtf(recipe.getGroup());
            buf.writeEnum(recipe.category());
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buf);
            }
            buf.writeItem(recipe.getResultItem(RegistryAccess.EMPTY));
        }
    }
}
