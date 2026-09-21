// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Marcador visual de un punto debil: una "carga ignea" roja que flota pegada al enemigo marcado
 * (ver WeakPointAnchor para donde). No es un proyectil: no explota, no colisiona, no se guarda.
 * Solo se dibuja para su dueño (ver el renderer). Vive hasta que el enemigo muere o vence el tiempo.
 */
public class WeakPointEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(WeakPointEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> TARGET =
            SynchedEntityData.defineId(WeakPointEntity.class, EntityDataSerializers.INT);

    private int maxAge = 80;

    public WeakPointEntity(EntityType<? extends WeakPointEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void bind(UUID owner, LivingEntity target, int lifetimeTicks) {
        this.entityData.set(OWNER, Optional.of(owner));
        this.entityData.set(TARGET, target.getId());
        this.maxAge = lifetimeTicks;
        this.setPos(WeakPointAnchor.of(target));
    }

    public int getTargetId() {
        return this.entityData.get(TARGET);
    }

    public UUID getOwnerId() {
        return this.entityData.get(OWNER).orElse(null);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(TARGET, -1);
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = this.level().getEntity(this.entityData.get(TARGET));
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }
        Vec3 anchor = WeakPointAnchor.of(living);
        this.setPos(anchor);
        if (!this.level().isClientSide() && this.tickCount > this.maxAge) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
