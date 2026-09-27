// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.faction.entity;

import com.tcorigenes.tcorigenes.faction.Faction;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * NPC humanoide propio de las facciones (uno por dios, ver Faction). No es un Villager de
 * vanilla: entidad y modelo propios, distinguidos solo por textura/faccion. Todavia sin
 * comercio ni aldeas; comportamiento basico de deambular + defenderse si lo atacan.
 */
public class FactionNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> FACTION =
            SynchedEntityData.defineId(FactionNpcEntity.class, EntityDataSerializers.INT);
    private static final String NBT_FACTION = "Faction";

    /** Nombres al azar (pedido de alejandr0: sin relacion con la faccion, y con dos guiños puntuales adentro). */
    private static final List<String> NAMES = List.of(
            "Roberto Carlos", "Marley", "Felipe Pettinato", "Ramona Quispe", "Herminio Basualdo",
            "Casimira Ledesma", "Facundo Achaval", "Aurelia Montenegro", "Bartolo Yupanqui", "Delfina Roldan",
            "Eustaquio Villagra", "Fermina Sarmiento", "Godofredo Cardozo", "Higinia Paz", "Ireneo Quiroga",
            "Josefa Benitez", "Leocadio Funes", "Macaria Sosa", "Nemesio Aranda", "Olegaria Vera",
            "Prudencio Godoy", "Remigia Ontiveros", "Segismundo Chaparro", "Tiburcia Manrique", "Ubaldo Escalante",
            "Venancia Bracamonte", "Wenceslao Zarate", "Ximena Maldonado", "Anacleto Figueroa", "Baltazar Rivadeneira");

    public FactionNpcEntity(EntityType<? extends FactionNpcEntity> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FACTION, Faction.PATER.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public Faction getFaction() {
        return Faction.byOrdinalSafe(this.entityData.get(FACTION));
    }

    public void setFaction(Faction faction) {
        this.entityData.set(FACTION, faction.ordinal());
    }

    /** Nombre al azar (sin relacion con la faccion): llamar una sola vez, al spawnear (no en cada carga del save,
     *  el nombre ya elegido viaja solo en el NBT estandar de CustomName). */
    public void assignRandomName(RandomSource random) {
        setCustomName(Component.literal(NAMES.get(random.nextInt(NAMES.size()))));
        setCustomNameVisible(true);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                         @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag dataTag) {
        BlockPos pos = this.blockPosition();
        ResourceKey<Biome> biomeKey = level.getBiome(pos).unwrapKey().orElse(null);
        if (biomeKey != null) {
            Faction matched = Faction.fromBiome(biomeKey);
            if (matched != null) {
                setFaction(matched);
            }
        }
        assignRandomName(this.random);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_FACTION, getFaction().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_FACTION)) {
            setFaction(Faction.byOrdinalSafe(tag.getInt(NBT_FACTION)));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        // Deiros es el angel caido: vive en el Nether, el fuego no le hace nada.
        return getFaction() == Faction.DEIROS || super.fireImmune();
    }
}
