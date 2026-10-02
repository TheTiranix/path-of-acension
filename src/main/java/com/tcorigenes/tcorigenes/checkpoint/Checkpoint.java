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
    /** false = la cama de este save ya no sirve como punto de reaparicion (el jugador puso otra), pero el save sigue en la lista y se puede cargar. */
    boolean respawnEnabled = true;
    /** Numero de save por orden de creacion (Save 1, Save 2...); no cambia aunque otros saves se borren. 0 = todavia sin asignar. */
    int number = 0;

    Checkpoint(UUID id, UUID owner, ResourceKey<Level> dimension, BlockPos pos, long placedAtTick) {
        this.id = id;
        this.owner = owner;
        this.dimension = dimension;
        this.pos = pos;
        this.placedAtTick = placedAtTick;
    }

    public int number() {
        return this.number;
    }

    /** Dia de juego (empezando en 1) en que se creo el save. */
    public long day() {
        return this.placedAtTick / 24000L + 1L;
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
        tag.putBoolean("respawn", this.respawnEnabled);
        tag.putInt("number", this.number);
        return tag;
    }

    static Checkpoint load(CompoundTag tag) {
        ResourceLocation dimLoc = ResourceLocation.tryParse(tag.getString("dimension"));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                dimLoc != null ? dimLoc : Level.OVERWORLD.location());
        Checkpoint checkpoint = new Checkpoint(tag.getUUID("id"), tag.getUUID("owner"), dimension,
                BlockPos.of(tag.getLong("pos")), tag.getLong("placedAt"));
        checkpoint.supersededAtTick = tag.contains("supersededAt") ? tag.getLong("supersededAt") : -1;
        checkpoint.respawnEnabled = !tag.contains("respawn") || tag.getBoolean("respawn");
        checkpoint.number = tag.getInt("number");
        return checkpoint;
    }
}
