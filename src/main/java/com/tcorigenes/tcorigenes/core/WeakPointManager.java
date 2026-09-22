// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.faction.ModEntityTypes;
import com.tudominio.elementaldamage.ModDamageTypes;
import com.tudominio.elementaldamage.PendingElementalHits;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
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
 *   dura 4s; cualquier golpe (cuerpo a cuerpo, proyectil, magia) hace +45% de daño y +5% de daño real,
 *   y tambien el daño ELEMENTAL de ese mismo golpe (llega diferido, ver PendingElementalHits).
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
    /** El daño elemental de un golpe llega SAFE_DELAY_TICKS despues: se le da un margen extra. */
    private static final int ELEMENTAL_WINDOW = PendingElementalHits.SAFE_DELAY_TICKS + 3;

    private static final class Mark {
        final UUID owner;
        final LivingEntity target;
        final boolean archer;
        final WeakPointEntity marker;
        long expiresAt;
        /** Hereje: tick en que el golpe acerto (-1 = todavia no). Sigue valiendo para su elemental diferido. */
        long consumedAt = -1;
        /** Arquero: ultimo tick en que un proyectil acerto el punto. */
        long lastProjectileHit = -1000;
        /** Ultimo tick en que se avisó que se erró el punto (para no repetir el aviso en cada golpe). */
        long lastMissHint = -1000;

        Mark(UUID owner, LivingEntity target, boolean archer, WeakPointEntity marker, long expiresAt) {
            this.owner = owner;
            this.target = target;
            this.archer = archer;
            this.marker = marker;
            this.expiresAt = expiresAt;
        }
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
            boolean same = mark.owner.equals(owner.getUUID()) && mark.archer == archer;
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
        list.add(new Mark(owner.getUUID(), target, archer, marker, expires));
    }

    private static void discard(Mark mark) {
        if (mark.marker != null && !mark.marker.isRemoved()) {
            mark.marker.discard();
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
                boolean same = mark.owner.equals(owner) && mark.archer == archer;
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
                boolean expired = !mark.target.isAlive() || mark.target.isRemoved()
                        || mark.target.level().getGameTime() > mark.expiresAt;
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

    /** Radio de acierto por hitbox (respaldo cuando el modelo no se pudo leer): crece con el tamaño del mob. */
    static double fallbackAimRadius(LivingEntity target) {
        return Math.min(1.4, 0.45 + 0.1 * target.getBbHeight());
    }

    /**
     * El bono solo vale si se APUNTA al punto debil (el marcador sobre el modelo): en cuerpo a cuerpo, si la
     * mira pasa a menos del radio del punto; con proyectiles, si su trayectoria en el impacto pasa por el punto.
     * Sin marcador vivo se usa el punto/radio de la hitbox.
     */
    private static boolean aimedAtPoint(ServerPlayer attacker, Mark mark, DamageSource source) {
        LivingEntity target = mark.target;
        boolean hasMarker = mark.marker != null && !mark.marker.isRemoved();
        Vec3 point = hasMarker ? mark.marker.getAimPoint(target) : WeakPointAnchor.of(target);
        double radius = hasMarker ? mark.marker.getAimRadius(target) : fallbackAimRadius(target);
        var direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile) {
            Vec3 motion = projectile.getDeltaMovement();
            Vec3 from = projectile.position().subtract(motion);
            Vec3 to = projectile.position().add(motion.scale(0.3));
            return distanceToSegment(point, from, to) <= radius;
        }
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getViewVector(1.0F);
        double t = Math.max(0.0, point.subtract(eye).dot(look));
        return point.distanceTo(eye.add(look.scale(t))) <= radius;
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1.0E-7 ? 0.0 : Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
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
        var key = event.getSource().typeHolder().unwrapKey().orElse(null);
        boolean elemental = key != null && ModDamageTypes.ALL.contains(key);
        long now = attacker.level().getGameTime();
        float base = event.getAmount();
        float multiplier = 1.0F;
        float trueFraction = 0.0F;
        boolean landed = false;

        for (Mark mark : list) {
            if (!mark.owner.equals(attacker.getUUID()) || now > mark.expiresAt) {
                continue;
            }
            if (!mark.archer) {
                if (mark.consumedAt < 0 && !elemental && !aimedAtPoint(attacker, mark, event.getSource())) {
                    if (now - mark.lastMissHint > 15) {
                        mark.lastMissHint = now;
                        attacker.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "Fallaste el punto débil: apuntá a la marca roja.").withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
                    }
                } else if (mark.consumedAt < 0 && !elemental) {
                    // Primer golpe: acierta el punto, desaparece el marcador, la ventana queda abierta
                    // para el daño elemental de este mismo golpe (llega diferido).
                    multiplier += 0.45F;
                    trueFraction += 0.05F;
                    discard(mark);
                    mark.consumedAt = now;
                    mark.expiresAt = now + ELEMENTAL_WINDOW;
                    landed = true;
                } else if (mark.consumedAt >= 0 && elemental && now - mark.consumedAt <= ELEMENTAL_WINDOW) {
                    multiplier += 0.45F;
                    trueFraction += 0.05F;
                }
            } else if (projectile && !elemental) {
                if (aimedAtPoint(attacker, mark, event.getSource())) {
                    mark.lastProjectileHit = now;
                    multiplier += 0.40F + (isHereje ? 0.15F : 0.0F);
                    trueFraction += 0.15F + (isHereje ? 0.05F : 0.0F);
                    landed = true;
                }
            } else if (elemental && now - mark.lastProjectileHit <= ELEMENTAL_WINDOW) {
                multiplier += 0.40F + (isHereje ? 0.15F : 0.0F);
                trueFraction += 0.15F + (isHereje ? 0.05F : 0.0F);
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
