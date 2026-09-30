// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat.jei;

import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * Categoria de JEI de las mesas de crafteo con nivel (tier 2: grilla de 3x3, tier 3: grilla de 5x5): las recetas que solo se
 * pueden hacer ahi (ver CraftingGate) salen de la categoria de crafteo comun y se muestran en la de su mesa, asi "R" y "U" sobre un
 * item llevan a la receta correcta.
 */
public class TieredCraftingCategory implements IRecipeCategory<CraftingRecipe> {
    private final RecipeType<CraftingRecipe> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IGuiHelper guiHelper;
    private final int grid;

    public TieredCraftingCategory(RecipeType<CraftingRecipe> type, Component title, ItemStack iconStack, int grid, IGuiHelper guiHelper) {
        this.type = type;
        this.title = title;
        this.grid = grid;
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.background = guiHelper.createBlankDrawable(grid * 18 + 70, grid * 18 + 4);
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return this.type;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CraftingRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : this.grid;
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.isEmpty()) {
                continue;
            }
            builder.addSlot(RecipeIngredientRole.INPUT, 2 + (i % width) * 18, 2 + (i / width) * 18)
                    .setBackground(this.guiHelper.getSlotDrawable(), -1, -1)
                    .addIngredients(ingredient);
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, this.grid * 18 + 40, this.grid * 9 - 7)
                    .setBackground(this.guiHelper.getSlotDrawable(), -1, -1)
                    .addItemStack(recipe.getResultItem(level.registryAccess()));
        }
    }

    @Override
    public void draw(CraftingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.drawString(Minecraft.getInstance().font, "\u2192", this.grid * 18 + 18, this.grid * 9 - 3, 0xFF555555, false);
    }
}
