// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.boss;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Los seis dioses como jefes (huevo de aparicion en el creativo): enormes (3 a 4 veces el jugador), vuelan, tienen barra de jefe y cada uno un
 * poder propio que usa cada pocos segundos: Pater lanza rayos, Deiros una lluvia de bolas de fuego, Meidris enreda con raices venenosas, Filis se
 * cura y marchita, Luna trae oscuridad y marchitamiento y Tempo se teletransporta y ciega. Los modelos son de cubos (ver GodModelData).
 */
public class GodBossEntity extends Monster {
    private final ServerBossEvent bossEvent;
    private final String god;
    private int abilityCooldown = 100;

    public GodBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        this.god = key == null ? "pater" : key.getPath().replace("god_", "");
        this.bossEvent = new ServerBossEvent(Component.translatable("entity.tcorigenes.god_" + this.god), colorOf(this.god),
                BossEvent.BossBarOverlay.NOTCHED_10);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 1000;
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    private static BossEvent.BossBarColor colorOf(String god) {
        return switch (god) {
            case "pater" -> BossEvent.BossBarColor.WHITE;
            case "deiros" -> BossEvent.BossBarColor.RED;
            case "meidris" -> BossEvent.BossBarColor.YELLOW;
            case "filis" -> BossEvent.BossBarColor.GREEN;
            case "luna" -> BossEvent.BossBarColor.BLUE;
            default -> BossEvent.BossBarColor.PURPLE;
        };
    }

    public String god() {
        return this.god;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 5_000_000.0) // son dioses: millones de vida (AttributeFix sube el tope de vida)
                .add(Attributes.ATTACK_DAMAGE, 500.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.5);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ChaseGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Vuela hacia su blanco y lo golpea cuando lo tiene a mano. */
    private static final class ChaseGoal extends Goal {
        private final GodBossEntity boss;
        private int attackDelay;

        ChaseGoal(GodBossEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return boss.getTarget() != null && boss.getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target == null) {
                return;
            }
            boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double reach = boss.getBbWidth() * 0.5 + target.getBbWidth() + 2.5;
            if (boss.distanceToSqr(target) > reach * reach) {
                boss.getMoveControl().setWantedPosition(target.getX(), target.getY() + 2.0, target.getZ(), 1.0);
            }
            if (--attackDelay <= 0 && boss.distanceToSqr(target) <= (reach + 1.5) * (reach + 1.5)) {
                attackDelay = 24;
                boss.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                boss.doHurtTarget(target);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        LivingEntity target = this.getTarget();
        if (target == null) {
            // sin blanco flota a poca altura del suelo
            if (this.level().getBlockState(this.blockPosition().below(3)).isAir()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.01, 0.0));
            }
            return;
        }
        if (--this.abilityCooldown <= 0 && this.distanceToSqr(target) < 40.0 * 40.0) {
            this.abilityCooldown = 90 + this.random.nextInt(60);
            useAbility(target);
        }
    }

    private void useAbility(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        AABB near = this.getBoundingBox().inflate(24.0);
        switch (this.god) {
            case "pater" -> { // rayos del Creador sobre los jugadores cercanos
                for (Player player : level.getEntitiesOfClass(Player.class, near, p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
                    for (int i = 0; i < 3; i++) {
                        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                        if (bolt != null) {
                            bolt.moveTo(player.getX() + (this.random.nextDouble() - 0.5) * 8, player.getY(), player.getZ() + (this.random.nextDouble() - 0.5) * 8);
                            level.addFreshEntity(bolt);
                        }
                    }
                }
            }
            case "deiros" -> { // lluvia de bolas de fuego
                for (int i = 0; i < 6; i++) {
                    Vec3 from = this.position().add((this.random.nextDouble() - 0.5) * 8, this.getBbHeight() * 0.7, (this.random.nextDouble() - 0.5) * 8);
                    Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from);
                    SmallFireball fireball = new SmallFireball(level, this, dir.x, dir.y, dir.z);
                    fireball.setPos(from.x, from.y, from.z);
                    level.addFreshEntity(fireball);
                }
            }
            case "meidris" -> { // raices venenosas
                for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(14.0), p -> p.isAlive() && !p.isCreative())) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                    player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, player.getX(), player.getY() + 0.5, player.getZ(), 30, 0.6, 0.5, 0.6, 0.02);
                }
            }
            case "filis" -> { // se cura y marchita a quien tiene cerca
                this.heal(this.getMaxHealth() * 0.04F);
                level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() * 0.7, this.getZ(), 16, 1.5, 1.5, 1.5, 0.02);
                for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0), p -> p.isAlive() && !p.isCreative())) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                }
            }
            case "luna" -> { // oscuridad y marchitamiento
                for (Player player : level.getEntitiesOfClass(Player.class, near, p -> p.isAlive() && !p.isCreative())) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                    level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.8, 0.5, 0.03);
                }
            }
            default -> { // Tempo: aparece al lado del blanco y lo ciega
                Vec3 spot = target.position().add((this.random.nextDouble() - 0.5) * 12, 3.0, (this.random.nextDouble() - 0.5) * 12);
                this.teleportTo(spot.x, spot.y, spot.z);
                for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(10.0), p -> p.isAlive() && !p.isCreative())) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                }
            }
        }
        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    // ---- barra de jefe
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ---- es un dios: no despawnea, no lo empuja nada y no recibe daño de caida, fuego ni ahogo
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
    }
}
