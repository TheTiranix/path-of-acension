// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

/** Acceso a Curios aislado en su propia clase para que ColdResistance no la cargue si Curios no esta instalado. */
final class CuriosGloves {
    private CuriosGloves() {
    }

    static boolean has(Player player) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler("hands"))
                .map(stacks -> {
                    for (int i = 0; i < stacks.getStacks().getSlots(); i++) {
                        if (!stacks.getStacks().getStackInSlot(i).isEmpty()) {
                            return true;
                        }
                    }
                    return false;
                }).orElse(false);
    }
}
