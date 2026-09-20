// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.favor;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Que ítem ofrendar en el altar de cada dios y cuanto favor da. Ítems tematicos razonables
 * segun el lore (Pater=orden/muerte-purga, Filis=redencion/curacion, Luna=oculto/magia oscura,
 * provisorio hasta confirmar). Facil de ajustar despues, y todavia faltan altares de
 * Meidris/Deiros/Tempo.
 */
public final class AltarOfferings {
    private static final Map<Deity, Map<Item, Integer>> OFFERINGS = new EnumMap<>(Deity.class);

    static {
        OFFERINGS.put(Deity.PATER, Map.of(
                Items.ROTTEN_FLESH, 1,
                Items.BONE, 1,
                Items.SPIDER_EYE, 2,
                Items.GUNPOWDER, 2,
                Items.WITHER_ROSE, 5,
                Items.TNT, 8));

        OFFERINGS.put(Deity.FILIS, Map.of(
                Items.WHEAT_SEEDS, 1,
                Items.WHEAT, 1,
                Items.EGG, 1,
                Items.APPLE, 2,
                Items.HAY_BLOCK, 3,
                Items.GOLDEN_CARROT, 3));

        OFFERINGS.put(Deity.LUNA, Map.of(
                Items.BOOK, 1,
                Items.LAPIS_LAZULI, 1,
                Items.EXPERIENCE_BOTTLE, 3,
                Items.NAME_TAG, 2,
                Items.ENCHANTED_BOOK, 10));

        OFFERINGS.put(Deity.MEIDRIS, Map.of(
                Items.OAK_SAPLING, 1,
                Items.BONE_MEAL, 1,
                Items.SWEET_BERRIES, 1,
                Items.MOSS_BLOCK, 2,
                Items.GLOW_BERRIES, 2,
                Items.TOTEM_OF_UNDYING, 10));

        OFFERINGS.put(Deity.DEIROS, Map.of(
                Items.FLINT_AND_STEEL, 2,
                Items.BLAZE_POWDER, 2,
                Items.FIRE_CHARGE, 3,
                Items.MAGMA_CREAM, 3,
                Items.BLAZE_ROD, 5,
                Items.NETHER_STAR, 15));

        OFFERINGS.put(Deity.TEMPO, Map.of(
                Items.ENDER_PEARL, 2,
                Items.ENDER_EYE, 3,
                Items.CHORUS_FRUIT, 1,
                Items.DRAGON_BREATH, 8,
                Items.CLOCK, 3));
    }

    private AltarOfferings() {
    }

    /** Devuelve el favor que da esa ofrenda en ese altar, o null si el altar no la acepta. */
    public static Integer getFavorValue(Deity deity, Item item) {
        Map<Item, Integer> table = OFFERINGS.get(deity);
        return table == null ? null : table.get(item);
    }
}
