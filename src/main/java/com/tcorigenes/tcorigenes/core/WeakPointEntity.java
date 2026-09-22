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
    /** Si esta marca prioriza "lo mas arriba posible" o "lo mas adelante posible" (ver WeakPointModelAnchor).
     *  Se decide una sola vez al crear la marca (aca, sincronizado) para que no cambie de un frame a otro. */
    private static final EntityDataAccessor<Boolean> PRIORITIZE_HEIGHT =
            SynchedEntityData.defineId(WeakPointEntity.class, EntityDataSerializers.BOOLEAN);

    private int maxAge = 80;
    /** Punto y radio sobre el MODELO del mob que calcula el cliente del dueño (el servidor no tiene modelos). */
    private Vec3 modelAim;
    private double modelRadius = -1;
    private long modelAimAt = -1000;

    public WeakPointEntity(EntityType<? extends WeakPointEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void bind(UUID owner, LivingEntity target, int lifetimeTicks) {
        this.entityData.set(OWNER, Optional.of(owner));
        this.entityData.set(TARGET, target.getId());
        this.entityData.set(PRIORITIZE_HEIGHT, target.getRandom().nextBoolean());
        this.maxAge = lifetimeTicks;
        this.setPos(WeakPointAnchor.of(target));
    }

    public boolean isPriorityHeight() {
        return this.entityData.get(PRIORITIZE_HEIGHT);
    }

    /** Lo manda el cliente del dueño (ver WeakPointAimPacket); ya viene validado. */
    public void setModelAim(Vec3 aim, double radius, long gameTime) {
        this.modelAim = aim;
        this.modelRadius = radius;
        this.modelAimAt = gameTime;
    }

    private boolean hasFreshModelAim() {
        return this.modelAim != null && this.level().getGameTime() - this.modelAimAt <= 30;
    }

    /** Donde hay que acertar: el punto del modelo si el cliente lo reporto hace poco, si no el de la hitbox. */
    public Vec3 getAimPoint(LivingEntity target) {
        if (hasFreshModelAim()) {
            return this.modelAim;
        }
        return WeakPointAnchor.of(target);
    }

    /** Radio de acierto: el que calculo el modelo (proporcional a su tamaño) si esta fresco, si no el de respaldo. */
    public double getAimRadius(LivingEntity target) {
        if (hasFreshModelAim() && this.modelRadius > 0) {
            return this.modelRadius;
        }
        return WeakPointManager.fallbackAimRadius(target);
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
        this.entityData.define(PRIORITIZE_HEIGHT, false);
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
