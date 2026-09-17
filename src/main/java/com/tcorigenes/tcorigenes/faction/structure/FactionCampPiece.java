package com.tcorigenes.tcorigenes.faction.structure;

import com.tcorigenes.tcorigenes.faction.Faction;
import com.tcorigenes.tcorigenes.faction.ModStructurePieceTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** La pieza real de la Structure (ver FactionCampStructure); la construccion de bloques vive en FactionCampBuilder. */
public class FactionCampPiece extends StructurePiece {
    private final Faction faction;
    private final BlockPos campOrigin;

    public FactionCampPiece(BlockPos origin, Faction faction) {
        super(ModStructurePieceTypes.FACTION_CAMP, 0, makeBoundingBox(origin));
        this.faction = faction;
        this.campOrigin = origin;
    }

    public FactionCampPiece(CompoundTag tag) {
        super(ModStructurePieceTypes.FACTION_CAMP, tag);
        this.faction = Faction.valueOf(tag.getString("TcFaction"));
        this.campOrigin = new BlockPos(tag.getInt("TcX"), tag.getInt("TcY"), tag.getInt("TcZ"));
    }

    /**
     * El Y de "origin" es solo una estimacion (ver FactionCampBuilder.findOverworldGround); el
     * Y real puede terminar bastante distinto una vez que postProcess recalcula sobre terreno ya
     * generado. Por eso el rango vertical de la bounding box es generoso (todo el alto del mundo
     * en vez de un margen chico), para que ese recalculo no quede afuera del area reservada.
     */
    private static BoundingBox makeBoundingBox(BlockPos origin) {
        return new BoundingBox(origin.getX() - 16, -64, origin.getZ() - 16,
                origin.getX() + 16, 320, origin.getZ() + 16);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("TcFaction", faction.name());
        tag.putInt("TcX", campOrigin.getX());
        tag.putInt("TcY", campOrigin.getY());
        tag.putInt("TcZ", campOrigin.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                             RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockPos center = faction == Faction.DEIROS
                ? FactionCampBuilder.findNetherGround(level, campOrigin)
                : FactionCampBuilder.findOverworldGround(level, campOrigin);
        if (center != null) {
            FactionCampBuilder.build(level, center, faction, random, box);
        }
    }
}
