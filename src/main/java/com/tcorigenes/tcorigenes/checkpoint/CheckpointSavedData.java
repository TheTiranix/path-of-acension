// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Lista de puntos de guardado compartida por TODO el mundo (no por dimension): se guarda en el overworld. */
public final class CheckpointSavedData extends SavedData {
    private static final String KEY = "tcorigenes_checkpoints";

    final List<Checkpoint> checkpoints = new ArrayList<>();

    public static CheckpointSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CheckpointSavedData::load, CheckpointSavedData::new, KEY);
    }

    private static CheckpointSavedData load(CompoundTag tag) {
        CheckpointSavedData data = new CheckpointSavedData();
        ListTag list = tag.getList("checkpoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.checkpoints.add(Checkpoint.load(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Checkpoint checkpoint : this.checkpoints) {
            list.add(checkpoint.save(new CompoundTag()));
        }
        tag.put("checkpoints", list);
        return tag;
    }
}
