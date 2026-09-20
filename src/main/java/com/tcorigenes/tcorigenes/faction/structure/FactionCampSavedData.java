// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.faction.structure;

import com.tcorigenes.tcorigenes.faction.Faction;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Recuerda donde genero cada campamento de faccion en este mundo (uno por dimension, la misma
 * separacion que usa DimensionDataStorage), para que /factionnpc locate pueda encontrarlos sin
 * tener que escanear bloques. Solo sabe de los que se generaron DESPUES de agregar este sistema
 * (no hay forma de indexar retroactivamente chunks ya generados).
 */
public class FactionCampSavedData extends SavedData {
    private static final String ID = "tcorigenes_faction_camps";
    private final Map<Faction, List<BlockPos>> camps = new EnumMap<>(Faction.class);

    public static FactionCampSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FactionCampSavedData::load, FactionCampSavedData::new, ID);
    }

    public void addCamp(Faction faction, BlockPos pos) {
        camps.computeIfAbsent(faction, f -> new ArrayList<>()).add(pos.immutable());
        setDirty();
    }

    @Nullable
    public BlockPos findNearest(Faction faction, BlockPos from) {
        List<BlockPos> list = camps.get(faction);
        if (list == null || list.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos pos : list) {
            double distSq = pos.distSqr(from);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = pos;
            }
        }
        return best;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<Faction, List<BlockPos>> entry : camps.entrySet()) {
            ListTag list = new ListTag();
            for (BlockPos pos : entry.getValue()) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", pos.getX());
                posTag.putInt("y", pos.getY());
                posTag.putInt("z", pos.getZ());
                list.add(posTag);
            }
            tag.put(entry.getKey().name(), list);
        }
        return tag;
    }

    public static FactionCampSavedData load(CompoundTag tag) {
        FactionCampSavedData data = new FactionCampSavedData();
        for (Faction faction : Faction.values()) {
            if (tag.contains(faction.name())) {
                ListTag list = tag.getList(faction.name(), Tag.TAG_COMPOUND);
                List<BlockPos> positions = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag posTag = list.getCompound(i);
                    positions.add(new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
                }
                data.camps.put(faction, positions);
            }
        }
        return data;
    }
}
