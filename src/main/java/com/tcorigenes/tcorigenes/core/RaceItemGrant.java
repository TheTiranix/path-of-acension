// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Items ligados a una RAZA (a diferencia de los de clase, ver ClassSelection): por ahora, la Espada
 * Ánima Fase 1 del Ender Warrior. Antes esto se hacia mal, con un /give de consola disparado desde
 * el comando /setrace que encima le ponia Maldicion del Desvanecimiento (se perdia al morir, todo lo
 * contrario a lo pedido) y solo pasaba al usar ese comando de admin, nunca en la eleccion real de
 * raza. Ahora se llama desde el UNICO lugar donde de verdad se fija la raza de un jugador (tanto la
 * pantalla real como /setrace pasan por aca) y la espada queda marcada Soulbound de verdad.
 */
public final class RaceItemGrant {
    private RaceItemGrant() {
    }

    public static void grant(ServerPlayer player, Race race) {
        if (race != Race.ENDER_WARRIOR) {
            return;
        }
        // Ya tiene la suya (marcada soulbound): no darle una segunda.
        boolean alreadyHasOne = player.getInventory().items.stream().anyMatch(SoulboundItems::isSoulbound);
        if (alreadyHasOne) {
            return;
        }
        Item sword = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "espada_anima_1"));
        if (sword == null) {
            return;
        }
        ItemStack stack = new ItemStack(sword);
        SoulboundItems.markSoulbound(stack);
        player.getInventory().add(stack);
    }
}
