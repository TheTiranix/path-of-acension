package com.tcorigenes.tcorigenes.compat.jei;

import java.text.DecimalFormat;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Categoria de JEI, una fila por item (icono + numero de daño/armadura), paginada con las
 * flechas de arriba (ej "38/412"). Se probo una grilla con scroll para que se vean muchos items
 * juntos como la lista de ingredientes normal, pero la API de JEI para eso no se comporto como
 * documentado (termino dibujando sin limites, tapando toda la pantalla) - esta version paginada
 * es la que funciona de forma estable.
 */
public class StatSortCategory implements IRecipeCategory<StatEntry> {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private static final int WIDTH = 90;
    private static final int HEIGHT = 18;

    private final RecipeType<StatEntry> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public StatSortCategory(RecipeType<StatEntry> type, Component title, ItemStack iconStack, IGuiHelper guiHelper) {
        this.type = type;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<StatEntry> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, StatEntry entry, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 1).addItemStack(entry.stack());
    }

    @Override
    public void draw(StatEntry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                FORMAT.format(entry.value()), 22, 5, 0x404040, false);
    }
}
