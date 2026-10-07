// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability.racial;

import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.ability.PlayerAbility;
import com.tcorigenes.tcorigenes.compat.MobScaling;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.RacialStateSyncPacket;
import com.tudominio.elementaldamage.ModDamageTypes;
import com.tudominio.elementaldamage.PendingElementalHits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

/**
 * Habilidades raciales (tecla J): Angel (impulso con las alas), Devoto (plegaria), Demonio (aura de fuego), Siervo de la Luna (corromper un mob),
 * Malnacido (golpe al piso) y Gigante Rocoso (agarrar y lanzar un bloque, como el Enderman). El "nivel del jugador" es su nivel de experiencia.
 * El servidor decide todo; el cliente solo recibe el estado visual (ver RacialStateSyncPacket).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class RacialAbilities {
    /** Habilidad que puede fallar sin gastar cooldown (sin blanco, sin bloque...). */
    public interface Racial extends PlayerAbility {
        boolean tryActivate(ServerPlayer player);

        @Override
        default void activate(ServerPlayer player) {
            tryActivate(player);
        }
    }

    // ----------------------------------------------------------------- constantes
    private static final int PRAYER_TICKS = 20 * 5;
    private static final int REGEN_TICKS = 20 * 4;
    private static final int WAVE_TICKS = 12;
    private static final double WAVE_RADIUS = 7.0;
    private static final int GRAB_TICKS = 20 * 3;
    private static final double GRAB_RANGE = 6.0;
    private static final double SLAM_RADIUS = 6.0;
    private static final int SLOW_TICKS = 20 * 3;
    private static final double GIANT_REACH = 4.5;
    private static final String SERVANT_KEY = "tc_servant_owner";
    private static final String HELD_KEY = "tc_giant_block";

    private static final Map<UUID, Prayer> PRAYING = new HashMap<>();
    private static final Map<UUID, Integer> AURA = new HashMap<>();
    private static final Map<UUID, java.util.Set<UUID>> WAVE_HIT = new HashMap<>();
    private static final Map<UUID, Grab> GRABS = new HashMap<>();
    private static final Map<UUID, UUID> THROWN = new HashMap<>(); // entidad del bloque lanzado -> dueño

    private record Prayer(int[] ticksLeft, Vec3 start) {
    }

    private static final class Grab {
        final UUID mob;
        int ticks = GRAB_TICKS;

        Grab(UUID mob) {
            this.mob = mob;
        }
    }

    private RacialAbilities() {
    }

    // ----------------------------------------------------------------- utilidades
    private static int level(Player player) {
        return com.tcorigenes.tcorigenes.progression.PlayerLevel.of(player);
    }

    private static void say(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("tcorigenes.racial." + key, args), true);
    }

    private static void sync(ServerPlayer player, int kind, int value) {
        Networking.sendToTrackingAndSelf(player, new RacialStateSyncPacket(player.getId(), kind, value));
    }

    private static boolean isServantOf(Entity entity, UUID owner) {
        return entity.getPersistentData().contains(SERVANT_KEY) && entity.getPersistentData().getString(SERVANT_KEY).equals(owner.toString());
    }

    private static boolean isServant(Entity entity) {
        return entity.getPersistentData().contains(SERVANT_KEY);
    }

    /** Blanco valido de un daño de habilidad: seres vivos que no son el dueño ni sus sirvientes ni mascotas ni otros jugadores. */
    private static boolean validVictim(ServerPlayer owner, LivingEntity target) {
        return target.isAlive() && target != owner && !(target instanceof Player) && !isServantOf(target, owner.getUUID())
                && !(target instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame());
    }

    private static void register(String id, int cooldownTicks, String icon, java.util.function.Function<ServerPlayer, Boolean> action) {
        AbilityRegistry.register(new Racial() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public int cooldownTicks() {
                return cooldownTicks;
            }

            @Override
            public ResourceLocation icon() {
                return ResourceLocation.withDefaultNamespace(icon);
            }

            @Override
            public boolean tryActivate(ServerPlayer player) {
                return action.apply(player);
            }
        });
    }

    public static void registerAll() {
        register("tcorigenes:impulso_angel", 20 * 20, "textures/item/feather.png", RacialAbilities::angelBoost);
        register("tcorigenes:plegaria_devoto", 20 * 60, "textures/item/golden_apple.png", RacialAbilities::pray);
        register("tcorigenes:aura_demonio", 20 * 45, "textures/item/blaze_powder.png", RacialAbilities::demonAura);
        register("tcorigenes:corrupcion_luna", 20 * 30, "textures/item/spider_eye.png", RacialAbilities::corrupt);
        register("tcorigenes:golpe_malnacido", 20 * 20, "textures/item/clay_ball.png", RacialAbilities::slam);
        register("tcorigenes:bloque_gigante", 20 * 15, "textures/item/flint.png", RacialAbilities::giantBlock);
    }

    // ----------------------------------------------------------------- Angel
    private static boolean angelBoost(ServerPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.5, 1.15, motion.z * 0.5);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 18, 0.5, 0.1, 0.5, 0.05);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.5, player.getZ(), 10, 0.4, 0.4, 0.4, 0.05);
            level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.2F, 1.2F);
        }
        return true;
    }

    // ----------------------------------------------------------------- Devoto
    private static boolean pray(ServerPlayer player) {
        if (PRAYING.containsKey(player.getUUID())) {
            return false;
        }
        PRAYING.put(player.getUUID(), new Prayer(new int[] {PRAYER_TICKS}, player.position()));
        sync(player, RacialStateSyncPacket.KIND_PRAYING, 1);
        say(player, "praying");
        return true;
    }

    private static void endPrayer(ServerPlayer player, boolean completed) {
        PRAYING.remove(player.getUUID());
        sync(player, RacialStateSyncPacket.KIND_PRAYING, 0);
        if (completed) {
            int amplifier = Math.min(4, level(player) / 10);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, amplifier, false, true));
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.4, 0.6, 0.4, 0.02);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
            }
        } else {
            say(player, "prayer_cancelled");
        }
    }

    private static boolean isPraying(Player player) {
        return PRAYING.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void blockAttack(AttackEntityEvent event) {
        if (isPraying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void blockUse(PlayerInteractEvent event) {
        if (event.isCancelable() && isPraying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        if (isPraying(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void blockItemUse(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && isPraying(player)) {
            event.setCanceled(true);
        }
    }

    // ----------------------------------------------------------------- Demonio
    private static boolean demonAura(ServerPlayer player) {
        if (AURA.containsKey(player.getUUID())) {
            return false;
        }
        AURA.put(player.getUUID(), 0);
        WAVE_HIT.put(player.getUUID(), new java.util.HashSet<>());
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 0.6F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.5F);
        return true;
    }

    /** Onda expansiva de fuego: un anillo que crece desde el demonio y quema una sola vez a cada ser vivo que alcanza (daño elemental de fuego). */
    private static void tickAura(ServerPlayer player, int elapsed) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = WAVE_RADIUS * (elapsed + 1) / WAVE_TICKS;
        for (int i = 0; i < 48; i++) {
            double angle = i * (Math.PI * 2 / 48);
            level.sendParticles(ParticleTypes.FLAME, player.getX() + Math.cos(angle) * radius, player.getY() + 0.3,
                    player.getZ() + Math.sin(angle) * radius, 1, 0.05, 0.1, 0.05, 0.02);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.LAVA, player.getX() + Math.cos(angle) * radius, player.getY() + 0.5,
                        player.getZ() + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        java.util.Set<UUID> hit = WAVE_HIT.computeIfAbsent(player.getUUID(), k -> new java.util.HashSet<>());
        float damage = 4.0F + level(player) * 0.8F;
        AABB box = player.getBoundingBox().inflate(radius, 2.0, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (validVictim(player, target) && !hit.contains(target.getUUID()) && target.distanceToSqr(player) <= radius * radius) {
                hit.add(target.getUUID());
                PendingElementalHits.hurtNow(target, player, ModDamageTypes.FIRE_ELEMENTAL, damage);
                target.setSecondsOnFire(4);
            }
        }
    }

    // ----------------------------------------------------------------- Siervo de la Luna
    private static boolean isBoss(Mob mob) {
        return mob.getType().is(Tags.EntityTypes.BOSSES) || mob.getMaxHealth() >= 150.0F || mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
    }

    private static int servantCount(ServerPlayer player) {
        int count = 0;
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob && mob.isAlive() && isServantOf(mob, player.getUUID())) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int servantLimit(Player player) {
        return 1 + level(player) / 10;
    }

    private static boolean corrupt(ServerPlayer player) {
        if (GRABS.containsKey(player.getUUID())) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(GRAB_RANGE));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, player.getBoundingBox().expandTowards(look.scale(GRAB_RANGE)).inflate(1.0),
                e -> e instanceof Mob && e.isAlive() && !e.isSpectator(), GRAB_RANGE * GRAB_RANGE);
        if (hit == null || !(hit.getEntity() instanceof Mob mob)) {
            say(player, "no_target");
            return false;
        }
        if (isServant(mob)) {
            say(player, "already_servant");
            return false;
        }
        if (isBoss(mob)) {
            say(player, "boss");
            return false;
        }
        if (MobScaling.levelOf(mob) > level(player)) {
            say(player, "too_strong", MobScaling.levelOf(mob));
            return false;
        }
        if (servantCount(player) >= servantLimit(player)) {
            say(player, "limit", servantLimit(player));
            return false;
        }
        mob.setNoAi(true);
        GRABS.put(player.getUUID(), new Grab(mob.getUUID()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.6F, 1.5F);
        return true;
    }

    private static void releaseGrab(ServerPlayer player, Mob mob) {
        GRABS.remove(player.getUUID());
        if (mob != null) {
            mob.setNoAi(false);
        }
    }

    private static void tickGrab(ServerPlayer player, Grab grab) {
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(grab.mob);
        if (!(entity instanceof Mob mob) || !mob.isAlive() || !player.isAlive() || mob.distanceTo(player) > GRAB_RANGE + 4.0) {
            releaseGrab(player, entity instanceof Mob m ? m : null);
            return;
        }
        // lo sostiene frente a el
        Vec3 hold = player.getEyePosition().add(player.getViewVector(1.0F).scale(2.0)).subtract(0.0, mob.getBbHeight() * 0.5, 0.0);
        mob.setPos(hold.x, hold.y, hold.z);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.fallDistance = 0.0F;
        mob.hurtMarked = true;
        level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 4, 0.3, 0.4, 0.3, 0.2);
        grab.ticks--;
        if (grab.ticks <= 0) {
            releaseGrab(player, mob);
            makeServant(player, mob);
        }
    }

    private static void makeServant(ServerPlayer player, Mob mob) {
        mob.getPersistentData().putString(SERVANT_KEY, player.getUUID().toString());
        mob.setPersistenceRequired();
        mob.setTarget(null);
        mob.setGlowingTag(true);
        addServantGoals(mob, player.getUUID());
        if (ModList.get().isLoaded("pehkui") && mob.getServer() != null) {
            // cuerpo deformado: mas ancho y encorvado (Pehkui)
            String selector = mob.getUUID().toString();
            var source = mob.getServer().createCommandSourceStack().withSuppressedOutput();
            mob.getServer().getCommands().performPrefixedCommand(source, "scale set pehkui:width 1.35 " + selector);
            mob.getServer().getCommands().performPrefixedCommand(source, "scale set pehkui:height 0.88 " + selector);
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1.0, mob.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
            level.playSound(null, mob.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4F, 1.8F);
        }
        say(player, "corrupted", servantCount(player), servantLimit(player));
    }

    private static final String MODE_KEY = "tc_servant_mode";
    private static final int MODE_FOLLOW = 0; // sigue al dueño y lo defiende (ataca a quien lo ataca o a quien el ataca), como un lobo
    private static final int MODE_STAY = 1;   // se queda quieto en el lugar
    private static final int MODE_AGGRESSIVE = 2; // sigue al dueño y ataca al enemigo mas cercano

    private static int modeOf(Entity mob) {
        return mob.getPersistentData().getInt(MODE_KEY);
    }

    /** Los sirvientes obedecen como los lobos: se les puede decir que se queden, que sigan o que ataquen (click derecho con la mano vacia). */
    private static void addServantGoals(Mob mob, UUID owner) {
        Predicate<LivingEntity> enemy = target -> target instanceof Enemy && !isServant(target) && !target.getUUID().equals(owner);
        mob.targetSelector.addGoal(1, new OwnerTargetGoal(mob, owner, true));
        mob.targetSelector.addGoal(2, new OwnerTargetGoal(mob, owner, false));
        mob.targetSelector.addGoal(3, new ModeGatedTargetGoal(mob, enemy));
        mob.goalSelector.addGoal(1, new StayGoal(mob));
        mob.goalSelector.addGoal(2, new FollowOwnerGoal(mob, owner));
    }

    /** Si el modo es "quieto" el mob no se mueve del lugar. */
    private static final class StayGoal extends Goal {
        private final Mob mob;

        StayGoal(Mob mob) {
            this.mob = mob;
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return modeOf(mob) == MODE_STAY;
        }

        @Override
        public void start() {
            mob.getNavigation().stop();
        }
    }

    /** Ataca a quien lastimo al dueño (hurtBy) o a lo que el dueño esta atacando (hurt), como los lobos. */
    private static final class OwnerTargetGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final UUID owner;
        private final boolean defend;
        private LivingEntity candidate;

        OwnerTargetGoal(Mob mob, UUID owner, boolean defend) {
            super(mob, false);
            this.owner = owner;
            this.defend = defend;
            setFlags(java.util.EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (modeOf(mob) == MODE_STAY) {
                return false;
            }
            Player player = mob.level().getPlayerByUUID(owner);
            if (player == null) {
                return false;
            }
            LivingEntity other = defend ? player.getLastHurtByMob() : player.getLastHurtMob();
            int stamp = defend ? player.getLastHurtByMobTimestamp() : player.getLastHurtMobTimestamp();
            if (other == null || stamp + 200 < player.tickCount || !other.isAlive() || other == player || isServant(other)) {
                return false;
            }
            candidate = other;
            return true;
        }

        @Override
        public void start() {
            mob.setTarget(candidate);
            super.start();
        }
    }

    /** Solo en modo agresivo busca al enemigo mas cercano. */
    private static final class ModeGatedTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        ModeGatedTargetGoal(Mob mob, Predicate<LivingEntity> enemy) {
            super(mob, LivingEntity.class, 10, true, false, enemy);
        }

        @Override
        public boolean canUse() {
            return modeOf(mob) == MODE_AGGRESSIVE && super.canUse();
        }
    }

    /** Click derecho con la mano vacia sobre un sirviente propio: cambia el modo (seguir, quedarse, agresivo). */
    @SubscribeEvent
    public static void onServantInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player) || !event.getEntity().getMainHandItem().isEmpty()
                || !isServantOf(event.getTarget(), player.getUUID()) || isPraying(player)) {
            return;
        }
        int mode = (modeOf(event.getTarget()) + 1) % 3;
        event.getTarget().getPersistentData().putInt(MODE_KEY, mode);
        if (event.getTarget() instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        say(player, mode == MODE_STAY ? "servant_stay" : mode == MODE_AGGRESSIVE ? "servant_aggressive" : "servant_follow");
        event.setCanceled(true);
    }

    private static final class FollowOwnerGoal extends Goal {
        private final Mob mob;
        private final UUID owner;
        private Player target;

        FollowOwnerGoal(Mob mob, UUID owner) {
            this.mob = mob;
            this.owner = owner;
            setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player player = mob.level().getPlayerByUUID(owner);
            if (player == null || modeOf(mob) == MODE_STAY || mob.getTarget() != null || mob.distanceToSqr(player) < 36.0) {
                return false;
            }
            target = player;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && mob.getTarget() == null && mob.distanceToSqr(target) > 9.0 && !mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(target, 1.1);
        }

        @Override
        public void tick() {
            if (mob.distanceToSqr(target) > 24 * 24) {
                mob.teleportTo(target.getX(), target.getY(), target.getZ());
            } else if (mob.tickCount % 20 == 0) {
                mob.getNavigation().moveTo(target, 1.1);
            }
        }
    }

    /** Al recargar un chunk los objetivos del mob se reinician: se le vuelven a poner. */
    @SubscribeEvent
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && isServant(mob)) {
            try {
                addServantGoals(mob, UUID.fromString(mob.getPersistentData().getString(SERVANT_KEY)));
            } catch (IllegalArgumentException ignored) {
                // marca corrupta: se ignora
            }
        }
    }

    @SubscribeEvent
    public static void servantTargets(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget == null || !(event.getEntity() instanceof Mob mob) || !isServant(mob)) {
            return;
        }
        String owner = mob.getPersistentData().getString(SERVANT_KEY);
        if (newTarget.getUUID().toString().equals(owner) || owner.equals(newTarget.getPersistentData().getString(SERVANT_KEY))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void servantsDontHurtOwner(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && isServant(attacker)) {
            String owner = attacker.getPersistentData().getString(SERVANT_KEY);
            LivingEntity victim = event.getEntity();
            if (victim.getUUID().toString().equals(owner) || owner.equals(victim.getPersistentData().getString(SERVANT_KEY))) {
                event.setCanceled(true);
            }
        }
    }

    // ----------------------------------------------------------------- Malnacido
    private static boolean slam(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.6F);
        BlockState ground = level.getBlockState(player.blockPosition().below());
        if (ground.isAir()) {
            ground = Blocks.DIRT.defaultBlockState();
        }
        for (int i = 0; i < 40; i++) {
            double angle = i * (Math.PI * 2 / 40);
            double radius = 1.0 + (i % 4) * 1.4;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX() + Math.cos(angle) * radius, player.getY() + 0.1,
                    player.getZ() + Math.sin(angle) * radius, 2, 0.1, 0.1, 0.1, 0.05);
        }
        AABB box = player.getBoundingBox().inflate(SLAM_RADIUS, 2.0, SLAM_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (target != player && target.isAlive() && !isServantOf(target, player.getUUID()) && target.distanceToSqr(player) <= SLAM_RADIUS * SLAM_RADIUS) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 3, false, true));
            }
        }
        return true;
    }

    // ----------------------------------------------------------------- Gigante Rocoso
    private static CompoundTag held(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.contains(HELD_KEY) ? persisted.getCompound(HELD_KEY) : null;
    }

    private static void setHeld(ServerPlayer player, BlockState state) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (state == null) {
            persisted.remove(HELD_KEY);
        } else {
            persisted.put(HELD_KEY, NbtUtils.writeBlockState(state));
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        sync(player, RacialStateSyncPacket.KIND_HELD_BLOCK, state == null ? 0 : net.minecraft.world.level.block.Block.getId(state));
    }

    private static boolean pickable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty()
                && level.getBlockEntity(pos) == null && state.getDestroySpeed(level, pos) >= 0.0F && state.getDestroySpeed(level, pos) <= 50.0F
                && !state.is(BlockTags.WITHER_IMMUNE);
    }

    private static boolean giantBlock(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        CompoundTag current = held(player);
        if (current != null) {
            return throwBlock(player, level, current);
        }
        // sin bloque en la mano: agarra el de enfrente o, si no hay, el de abajo
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1.0F).scale(GIANT_REACH)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        BlockPos pos = null;
        if (hit.getType() == HitResult.Type.BLOCK && pickable(level, hit.getBlockPos())) {
            pos = hit.getBlockPos();
        } else if (player.onGround() && pickable(level, player.blockPosition().below())) {
            pos = player.blockPosition().below();
        }
        if (pos == null) {
            say(player, "no_block");
            return false;
        }
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
        setHeld(player, state);
        say(player, "picked");
        return false; // agarrar no gasta cooldown: se gasta al lanzarlo
    }

    private static boolean throwBlock(ServerPlayer player, ServerLevel level, CompoundTag tag) {
        BlockState state = NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag);
        BlockPos head = BlockPos.containing(player.getEyePosition());
        if (!level.getBlockState(head).isAir()) {
            say(player, "no_room");
            return false;
        }
        FallingBlockEntity block = FallingBlockEntity.fall(level, head, state);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 start = player.getEyePosition().add(look.scale(0.8)).subtract(0.0, 0.25, 0.0);
        block.setPos(start.x, start.y, start.z);
        block.setDeltaMovement(look.scale(1.5).add(0.0, 0.15, 0.0));
        block.dropItem = false;
        block.hurtMarked = true;
        THROWN.put(block.getUUID(), player.getUUID());
        setHeld(player, null);
        level.playSound(null, player.blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    private static void tickThrown(MinecraftServer server) {
        Iterator<Map.Entry<UUID, UUID>> it = THROWN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            Entity block = null;
            for (ServerLevel level : server.getAllLevels()) {
                block = level.getEntity(entry.getKey());
                if (block != null) {
                    break;
                }
            }
            if (block == null || block.isRemoved()) {
                it.remove(); // aterrizo y se coloco solo, como un bloque que cae
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getValue());
            if (owner == null) {
                continue;
            }
            List<LivingEntity> hits = new ArrayList<>(block.level().getEntitiesOfClass(LivingEntity.class, block.getBoundingBox().inflate(0.4),
                    target -> validVictim(owner, target)));
            if (!hits.isEmpty()) {
                LivingEntity target = hits.get(0);
                PendingElementalHits.hurtNow(target, owner, ModDamageTypes.EARTH, 5.0F + level(owner) * 0.6F);
                if (block.level() instanceof ServerLevel level) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ((FallingBlockEntity) block).getBlockState()), block.getX(),
                            block.getY(), block.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
                    level.playSound(null, block.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                }
                block.discard(); // le dio a un mob: el bloque se rompe, no se vuelve a colocar
                it.remove();
            }
        }
    }

    // ----------------------------------------------------------------- tick y red
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Prayer prayer = PRAYING.get(player.getUUID());
        if (prayer != null) {
            boolean moved = player.position().subtract(prayer.start()).horizontalDistanceSqr() > 0.04 || player.zza != 0.0F || player.xxa != 0.0F
                    || player.getDeltaMovement().y > 0.1;
            if (moved) {
                endPrayer(player, false);
            } else {
                player.setPose(Pose.CROUCHING);
                if (--prayer.ticksLeft()[0] <= 0) {
                    endPrayer(player, true);
                }
            }
        }
        Integer aura = AURA.get(player.getUUID());
        if (aura != null) {
            tickAura(player, aura);
            if (aura + 1 >= WAVE_TICKS) {
                AURA.remove(player.getUUID());
                WAVE_HIT.remove(player.getUUID());
            } else {
                AURA.put(player.getUUID(), aura + 1);
            }
        }
        Grab grab = GRABS.get(player.getUUID());
        if (grab != null) {
            tickGrab(player, grab);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !THROWN.isEmpty()) {
            tickThrown(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PRAYING.remove(player.getUUID());
            AURA.remove(player.getUUID());
            WAVE_HIT.remove(player.getUUID());
            Grab grab = GRABS.remove(player.getUUID());
            if (grab != null && player.serverLevel().getEntity(grab.mob) instanceof Mob mob) {
                mob.setNoAi(false);
            }
        }
    }

    /** Los que se acercan a un Gigante Rocoso (o entran al mundo) ven el bloque que lleva. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            CompoundTag tag = held(target);
            if (tag != null) {
                BlockState state = NbtUtils.readBlockState(target.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag);
                Networking.sendToPlayer(viewer, new RacialStateSyncPacket(target.getId(), RacialStateSyncPacket.KIND_HELD_BLOCK,
                        net.minecraft.world.level.block.Block.getId(state)));
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && held(player) != null) {
            BlockState state = NbtUtils.readBlockState(player.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), held(player));
            sync(player, RacialStateSyncPacket.KIND_HELD_BLOCK, net.minecraft.world.level.block.Block.getId(state));
        }
    }
}
