// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

/** La Endersoul Hand (Mutant Monsters) equipada en el slot de guantes de Curios agrega daño elemental ender a cada golpe. */
public final class EndersoulGlove {
    public static final ResourceLocation ITEM_ID = ResourceLocation.fromNamespaceAndPath("mutantmonsters", "endersoul_hand");
    /** Daño elemental ender por golpe mientras esta equipada como guante. */
    public static final float ENDER_DAMAGE = 30.0F;

    private EndersoulGlove() {
    }

    public static boolean isEquipped(Player player) {
        Item item = ForgeRegistries.ITEMS.getValue(ITEM_ID);
        if (item == null || !ForgeRegistries.ITEMS.containsKey(ITEM_ID)) {
            return false;
        }
        try {
            return CuriosApi.getCuriosInventory(player)
                    .map(handler -> handler.findFirstCurio(item).isPresent()).orElse(false);
        } catch (LinkageError e) {
            return false; // sin Curios
        }
    }
}
