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
    /** Ya se tomo el save inicial automatico de este mundo (ver CheckpointManager#onLogin). */
    boolean initialSnapshotTaken = false;
    /** Tick (overworld) del ultimo save de cama creado: cooldown de 1 dia entre saves. Long.MIN_VALUE = ninguno. */
    long lastSaveTick = Long.MIN_VALUE;
    /** Camas recien colocadas ("dim|pos" -> tick): hay que esperar 1 dia para poder guardar en ellas. */
    /** Cama que es el punto de guardado vigente de cada jugador (la ultima que coloco): "dim|pos". */
    final java.util.Map<java.util.UUID, String> saveBed = new java.util.HashMap<>();
    final java.util.Map<String, Long> bedPlacedAt = new java.util.HashMap<>();

    public static CheckpointSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CheckpointSavedData::load, CheckpointSavedData::new, KEY);
    }

    private static CheckpointSavedData load(CompoundTag tag) {
        CheckpointSavedData data = new CheckpointSavedData();
        ListTag list = tag.getList("checkpoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.checkpoints.add(Checkpoint.load(list.getCompound(i)));
        }
        data.initialSnapshotTaken = tag.getBoolean("initial_snapshot");
        if (tag.contains("last_save")) {
            data.lastSaveTick = tag.getLong("last_save");
        }
        ListTag saveBeds = tag.getList("save_beds", Tag.TAG_COMPOUND);
        for (int i = 0; i < saveBeds.size(); i++) {
            data.saveBed.put(saveBeds.getCompound(i).getUUID("o"), saveBeds.getCompound(i).getString("k"));
        }
        ListTag beds = tag.getList("beds", Tag.TAG_COMPOUND);
        for (int i = 0; i < beds.size(); i++) {
            data.bedPlacedAt.put(beds.getCompound(i).getString("k"), beds.getCompound(i).getLong("t"));
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
        tag.putBoolean("initial_snapshot", this.initialSnapshotTaken);
        if (this.lastSaveTick != Long.MIN_VALUE) {
            tag.putLong("last_save", this.lastSaveTick);
        }
        ListTag beds = new ListTag();
        this.bedPlacedAt.forEach((k, t) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("k", k);
            entry.putLong("t", t);
            beds.add(entry);
        });
        tag.put("beds", beds);
        ListTag saveBeds = new ListTag();
        this.saveBed.forEach((owner, key) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("o", owner);
            entry.putString("k", key);
            saveBeds.add(entry);
        });
        tag.put("save_beds", saveBeds);
        return tag;
    }
}
