// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability;

import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Espiritu del Guerrero Anima (habilidad "Espiritu Anima"): un genio de la lampara sin piernas (solo torso, brazos y cabeza) que
 * levita en el lugar donde se invoco mientras dura la habilidad. Los enemigos no pueden elegirlo de objetivo y no recibe daño de
 * nada. Hace una animacion al aparecer (crece con un estallido de particulas) y otra al irse. Su color depende de la raza del
 * jugador (ver AnimaSpiritTracker). Golpea a los mobs hostiles cercanos igual que el pulso original: 4 de daño cada 10 ticks en
 * 4 bloques.
 */
public class AnimaSpiritEntity extends Mob {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(AnimaSpiritEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REMAINING = SynchedEntityData.defineId(AnimaSpiritEntity.class, EntityDataSerializers.INT);
    public static final int APPEAR_TICKS = 20;
    public static final int VANISH_TICKS = 20;

    public AnimaSpiritEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setNoAi(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COLOR, 0x55D6FF);
        this.entityData.define(REMAINING, 300);
    }

    private java.util.UUID ownerId;

    /** Dueño del espiritu: su daño escala con el nivel de ese jugador (ver PlayerLevel). */
    public void setOwner(net.minecraft.world.entity.player.Player owner) {
        this.ownerId = owner.getUUID();
    }

    private float damage(ServerLevel level) {
        net.minecraft.world.entity.player.Player owner = this.ownerId == null ? null : level.getPlayerByUUID(this.ownerId);
        int playerLevel = owner == null ? 1 : com.tcorigenes.tcorigenes.progression.PlayerLevel.of(owner);
        return 3.0F + playerLevel;
    }

    public void setup(int rgb, int durationTicks) {
        this.entityData.set(COLOR, rgb);
        this.entityData.set(REMAINING, durationTicks);
    }

    public int color() {
        return this.entityData.get(COLOR);
    }

    public int remaining() {
        return this.entityData.get(REMAINING);
    }

    /** Se va con su animacion: el tracker lo usa cuando el jugador invoca otro o se desconecta. */
    public void dismiss() {
        if (remaining() > VANISH_TICKS) {
            this.entityData.set(REMAINING, VANISH_TICKS);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0, 0.0, 0.0);
        if (this.level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        int left = remaining() - 1;
        this.entityData.set(REMAINING, left);
        float r = ((color() >> 16) & 255) / 255.0F;
        float g = ((color() >> 8) & 255) / 255.0F;
        float b = (color() & 255) / 255.0F;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), 1.2F);
        if (this.tickCount == 1) {
            // aparicion: estallido de particulas del color del genio y un anillo de luz
            level.sendParticles(dust, getX(), getY() + 1.0, getZ(), 60, 0.5, 0.8, 0.5, 0.08);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 24, 0.6, 0.9, 0.6, 0.04);
        }
        if (left == VANISH_TICKS) {
            level.sendParticles(dust, getX(), getY() + 1.0, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        }
        if (left <= 0) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            this.discard();
            return;
        }
        if (this.tickCount % 3 == 0) {
            level.sendParticles(dust, getX(), getY() + 0.2, getZ(), 2, 0.2, 0.1, 0.2, 0.0);
        }
        if (this.tickCount > APPEAR_TICKS && left > VANISH_TICKS && this.tickCount % 10 == 0) {
            AABB area = new AABB(this.blockPosition()).inflate(4.0);
            List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, area, e -> e instanceof Monster && e.isAlive());
            net.minecraft.world.entity.player.Player owner = this.ownerId == null ? null : level.getPlayerByUUID(this.ownerId);
            DamageSource source = owner == null ? this.damageSources().magic() : this.damageSources().indirectMagic(this, owner);
            float damage = damage(level);
            enemies.forEach(e -> e.hurt(source, damage));
            if (!enemies.isEmpty()) {
                // gira hacia el enemigo mas cercano
                LivingEntity near = enemies.get(0);
                double dx = near.getX() - getX();
                double dz = near.getZ() - getZ();
                this.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
                this.yRotO = this.getYRot();
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
            }
        }
    }

    // ---- inmune a todo y fuera del radar de los enemigos
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void registerGoals() {
    }
}
