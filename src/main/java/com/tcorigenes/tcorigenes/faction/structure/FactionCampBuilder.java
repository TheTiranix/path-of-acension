package com.tcorigenes.tcorigenes.faction.structure;

import com.tcorigenes.tcorigenes.faction.Faction;
import com.tcorigenes.tcorigenes.faction.ModEntityTypes;
import com.tcorigenes.tcorigenes.faction.entity.FactionNpcEntity;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Logica de construccion del campamento, compartida por FactionCampPiece (la Structure real).
 * Cada llamada a place() respeta el BoundingBox de la porcion de chunk que se esta procesando
 * (postProcess se llama una vez por cada chunk que toca el campamento), y el spawn de NPCs/cofre
 * solo se ejecuta una vez, cuando el chunk que contiene el centro es el que se esta procesando.
 */
public final class FactionCampBuilder {
    private static final int HUT_RING_RADIUS = 9;
    private static final int FLOOR_RADIUS = 13;

    private FactionCampBuilder() {
    }

    public static BlockPos findNetherGround(WorldGenLevel level, BlockPos origin) {
        int x = origin.getX();
        int z = origin.getZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, 100, z);
        for (int y = 100; y > 10; y--) {
            cursor.setY(y);
            if (!level.getBlockState(cursor).isAir()
                    && level.getBlockState(cursor.above()).isAir()
                    && level.getBlockState(cursor.above(2)).isAir()
                    && level.getBlockState(cursor.above(3)).isAir()
                    && level.getBlockState(cursor.above(4)).isAir()) {
                return cursor.above().immutable();
            }
        }
        return null;
    }

    /**
     * El Y calculado en Structure.findGenerationPoint viene de un muestreo de ruido, hecho ANTES
     * de que el terreno final del chunk este generado. En biomas irregulares (badlands) eso podia
     * anclar el campamento a un pico aislado y dejarlo flotando sobre el terreno real. Ahora que
     * postProcess corre con el terreno YA generado, volvemos a calcular la altura de verdad
     * muestreando varios puntos (no solo el centro) y usando la mediana, para no quedar pegados
     * a un pico o pozo puntual tampoco.
     */
    public static BlockPos findOverworldGround(WorldGenLevel level, BlockPos approxOrigin) {
        int x = approxOrigin.getX();
        int z = approxOrigin.getZ();
        int[] samples = {
                level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z),
                level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x - 10, z),
                level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x + 10, z),
                level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z - 10),
                level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z + 10)
        };
        Arrays.sort(samples);
        return new BlockPos(x, samples[2], z);
    }

    public static void build(WorldGenLevel level, BlockPos center, Faction faction, RandomSource random, BoundingBox box) {
        FactionPalette palette = FactionPalette.forFaction(faction);
        buildCamp(level, center, palette, box);
        if (box.isInside(center)) {
            fillFlavorChest(level, center, faction);
            spawnNpcs(level, center, faction, random);
            FactionCampSavedData.get(level.getLevel()).addCamp(faction, center);
        }
    }

    private static void place(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 3);
        }
    }

    private static void buildCamp(WorldGenLevel level, BlockPos center, FactionPalette palette, BoundingBox box) {
        for (int dx = -FLOOR_RADIUS; dx <= FLOOR_RADIUS; dx++) {
            for (int dz = -FLOOR_RADIUS; dz <= FLOOR_RADIUS; dz++) {
                if (dx * dx + dz * dz <= FLOOR_RADIUS * FLOOR_RADIUS) {
                    place(level, box, center.offset(dx, -1, dz), palette.floor.defaultBlockState());
                }
            }
        }

        place(level, box, center, palette.accent.defaultBlockState());
        place(level, box, center.above(), palette.accent.defaultBlockState());
        place(level, box, center.above(2), palette.light.defaultBlockState());

        double[] angles = {90, 210, 330};
        for (double angleDeg : angles) {
            double rad = Math.toRadians(angleDeg);
            int hx = center.getX() + (int) Math.round(Math.cos(rad) * HUT_RING_RADIUS);
            int hz = center.getZ() + (int) Math.round(Math.sin(rad) * HUT_RING_RADIUS);
            BlockPos hutCenter = new BlockPos(hx, center.getY(), hz);
            Direction doorFacing = nearestCardinal(center.getX() - hx, center.getZ() - hz);
            buildHut(level, hutCenter, palette, doorFacing, box);
        }
    }

    private static Direction nearestCardinal(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static void buildHut(WorldGenLevel level, BlockPos hutCenter, FactionPalette palette, Direction doorFacing, BoundingBox box) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos base = hutCenter.offset(dx, 0, dz);
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (edge) {
                    place(level, box, base, palette.wall.defaultBlockState());
                    place(level, box, base.above(1), palette.wall.defaultBlockState());
                    place(level, box, base.above(2), palette.wall.defaultBlockState());
                } else {
                    place(level, box, base, palette.floor.defaultBlockState());
                    place(level, box, base.above(1), Blocks.AIR.defaultBlockState());
                    place(level, box, base.above(2), Blocks.AIR.defaultBlockState());
                }
            }
        }

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                place(level, box, hutCenter.offset(dx, 3, dz), palette.roof.defaultBlockState());
            }
        }

        placeDoor(level, box, hutCenter, palette, doorFacing);
        placeWindows(level, box, hutCenter, doorFacing);
        placeInteriorLight(level, box, hutCenter, palette, doorFacing);
    }

    /** Puerta de verdad (se puede abrir), no solo un hueco en la pared. */
    private static void placeDoor(WorldGenLevel level, BoundingBox box, BlockPos hutCenter, FactionPalette palette, Direction doorFacing) {
        BlockPos doorBase = hutCenter.relative(doorFacing, 2);
        place(level, box, doorBase, palette.floor.defaultBlockState());

        BlockState lower = palette.door.defaultBlockState()
                .setValue(DoorBlock.FACING, doorFacing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.POWERED, false);
        BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        place(level, box, doorBase.above(1), lower);
        place(level, box, doorBase.above(2), upper);
    }

    /** Ventanas de vidrio en las dos paredes laterales (perpendiculares a la puerta). */
    private static void placeWindows(WorldGenLevel level, BoundingBox box, BlockPos hutCenter, Direction doorFacing) {
        Direction sideA = doorFacing.getClockWise();
        Direction sideB = doorFacing.getCounterClockWise();
        place(level, box, hutCenter.relative(sideA, 2).above(1), Blocks.GLASS_PANE.defaultBlockState());
        place(level, box, hutCenter.relative(sideB, 2).above(1), Blocks.GLASS_PANE.defaultBlockState());
    }

    /** Antes esto era una lampara parada en el piso; ahora cuelga del techo o va en la pared del fondo. */
    private static void placeInteriorLight(WorldGenLevel level, BoundingBox box, BlockPos hutCenter, FactionPalette palette, Direction doorFacing) {
        if (palette.light == Blocks.LANTERN || palette.light == Blocks.SOUL_LANTERN) {
            BlockState hanging = palette.light.defaultBlockState().setValue(LanternBlock.HANGING, true);
            place(level, box, hutCenter.above(2), hanging);
            return;
        }
        BlockPos wallLightPos = hutCenter.relative(doorFacing.getOpposite(), 1).above(1);
        if (palette.light == Blocks.END_ROD) {
            place(level, box, wallLightPos, Blocks.END_ROD.defaultBlockState().setValue(DirectionalBlock.FACING, doorFacing));
        } else {
            place(level, box, wallLightPos, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, doorFacing));
        }
    }

    private static void fillFlavorChest(WorldGenLevel level, BlockPos center, Faction faction) {
        BlockPos chestPos = center.offset(0, 0, 2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (blockEntity instanceof ChestBlockEntity chest) {
            Item[] items = flavorItems(faction);
            chest.setItem(0, new ItemStack(items[0], 2));
            chest.setItem(1, new ItemStack(items[1], 1));
        }
    }

    private static Item[] flavorItems(Faction faction) {
        return switch (faction) {
            case PATER -> new Item[]{Items.BONE, Items.GUNPOWDER};
            case FILIS -> new Item[]{Items.WHEAT, Items.APPLE};
            case MEIDRIS -> new Item[]{Items.OAK_SAPLING, Items.MOSS_BLOCK};
            case LUNA -> new Item[]{Items.BOOK, Items.LAPIS_LAZULI};
            case DEIROS -> new Item[]{Items.MAGMA_CREAM, Items.BLAZE_POWDER};
            case TEMPO -> new Item[]{Items.ENDER_PEARL, Items.CHORUS_FRUIT};
        };
    }

    private static void spawnNpcs(WorldGenLevel level, BlockPos center, Faction faction, RandomSource random) {
        double[] angles = {90, 210, 330};
        for (double angleDeg : angles) {
            double rad = Math.toRadians(angleDeg);
            double nx = center.getX() + 0.5 + Math.cos(rad) * (HUT_RING_RADIUS - 3.0);
            double nz = center.getZ() + 0.5 + Math.sin(rad) * (HUT_RING_RADIUS - 3.0);
            FactionNpcEntity npc = ModEntityTypes.FACTION_NPC.get().create(level.getLevel());
            if (npc != null) {
                npc.setFaction(faction);
                npc.moveTo(nx, center.getY(), nz, random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(npc);
            }
        }
    }
}
