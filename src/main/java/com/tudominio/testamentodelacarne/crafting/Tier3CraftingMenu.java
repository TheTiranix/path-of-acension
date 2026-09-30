// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.crafting;

import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Mesa de crafteo tier 3: grilla de 5x5. Resuelve primero las recetas de 5x5 (Tier3ShapedRecipe) y despues las de crafteo comun
 * (de 3x3 caben en la grilla, y al ser nivel 3 no se filtra ninguna).
 */
public class Tier3CraftingMenu extends AbstractContainerMenu implements CraftingTier {
    public static final int GRID = 5;
    public static final int RESULT_SLOT = 0;
    private static final int GRID_START = 1;
    private static final int GRID_END = GRID_START + GRID * GRID;
    private static final int INV_START = GRID_END;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_END = INV_END + 9;

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, GRID, GRID);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private final Block table;

    public Tier3CraftingMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL, null);
    }

    public Tier3CraftingMenu(int id, Inventory inventory, ContainerLevelAccess access, Block table) {
        super(ModCrafting.TIER3_MENU.get(), id);
        this.access = access;
        this.player = inventory.player;
        this.table = table;
        this.addSlot(new ResultSlot(inventory.player, this.craftSlots, this.resultSlots, 0, 142, 56));
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                this.addSlot(new Slot(this.craftSlots, col + row * GRID, 18 + col * 18, 20 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 132 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 190));
        }
    }

    @Override
    public int craftingTier() {
        return 3;
    }

    @Override
    public void slotsChanged(Container container) {
        this.access.execute((level, pos) -> updateResult(level));
    }

    private void updateResult(Level level) {
        if (level.isClientSide() || !(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack result = ItemStack.EMPTY;
        Optional<? extends CraftingRecipe> recipe = level.getRecipeManager().getRecipeFor(ModCrafting.TIER3_TYPE.get(), this.craftSlots, level);
        if (recipe.isEmpty()) {
            recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.craftSlots, level);
        }
        if (recipe.isPresent()) {
            CraftingRecipe found = recipe.get();
            if (this.resultSlots.setRecipeUsed(level, serverPlayer, found)) {
                ItemStack assembled = found.assemble(this.craftSlots, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        this.resultSlots.setItem(0, result);
        this.setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.table == null || stillValid(this.access, player, this.table);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index == RESULT_SLOT) {
                this.access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, level, player));
                if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, copy);
            } else if (index >= INV_START && index < HOTBAR_END) {
                if (!this.moveItemStackTo(stack, GRID_START, GRID_END, false)) {
                    if (index < INV_END) {
                        if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (index == RESULT_SLOT) {
                player.drop(stack, false);
            }
        }
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }
}
