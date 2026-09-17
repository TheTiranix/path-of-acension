package com.tcorigenes.tcorigenes.ability;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Invocacion real del Guerrero Anima: un ArmorStand "marker" (sin hitbox, invulnerable,
 * invisible) que sigue existiendo 15s y golpea enemigos cercanos cada 10 ticks, en vez del
 * pulso instantaneo original. Se revisa desde ModEvents#onPlayerTick (throttled), no hace
 * falta un tick global nuevo.
 */
public final class AnimaSpiritTracker {
    private record ActiveSpirit(ArmorStand entity, long expireAtGameTime) {
    }

    private static final List<ActiveSpirit> ACTIVE = new ArrayList<>();

    private AnimaSpiritTracker() {
    }

    public static void summon(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ArmorStand spirit = new ArmorStand(EntityType.ARMOR_STAND, serverLevel);
        // setMarker(boolean) es privado: la unica forma publica de activarlo es via NBT.
        net.minecraft.nbt.CompoundTag markerTag = new net.minecraft.nbt.CompoundTag();
        markerTag.putBoolean("Marker", true);
        spirit.load(markerTag);
        spirit.setPos(player.getX(), player.getY(), player.getZ());
        spirit.setInvisible(true);
        spirit.setInvulnerable(true);
        spirit.setNoGravity(true);
        serverLevel.addFreshEntity(spirit);
        ACTIVE.add(new ActiveSpirit(spirit, serverLevel.getGameTime() + 20 * 15));
    }

    /** Llamar cada ~10 ticks (el costo es trivial: la lista tiene 0-4 elementos normalmente). */
    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        ACTIVE.removeIf(spirit -> {
            if (now >= spirit.expireAtGameTime() || !spirit.entity().isAlive()) {
                spirit.entity().discard();
                return true;
            }
            AABB area = new AABB(spirit.entity().blockPosition()).inflate(4.0);
            List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e instanceof Monster && e.isAlive());
            enemies.forEach(e -> e.hurt(spirit.entity().damageSources().magic(), 4.0F));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spirit.entity().getX(), spirit.entity().getY() + 1.0,
                    spirit.entity().getZ(), 5, 0.4, 0.4, 0.4, 0.01);
            return false;
        });
    }
}
