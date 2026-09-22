// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Un punto de guardado: una cama que un jugador coloco. Mientras {@code supersededAtTick} sea -1 (o no haya
 * llegado todavia), es un punto de guardado ACTIVO y entra en la lista compartida de todo el mundo.
 */
public final class Checkpoint {
    public final UUID id;
    public final UUID owner;
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;
    public final long placedAtTick;
    /** Tick en el que deja de ser valido (lo reemplazo una cama nueva del mismo dueño hace 1 dia); -1 = nunca. */
    long supersededAtTick = -1;

    Checkpoint(UUID id, UUID owner, ResourceKey<Level> dimension, BlockPos pos, long placedAtTick) {
        this.id = id;
        this.owner = owner;
        this.dimension = dimension;
        this.pos = pos;
        this.placedAtTick = placedAtTick;
    }

    public boolean isActive(long now) {
        return this.supersededAtTick < 0 || now < this.supersededAtTick;
    }

    CompoundTag save(CompoundTag tag) {
        tag.putUUID("id", this.id);
        tag.putUUID("owner", this.owner);
        tag.putString("dimension", this.dimension.location().toString());
        tag.putLong("pos", this.pos.asLong());
        tag.putLong("placedAt", this.placedAtTick);
        tag.putLong("supersededAt", this.supersededAtTick);
        return tag;
    }

    static Checkpoint load(CompoundTag tag) {
        ResourceLocation dimLoc = ResourceLocation.tryParse(tag.getString("dimension"));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                dimLoc != null ? dimLoc : Level.OVERWORLD.location());
        Checkpoint checkpoint = new Checkpoint(tag.getUUID("id"), tag.getUUID("owner"), dimension,
                BlockPos.of(tag.getLong("pos")), tag.getLong("placedAt"));
        checkpoint.supersededAtTick = tag.contains("supersededAt") ? tag.getLong("supersededAt") : -1;
        return checkpoint;
    }
}
