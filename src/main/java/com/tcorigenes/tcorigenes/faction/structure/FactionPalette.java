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

    private FactionPalette(Block floor, Block wall, Block roof, Block accent, Block light, Block door) {
        this.floor = floor;
        this.wall = wall;
        this.roof = roof;
        this.accent = accent;
        this.light = light;
        this.door = door;
    }

    public static FactionPalette forFaction(Faction faction) {
        return switch (faction) {
            case PATER -> new FactionPalette(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE,
                    Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.REDSTONE_BLOCK, Blocks.TORCH, Blocks.DARK_OAK_DOOR);
            case FILIS -> new FactionPalette(Blocks.DIRT_PATH, Blocks.OAK_PLANKS,
                    Blocks.OAK_SLAB, Blocks.HAY_BLOCK, Blocks.LANTERN, Blocks.OAK_DOOR);
            case MEIDRIS -> new FactionPalette(Blocks.MOSSY_COBBLESTONE, Blocks.DARK_OAK_PLANKS,
                    Blocks.DARK_OAK_SLAB, Blocks.FLOWERING_AZALEA, Blocks.LANTERN, Blocks.SPRUCE_DOOR);
            case LUNA -> new FactionPalette(Blocks.ANDESITE, Blocks.DARK_OAK_PLANKS,
                    Blocks.DARK_OAK_SLAB, Blocks.AMETHYST_BLOCK, Blocks.SOUL_LANTERN, Blocks.WARPED_DOOR);
            case DEIROS -> new FactionPalette(Blocks.BASALT, Blocks.POLISHED_BASALT,
                    Blocks.BLACKSTONE_SLAB, Blocks.MAGMA_BLOCK, Blocks.TORCH, Blocks.CRIMSON_DOOR);
            case TEMPO -> new FactionPalette(Blocks.END_STONE, Blocks.END_STONE_BRICKS,
                    Blocks.PURPUR_SLAB, Blocks.CHORUS_FLOWER, Blocks.END_ROD, Blocks.BIRCH_DOOR);
        };
    }
}
