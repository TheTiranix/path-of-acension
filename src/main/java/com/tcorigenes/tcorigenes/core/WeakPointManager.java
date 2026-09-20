package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.server.level.ServerPlayer;
import com.tcorigenes.tcorigenes.faction.ModEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Puntos debiles marcados sobre enemigos, visibles solo para quien los puede golpear (un
 * marcador WeakPointEntity: carga ignea roja pegada al cuerpo).
 * - Hereje (Expertiz anatomica): un punto cada 10s en el enemigo cercano (con prioridad a un jefe);
 *   dura 4s; cualquier golpe (cuerpo a cuerpo, proyectil o magia) hace +45% de daño y +5% de daño real.
 * - Arquero (habilidad Ojo de Halcon): marca a todos los enemigos en 30 bloques por 20s; solo los
 *   proyectiles que los golpean hacen +40% de daño y +15% de daño real (un Hereje suma +15% / +5%).
 * El "daño real" ignora armadura: se resta directo de la vida (nunca mata solo, deja minimo 0.5).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class WeakPointManager {
    private static final int HEREJE_TICKS = 80;
    private static final int ARCHER_TICKS = 400;
    private static final double HEREJE_RANGE = 16.0;
    private static final double BOSS_RANGE = 32.0;
    private static final double ARCHER_RANGE = 30.0;

    private record Mark(UUID owner, LivingEntity target, long expiresAt, boolean archer, WeakPointEntity marker) {
    }

    private static final Map<Integer, List<Mark>> MARKS = new ConcurrentHashMap<>();

    private WeakPointManager() {
    }

    private static boolean isBoss(LivingEntity entity) {
        var type = entity.getType();
        return type.is(Tags.EntityTypes.BOSSES) || type == EntityType.WARDEN;
    }

    private static void add(ServerPlayer owner, LivingEntity target, int ticks, boolean archer) {
        long expires = owner.level().getGameTime() + ticks;
        List<Mark> list = MARKS.computeIfAbsent(target.getId(), id -> new ArrayList<>());
        list.removeIf(mark -> {
            boolean same = mark.owner().equals(owner.getUUID()) && mark.archer() == archer;
            if (same) {
                discard(mark);
            }
            return same;
        });
        WeakPointEntity marker = ModEntityTypes.WEAK_POINT.get().create(owner.level());
        if (marker != null) {
            marker.bind(owner.getUUID(), target, ticks);
            owner.level().addFreshEntity(marker);
        }
        list.add(new Mark(owner.getUUID(), target, expires, archer, marker));
    }

    private static void discard(Mark mark) {
        if (mark.marker() != null && !mark.marker().isRemoved()) {
            mark.marker().discard();
        }
    }

    /** Vale como enemigo: cualquier Enemy (incluye muchos mobs de mods) o un jefe. */
    private static boolean isEnemy(LivingEntity e) {
        return e.isAlive() && (e instanceof Enemy || isBoss(e));
    }

    /** Hereje: un unico punto activo; si hay un jefe cerca se marca ese, si no el enemigo mas cercano. */
    public static void markForHereje(ServerPlayer player) {
        clearOwner(player.getUUID(), false);
        List<LivingEntity> bosses = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(BOSS_RANGE), e -> e.isAlive() && isBoss(e));
        List<LivingEntity> pool = bosses.isEmpty() ? player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(HEREJE_RANGE), WeakPointManager::isEnemy) : bosses;
        pool.stream().min((a, b) -> Double.compare(a.distanceTo(player), b.distanceTo(player)))
                .ifPresent(target -> add(player, target, HEREJE_TICKS, false));
    }

    /** Arquero: marca a todos los enemigos cercanos por 20 segundos. */
    public static int markForArcher(ServerPlayer player) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(ARCHER_RANGE), WeakPointManager::isEnemy);
        targets.forEach(target -> add(player, target, ARCHER_TICKS, true));
        return targets.size();
    }

    private static void clearOwner(UUID owner, boolean archer) {
        for (List<Mark> list : MARKS.values()) {
            list.removeIf(mark -> {
                boolean same = mark.owner().equals(owner) && mark.archer() == archer;
                if (same) {
                    discard(mark);
                }
                return same;
            });
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 4 != 0) {
            return;
        }
        Iterator<Map.Entry<Integer, List<Mark>>> it = MARKS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            entry.getValue().removeIf(mark -> {
                boolean expired = !mark.target().isAlive() || mark.target().isRemoved()
                        || mark.target().level().getGameTime() > mark.expiresAt();
                if (expired) {
                    discard(mark);
                }
                return expired;
            });
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    /** Se llama en HIGH para que el multiplicador se aplique antes que el resto de los modificadores de daño. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        List<Mark> list = MARKS.get(event.getEntity().getId());
        if (list == null) {
            return;
        }
        boolean isHereje = attacker.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace() == Race.HEREJE).orElse(false);
        boolean projectile = event.getSource().getDirectEntity() instanceof Projectile;
        long now = attacker.level().getGameTime();
        float base = event.getAmount();
        float multiplier = 1.0F;
        float trueFraction = 0.0F;
        boolean landed = false;

        for (Iterator<Mark> it = list.iterator(); it.hasNext(); ) {
            Mark mark = it.next();
            if (!mark.owner().equals(attacker.getUUID()) || now > mark.expiresAt()) {
                continue;
            }
            if (!mark.archer()) {
                multiplier += 0.45F;
                trueFraction += 0.05F;
                discard(mark);
                it.remove();
                landed = true;
            } else if (projectile) {
                multiplier += 0.40F + (isHereje ? 0.15F : 0.0F);
                trueFraction += 0.15F + (isHereje ? 0.05F : 0.0F);
                landed = true;
            }
        }
        if (landed) {
            // Golpe pesado y "humedo" (carne): confirma al jugador que acerto el punto debil. Solo lo oye el.
            attacker.playNotifySound(SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.3F, 0.6F);
            attacker.playNotifySound(SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.2F, 0.5F);
            attacker.playNotifySound(SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.75F);
        }
        if (multiplier > 1.0F) {
            event.setAmount(base * multiplier);
            LivingEntity target = event.getEntity();
            float extra = base * trueFraction;
            if (extra > 0.0F) {
                target.setHealth(Math.max(0.5F, target.getHealth() - extra));
            }
        }
    }
}
