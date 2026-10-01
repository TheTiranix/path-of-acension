// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import com.tudominio.testamentodelacarne.crafting.CraftingGate;
import com.tudominio.testamentodelacarne.crafting.CraftingTier;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mesas de crafteo con nivel (ver CraftingGate): mientras CraftingMenu#slotChangedCraftingGrid calcula el resultado (la usan la
 * mesa comun, la tier 2 y la grilla 2x2 del inventario), se anota el nivel del menu y se descartan las recetas que piden mas.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "m_150546_", at = @At("HEAD"), remap = false, require = 0)
    private static void testamentodelacarne$tierStart(AbstractContainerMenu menu, Level level, Player player, CraftingContainer container,
            ResultContainer result, CallbackInfo ci) {
        // todo menu que no sea de nuestras mesas con nivel (mesa comun, grilla 2x2 del inventario, mesas de otros mods) es nivel 1
        CraftingGate.currentTier = menu instanceof CraftingTier tiered ? tiered.craftingTier() : 1;
    }

    @Inject(method = "m_150546_", at = @At("RETURN"), remap = false, require = 0)
    private static void testamentodelacarne$tierEnd(AbstractContainerMenu menu, Level level, Player player, CraftingContainer container,
            ResultContainer result, CallbackInfo ci) {
        CraftingGate.currentTier = -1;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "m_150546_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;m_44015_(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"),
            remap = false, require = 0)
    private static Optional testamentodelacarne$filterByTier(RecipeManager manager, RecipeType type, Container container, Level level) {
        Optional<Recipe> found = manager.getRecipeFor(type, container, level);
        int tier = CraftingGate.currentTier;
        if (tier >= 0 && found.isPresent() && found.get() instanceof CraftingRecipe recipe
                && CraftingGate.requiredTier(recipe, level.registryAccess()) > tier) {
            return Optional.empty();
        }
        return found;
    }
}
