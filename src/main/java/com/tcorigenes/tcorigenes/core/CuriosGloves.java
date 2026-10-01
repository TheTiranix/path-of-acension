// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Acceso a Curios aislado en su propia clase para que ColdResistance y ClimateImmunity no la carguen si Curios no esta instalado. */
final class CuriosGloves {
    private CuriosGloves() {
    }

    static boolean has(Player player) {
        return !first(player, "hands").isEmpty();
    }

    /** Primer item no vacio del slot de Curios con ese id, o vacio. */
    static ItemStack first(Player player, String slotId) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler(slotId))
                .map(stacks -> {
                    for (int i = 0; i < stacks.getStacks().getSlots(); i++) {
                        ItemStack stack = stacks.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            return stack;
                        }
                    }
                    return ItemStack.EMPTY;
                }).orElse(ItemStack.EMPTY);
    }
}
