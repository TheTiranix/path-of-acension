// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.faction.structure;

import com.tcorigenes.tcorigenes.faction.Faction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Paleta de bloques vanilla por faccion, usada para levantar el campamento (ver FactionCampBuilder). */
public final class FactionPalette {
    public final Block floor;
    public final Block wall;
    public final Block roof;
    public final Block accent;
    public final Block light;
    public final Block door;
    /** Cama a juego con la faccion (detalle de interior). */
    public final Block bed;
    /** Escalera a juego con la pared, para el alero del techo (mas detalle que una losa lisa). */
    public final Block wallStairs;

    private FactionPalette(Block floor, Block wall, Block roof, Block accent, Block light, Block door, Block bed, Block wallStairs) {
        this.floor = floor;
        this.wall = wall;
        this.roof = roof;
        this.accent = accent;
        this.light = light;
        this.door = door;
        this.bed = bed;
        this.wallStairs = wallStairs;
    }

    public static FactionPalette forFaction(Faction faction) {
        return switch (faction) {
            case PATER -> new FactionPalette(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE,
                    Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.REDSTONE_BLOCK, Blocks.TORCH, Blocks.DARK_OAK_DOOR,
                    Blocks.RED_BED, Blocks.POLISHED_BLACKSTONE_STAIRS);
            case FILIS -> new FactionPalette(Blocks.DIRT_PATH, Blocks.OAK_PLANKS,
                    Blocks.OAK_SLAB, Blocks.HAY_BLOCK, Blocks.LANTERN, Blocks.OAK_DOOR,
                    Blocks.YELLOW_BED, Blocks.OAK_STAIRS);
            case MEIDRIS -> new FactionPalette(Blocks.MOSSY_COBBLESTONE, Blocks.DARK_OAK_PLANKS,
                    Blocks.DARK_OAK_SLAB, Blocks.FLOWERING_AZALEA, Blocks.LANTERN, Blocks.SPRUCE_DOOR,
                    Blocks.GREEN_BED, Blocks.DARK_OAK_STAIRS);
            case LUNA -> new FactionPalette(Blocks.ANDESITE, Blocks.DARK_OAK_PLANKS,
                    Blocks.DARK_OAK_SLAB, Blocks.AMETHYST_BLOCK, Blocks.SOUL_LANTERN, Blocks.WARPED_DOOR,
                    Blocks.PURPLE_BED, Blocks.DARK_OAK_STAIRS);
            case DEIROS -> new FactionPalette(Blocks.BASALT, Blocks.POLISHED_BASALT,
                    Blocks.BLACKSTONE_SLAB, Blocks.MAGMA_BLOCK, Blocks.TORCH, Blocks.CRIMSON_DOOR,
                    Blocks.BLACK_BED, Blocks.BLACKSTONE_STAIRS);
            case TEMPO -> new FactionPalette(Blocks.END_STONE, Blocks.END_STONE_BRICKS,
                    Blocks.PURPUR_SLAB, Blocks.CHORUS_FLOWER, Blocks.END_ROD, Blocks.BIRCH_DOOR,
                    Blocks.WHITE_BED, Blocks.END_STONE_BRICK_STAIRS);
        };
    }
}
