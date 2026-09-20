// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat.jei;

import java.text.DecimalFormat;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Categoria de JEI: cada "receta" es una TANDA de hasta GRID_COLUMNS x GRID_ROWS items (ver
 * ModJeiPlugin, que parte la lista completa en tandas de a COLUMNS*ROWS), dibujados en una
 * grilla fija dentro de esa tanda -- varios items uno al lado del otro por pagina, en vez de
 * uno solo. Se probaron dos veces los widgets de scroll de JEI (addScrollGridWidget y
 * createScrollGridFactory) para meter la lista COMPLETA en una sola pagina scrolleable, pero esa
 * parte de la API esta deprecada y no se comporta como documenta (dibuja sin limites). Esta
 * version usa unicamente addSlot(role, x, y), la API estable de toda la vida, asi que cada
 * pagina se ve como una grilla real aunque haya que pasar de pagina en pagina para ver toda la
 * lista (412 items / 36 por pagina = 12 paginas en vez de 412).
 */
public class StatSortCategory implements IRecipeCategory<StatSortRecipe> {
    static final int GRID_COLUMNS = 9;
    static final int GRID_ROWS = 4;
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private static final int WIDTH = GRID_COLUMNS * 18;
    private static final int HEIGHT = GRID_ROWS * 18;

    private final RecipeType<StatSortRecipe> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public StatSortCategory(RecipeType<StatSortRecipe> type, Component title, ItemStack iconStack, IGuiHelper guiHelper) {
        this.type = type;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<StatSortRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, StatSortRecipe recipe, IFocusGroup focuses) {
        var entries = recipe.entries();
        for (int i = 0; i < entries.size(); i++) {
            StatEntry entry = entries.get(i);
            int col = i % GRID_COLUMNS;
            int row = i / GRID_COLUMNS;
            builder.addSlot(RecipeIngredientRole.OUTPUT, col * 18 + 1, row * 18 + 1)
                    .addItemStack(entry.stack())
                    .addTooltipCallback((slotView, tooltip) ->
                            tooltip.add(Component.literal(recipe.statLabel() + ": " + FORMAT.format(entry.value()))));
        }
    }
}
