// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat.jei;

import com.tcorigenes.tcorigenes.compat.LightArmorSets;
import java.text.DecimalFormat;
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

/** Categoria de JEI de sets de armadura: cada fila muestra casco, pechera, pantalones y botas del set, su nombre y su proteccion total. */
public class SetSortCategory implements IRecipeCategory<SetSortRecipe> {
    public static final int ROWS_PER_PAGE = 6;
    private static final int ROW_HEIGHT = 20;
    private static final int WIDTH = 9 * 18 + 24;
    private static final int HEIGHT = ROWS_PER_PAGE * ROW_HEIGHT;
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    private final RecipeType<SetSortRecipe> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public SetSortCategory(RecipeType<SetSortRecipe> type, Component title, ItemStack iconStack, IGuiHelper guiHelper) {
        this.type = type;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<SetSortRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SetSortRecipe recipe, IFocusGroup focuses) {
        for (int row = 0; row < recipe.sets().size(); row++) {
            ItemStack[] pieces = recipe.sets().get(row).pieces();
            for (int i = 0; i < 4; i++) {
                if (pieces[i] != null) {
                    builder.addSlot(RecipeIngredientRole.OUTPUT, i * 18 + 1, row * ROW_HEIGHT + 1).addItemStack(pieces[i]);
                }
            }
        }
    }

    @Override
    public void draw(SetSortRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        for (int row = 0; row < recipe.sets().size(); row++) {
            LightArmorSets.ArmorSet set = recipe.sets().get(row);
            int y = row * ROW_HEIGHT;
            String name = font.plainSubstrByWidth(set.name(), WIDTH - 82 - 34);
            guiGraphics.drawString(font, name, 78, y + 6, 0xFF404040, false);
            String total = FORMAT.format(set.total());
            guiGraphics.drawString(font, total, WIDTH - font.width(total) - 2, y + 6, 0xFF2E7D32, false);
        }
    }
}
