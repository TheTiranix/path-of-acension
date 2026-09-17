package com.tcorigenes.tcorigenes.compat.jei;

import java.text.DecimalFormat;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Categoria de JEI con UNA sola "receta" (toda la lista ordenada de una vez), mostrada como una
 * grilla scrolleable de items uno al lado del otro (como la lista de ingredientes normal), en
 * vez de una fila por item que obligaba a pasar de pagina en pagina. El valor (daño/armadura) se
 * ve en el tooltip al pasar el mouse por cada item.
 */
public class StatSortCategory implements IRecipeCategory<StatSortRecipe> {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int WIDTH = COLUMNS * 18;
    private static final int HEIGHT = ROWS * 18;

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
        for (StatEntry entry : recipe.entries()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT)
                    .addItemStack(entry.stack())
                    .addTooltipCallback((slotView, tooltip) ->
                            tooltip.add(Component.literal(recipe.statLabel() + ": " + FORMAT.format(entry.value()))));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, StatSortRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> slots = builder.getRecipeSlots().getSlots();
        builder.addScrollGridWidget(slots, WIDTH, HEIGHT);
    }
}
