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
 * Categoria generica de JEI: solo el icono del item con su stat (daño o armadura) superpuesto
 * como numero chico, del mismo tamaño que un slot de inventario (18x18) para que JEI pueda
 * acomodar muchos por pagina en grilla, como la lista de ingredientes normal.
 */
public class StatSortCategory implements IRecipeCategory<StatEntry> {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private static final int WIDTH = 18;
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
        builder.addSlot(RecipeIngredientRole.OUTPUT, 0, 0).addItemStack(entry.stack());
    }

    @Override
    public void draw(StatEntry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Numero chico en la esquina inferior derecha del icono, como una cuenta de stack.
        var font = net.minecraft.client.Minecraft.getInstance().font;
        String text = FORMAT.format(entry.value());
        int x = WIDTH - font.width(text) - 1;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0, 0.0, 300.0);
        guiGraphics.drawString(font, text, x, 9, 0xFFFFA0, true);
        guiGraphics.pose().popPose();
    }
}
