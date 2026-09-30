// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.crafting;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.block.Block;

/**
 * Menu de la mesa tier 2: el mismo de la mesa comun (3x3, el cliente abre el de vanilla), pero valido solo estando cerca de NUESTRO
 * bloque y avisando su nivel para filtrar recetas (ver CraftingGate).
 */
public class TieredCraftingMenu extends CraftingMenu implements CraftingTier {
    private final ContainerLevelAccess tableAccess;
    private final Block table;
    private final int tier;

    public TieredCraftingMenu(int id, Inventory inventory, ContainerLevelAccess access, int tier, Block table) {
        super(id, inventory, access);
        this.tableAccess = access;
        this.table = table;
        this.tier = tier;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.tableAccess, player, this.table);
    }

    @Override
    public int craftingTier() {
        return this.tier;
    }
}
