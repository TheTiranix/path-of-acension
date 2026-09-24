// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat.jei;

import com.tcorigenes.tcorigenes.compat.ItemStatRanking;
import com.tcorigenes.tcorigenes.compat.ItemStatRanking.ArmorCategory;
import com.tcorigenes.tcorigenes.item.ModItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Categorias de JEI "Ordenado por daño" y "Ordenado por armadura" (esta ultima separada en
 * cascos, pecheras, pantalones, botas y otros), de menor a mayor. Se abren con los botones del
 * inventario (ver client.RankingButtons) o haciendo click en "usos" del Orbe / Anillo de Purificacion.
 */
@JeiPlugin
public class ModJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath("tcorigenes", "jei_plugin");
    public static final RecipeType<StatSortRecipe> DAMAGE_SORT = RecipeType.create("tcorigenes", "damage_sort", StatSortRecipe.class);
    public static final Map<ArmorCategory, RecipeType<StatSortRecipe>> ARMOR_SORT = new EnumMap<>(ArmorCategory.class);

    static {
        for (ArmorCategory category : ArmorCategory.values()) {
            ARMOR_SORT.put(category, RecipeType.create("tcorigenes", "armor_" + category.name().toLowerCase(), StatSortRecipe.class));
        }
    }

    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        // Armas eliminadas del pack (ver RemovedItems): fuera de JEI.
        List<ItemStack> hidden = new ArrayList<>();
        for (ResourceLocation id : com.tcorigenes.tcorigenes.weapon.RemovedItems.IDS) {
            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != Items.AIR) {
                hidden.add(new ItemStack(item));
            }
        }
        if (!hidden.isEmpty()) {
            jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, hidden);
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static void showDamage() {
        if (runtime != null) {
            runtime.getRecipesGui().showTypes(List.of(DAMAGE_SORT));
        }
    }

    public static void showArmor() {
        if (runtime != null) {
            List<RecipeType<?>> types = new ArrayList<>(ARMOR_SORT.values());
            runtime.getRecipesGui().showTypes(types);
        }
    }

    private static Item iconOf(ArmorCategory category) {
        return switch (category) {
            case HELMET -> Items.NETHERITE_HELMET;
            case CHESTPLATE -> Items.NETHERITE_CHESTPLATE;
            case LEGGINGS -> Items.NETHERITE_LEGGINGS;
            case BOOTS -> Items.NETHERITE_BOOTS;
            case OTHER -> Items.SHIELD;
        };
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new StatSortCategory(DAMAGE_SORT, Component.literal("Ordenado por daño"),
                new ItemStack(Items.NETHERITE_SWORD), guiHelper));
        for (ArmorCategory category : ArmorCategory.values()) {
            registration.addRecipeCategories(new StatSortCategory(ARMOR_SORT.get(category),
                    Component.literal("Armadura: " + category.label()), new ItemStack(iconOf(category)), guiHelper));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<StatEntry> damageEntries = ItemStatRanking.damageRanking();
        org.slf4j.LoggerFactory.getLogger(ModJeiPlugin.class).info(
                "[tcorigenes] JEI: {} items con daño, {} con armadura", damageEntries.size(), ItemStatRanking.armorRanking().size());

        registration.addRecipes(DAMAGE_SORT, chunk(damageEntries, "Daño"));
        for (ArmorCategory category : ArmorCategory.values()) {
            registration.addRecipes(ARMOR_SORT.get(category), chunk(ItemStatRanking.armorRanking(category), "Armadura"));
        }
    }

    /** Parte la lista completa en tandas de GRID_COLUMNS*GRID_ROWS para que cada pagina de JEI
     *  muestre una grilla llena en vez de un solo item. */
    private static List<StatSortRecipe> chunk(List<StatEntry> sorted, String label) {
        int pageSize = StatSortCategory.GRID_COLUMNS * StatSortCategory.GRID_ROWS;
        List<StatSortRecipe> pages = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i += pageSize) {
            pages.add(new StatSortRecipe(sorted.subList(i, Math.min(i + pageSize, sorted.size())), label));
        }
        return pages;
    }

    /** Reserva la zona de los botones de ranking para que la lista de items de JEI no los tape. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(InventoryScreen.class, rankingButtonsArea());
        registration.addGuiContainerHandler(CreativeModeInventoryScreen.class, rankingButtonsArea());
    }

    private static <T extends AbstractContainerScreen<?>> IGuiContainerHandler<T> rankingButtonsArea() {
        return new IGuiContainerHandler<T>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(T screen) {
                return List.of(com.tcorigenes.tcorigenes.client.RankingButtons.area(screen));
            }
        };
    }

    /** Catalizadores: items que NUNCA aparecen en las listas (si no JEI filtra los "usos" a ese item). */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(DAMAGE_SORT, ModItems.ORBE_DE_ORIGENES.get());
        for (ArmorCategory category : ArmorCategory.values()) {
            registration.addRecipeCatalysts(ARMOR_SORT.get(category), ModItems.ANILLO_DE_PURIFICACION.get());
        }
    }
}
