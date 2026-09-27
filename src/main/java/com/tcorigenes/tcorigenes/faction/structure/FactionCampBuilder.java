// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Logica de construccion del campamento, compartida por FactionCampPiece (la Structure real).
 * Cada llamada a place() respeta el BoundingBox de la porcion de chunk que se esta procesando
 * (postProcess se llama una vez por cada chunk que toca el campamento), y el spawn de NPCs/cofre
 * solo se ejecuta una vez, cuando el chunk que contiene el centro es el que se esta procesando.
 *
 * Pedido de alejandr0: aldeas mas grandes (no solo 3 casas) y casas mas detalladas. Ahora son 6
 * cabañas mas grandes, con techo a 4 aguas (aleros de escalera + capas de losa en piramide,
 * rematado con un adorno arriba), mas ventanas, y cama + mesa de crafteo adentro.
 */
public final class FactionCampBuilder {
    private static final int HUT_RING_RADIUS = 15;
    private static final int FLOOR_RADIUS = 20;
    /** Media distancia del piso al muro de la cabaña (7x7 de base). */
    private static final int HUT_HALF = 3;
    private static final int WALL_HEIGHT = 4;
    private static final double[] HUT_ANGLES = {90, 150, 210, 270, 330, 30};

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

        for (double angleDeg : HUT_ANGLES) {
            double rad = Math.toRadians(angleDeg);
            int hx = center.getX() + (int) Math.round(Math.cos(rad) * HUT_RING_RADIUS);
            int hz = center.getZ() + (int) Math.round(Math.sin(rad) * HUT_RING_RADIUS);
            BlockPos hutCenter = new BlockPos(hx, center.getY(), hz);
            Direction doorFacing = nearestCardinal(center.getX() - hx, center.getZ() - hz);
            buildHut(level, hutCenter, palette, doorFacing, box);
            placePath(level, center, hutCenter, palette, box);
        }
    }

    private static Direction nearestCardinal(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Sendero de losas entre la puerta de la cabaña y la plaza central, para que la aldea se vea conectada. */
    private static void placePath(WorldGenLevel level, BlockPos center, BlockPos hutCenter, FactionPalette palette, BoundingBox box) {
        BlockPos from = hutCenter.relative(nearestCardinal(center.getX() - hutCenter.getX(), center.getZ() - hutCenter.getZ()), HUT_HALF + 2);
        int steps = Math.max(Math.abs(center.getX() - from.getX()), Math.abs(center.getZ() - from.getZ()));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int px = (int) Math.round(from.getX() + (center.getX() - from.getX()) * t);
            int pz = (int) Math.round(from.getZ() + (center.getZ() - from.getZ()) * t);
            place(level, box, new BlockPos(px, hutCenter.getY() - 1, pz), palette.accent.defaultBlockState());
        }
    }

    private static void buildHut(WorldGenLevel level, BlockPos hutCenter, FactionPalette palette, Direction doorFacing, BoundingBox box) {
        for (int dx = -HUT_HALF; dx <= HUT_HALF; dx++) {
            for (int dz = -HUT_HALF; dz <= HUT_HALF; dz++) {
                BlockPos base = hutCenter.offset(dx, 0, dz);
                boolean edge = Math.abs(dx) == HUT_HALF || Math.abs(dz) == HUT_HALF;
                if (edge) {
                    for (int y = 0; y < WALL_HEIGHT; y++) {
                        place(level, box, base.above(y), palette.wall.defaultBlockState());
                    }
                    boolean corner = Math.abs(dx) == HUT_HALF && Math.abs(dz) == HUT_HALF;
                    if (corner) {
                        // Vigas de esquina a la vista (detalle: se nota la estructura, no es un cubo liso).
                        for (int y = 0; y < WALL_HEIGHT; y++) {
                            place(level, box, base.above(y), palette.accent.defaultBlockState());
                        }
                    }
                } else {
                    place(level, box, base, palette.floor.defaultBlockState()); // y=0: piso
                    for (int y = 1; y < WALL_HEIGHT; y++) { // y=1..3: aire (ojo: arrancar en 1, no en 0,
                        place(level, box, base.above(y), Blocks.AIR.defaultBlockState()); // o se borra el piso de arriba)
                    }
                }
            }
        }

        buildRoof(level, hutCenter, palette, box);
        placeDoor(level, box, hutCenter, palette, doorFacing);
        placeWindows(level, box, hutCenter, doorFacing);
        placeInteriorLight(level, box, hutCenter, palette, doorFacing);
        placeFurniture(level, box, hutCenter, palette, doorFacing);
    }

    /**
     * Techo a cuatro aguas, en capas escalonadas (piramide/ziggurat): cada capa es un alero de
     * escaleras (orientadas hacia afuera) CON el interior relleno de losa en la MISMA altura, para
     * que apoye directo sobre la capa de abajo sin dejar un hueco de aire en el medio (el bug que
     * hacia "flotar" el techo). Se achica un anillo por capa hasta terminar en un adorno (acento).
     */
    private static void buildRoof(WorldGenLevel level, BlockPos hutCenter, FactionPalette palette, BoundingBox box) {
        int eave = HUT_HALF + 1;
        int roofY = WALL_HEIGHT;
        for (int layer = 0; layer <= eave; layer++) {
            int radius = eave - layer;
            int y = roofY + layer;
            if (radius == 0) {
                place(level, box, hutCenter.offset(0, y, 0), palette.accent.defaultBlockState());
                break;
            }
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = hutCenter.offset(dx, y, dz);
                    boolean onRing = Math.max(Math.abs(dx), Math.abs(dz)) == radius;
                    if (!onRing) {
                        place(level, box, pos, palette.roof.defaultBlockState()); // relleno solido: sin huecos
                    } else if (Math.abs(dx) == radius && Math.abs(dz) == radius) {
                        place(level, box, pos, palette.roof.defaultBlockState()); // esquina: losa lisa
                    } else if (Math.abs(dx) == radius) {
                        place(level, box, pos, palette.wallStairs.defaultBlockState()
                                .setValue(StairBlock.FACING, dx > 0 ? Direction.EAST : Direction.WEST));
                    } else {
                        place(level, box, pos, palette.wallStairs.defaultBlockState()
                                .setValue(StairBlock.FACING, dz > 0 ? Direction.SOUTH : Direction.NORTH));
                    }
                }
            }
        }
    }

    /** Puerta de verdad (se puede abrir), no solo un hueco en la pared. */
    private static void placeDoor(WorldGenLevel level, BoundingBox box, BlockPos hutCenter, FactionPalette palette, Direction doorFacing) {
        BlockPos doorBase = hutCenter.relative(doorFacing, HUT_HALF); // sobre la pared misma, no adentro
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

    /** Dos ventanas de vidrio por pared lateral (perpendiculares a la puerta), no solo una. Vidrio macizo,
     *  no paneles: los paneles quedaban como una linea fina en el hueco de la pared en vez de una ventana. */
    private static void placeWindows(WorldGenLevel level, BoundingBox box, BlockPos hutCenter, Direction doorFacing) {
        Direction sideA = doorFacing.getClockWise();
        Direction sideB = doorFacing.getCounterClockWise();
        for (Direction side : new Direction[] {sideA, sideB}) {
            for (int offset : new int[] {-1, 1}) {
                BlockPos pos = hutCenter.relative(side, HUT_HALF).relative(doorFacing, offset);
                place(level, box, pos.above(1), Blocks.GLASS.defaultBlockState());
                place(level, box, pos.above(2), Blocks.GLASS.defaultBlockState());
            }
        }
    }

    /** Antes esto era una lampara parada en el piso; ahora cuelga del techo o va en la pared del fondo. */
    private static void placeInteriorLight(WorldGenLevel level, BoundingBox box, BlockPos hutCenter, FactionPalette palette, Direction doorFacing) {
        if (palette.light == Blocks.LANTERN || palette.light == Blocks.SOUL_LANTERN) {
            BlockState hanging = palette.light.defaultBlockState().setValue(LanternBlock.HANGING, true);
            place(level, box, hutCenter.above(WALL_HEIGHT - 1), hanging);
            return;
        }
        BlockPos wallLightPos = hutCenter.relative(doorFacing.getOpposite(), HUT_HALF - 1).above(1);
        if (palette.light == Blocks.END_ROD) {
            place(level, box, wallLightPos, Blocks.END_ROD.defaultBlockState().setValue(DirectionalBlock.FACING, doorFacing));
        } else {
            place(level, box, wallLightPos, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, doorFacing));
        }
    }

    /** Cama contra la pared del fondo y mesa de crafteo cerca de la puerta: la cabaña ya no esta vacia por dentro. */
    private static void placeFurniture(WorldGenLevel level, BoundingBox box, BlockPos hutCenter, FactionPalette palette, Direction doorFacing) {
        Direction sideA = doorFacing.getClockWise();
        Direction back = doorFacing.getOpposite();
        BlockPos foot = hutCenter.relative(back, HUT_HALF - 2).relative(sideA, HUT_HALF - 1);
        BlockPos head = foot.relative(back, 1);
        place(level, box, foot, palette.bed.defaultBlockState().setValue(BedBlock.FACING, back).setValue(BedBlock.PART, BedPart.FOOT));
        place(level, box, head, palette.bed.defaultBlockState().setValue(BedBlock.FACING, back).setValue(BedBlock.PART, BedPart.HEAD));

        BlockPos table = hutCenter.relative(doorFacing.getCounterClockWise(), HUT_HALF - 1).relative(doorFacing, HUT_HALF - 2);
        place(level, box, table, Blocks.CRAFTING_TABLE.defaultBlockState());
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
        for (double angleDeg : HUT_ANGLES) {
            double rad = Math.toRadians(angleDeg);
            double nx = center.getX() + 0.5 + Math.cos(rad) * (HUT_RING_RADIUS - HUT_HALF - 1.0);
            double nz = center.getZ() + 0.5 + Math.sin(rad) * (HUT_RING_RADIUS - HUT_HALF - 1.0);
            FactionNpcEntity npc = ModEntityTypes.FACTION_NPC.get().create(level.getLevel());
            if (npc != null) {
                npc.setFaction(faction);
                npc.assignRandomName(random);
                npc.moveTo(nx, center.getY(), nz, random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(npc);
            }
        }
    }
}
