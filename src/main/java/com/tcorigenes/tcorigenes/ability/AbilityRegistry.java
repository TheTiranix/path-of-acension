// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/** Registro simple en memoria (no necesita DeferredRegister: no son items/bloques). */
public final class AbilityRegistry {
    private static final Map<String, PlayerAbility> REGISTRY = new LinkedHashMap<>();

    private AbilityRegistry() {
    }

    public static void register(PlayerAbility ability) {
        REGISTRY.put(ability.id(), ability);
    }

    public static PlayerAbility get(String id) {
        return REGISTRY.get(id);
    }

    /**
     * Habilidad de prueba para validar el circuito completo (keybind -> paquete -> cooldown
     * server-side -> efecto -> HUD). Reemplazar/quitar cuando se registren las habilidades
     * reales de cada clase (Berserker, Escudero, etc).
     */
    public static void registerDefaults() {
        registerFuriaBerserker();
        registerGuardiaTotal();
        registerOjoDeHalcon();
        registerEspirituAnima();
        registerTeletransporteEnder();
        register(new PlayerAbility() {
            @Override
            public String id() {
                return "tcorigenes:grito_de_guerra";
            }

            @Override
            public int cooldownTicks() {
                return 20 * 30;
            }

            @Override
            public ResourceLocation icon() {
                return ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png");
            }

            @Override
            public void activate(net.minecraft.server.level.ServerPlayer player) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 5, 0, false, true, true));
                Level level = player.level();
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 2.0, player.getZ(), 8, 0.3, 0.3, 0.3, 0.01);
                }
            }
        });
    }

    /**
     * "Furia Berserker" (fase provisoria: +40% daño / +15% crit exactos y el "desangre" bien
     * calibrado quedan para cuando definamos el sistema de daño critico del Berserker; por
     * ahora uso Fuerza II + Wither I como aproximacion jugable de "mas daño a cambio de sangrar").
     * Duracion 10s, cooldown 5 minutos, tal como pediste.
     */
    private static void registerFuriaBerserker() {
        register(new PlayerAbility() {
            @Override
            public String id() {
                return "tcorigenes:furia_berserker";
            }

            @Override
            public int cooldownTicks() {
                return BerserkerFury.COOLDOWN_TICKS;
            }

            @Override
            public ResourceLocation icon() {
                return ResourceLocation.withDefaultNamespace("textures/item/iron_sword.png");
            }

            @Override
            public void activate(net.minecraft.server.level.ServerPlayer player) {
                BerserkerFury.start(player);
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GOAT_SCREAMING_HORN_BREAK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.7F);
            }
        });
    }

    /**
     * "Guardia Total" (Escudero). Duracion 10s y cooldown de 3 minutos (documento v2).
     */
    private static void registerGuardiaTotal() {
        register(new PlayerAbility() {
            @Override
            public String id() {
                return "tcorigenes:guardia_total";
            }

            @Override
            public int cooldownTicks() {
                return 20 * 60 * 3;
            }

            @Override
            public ResourceLocation icon() {
                return ResourceLocation.withDefaultNamespace("textures/item/shield.png");
            }

            @Override
            public void activate(net.minecraft.server.level.ServerPlayer player) {
                com.tcorigenes.tcorigenes.ability.TemporaryInvulnerability.grant(player, 20 * 10);
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        });
    }

    /**
     * "Ojo de Halcón" (Arquero): marca un punto debil (particula roja grande) en todos los
     * enemigos en 30 bloques por 20s; los proyectiles que lo aciertan hacen +40% de daño y +15%
     * de daño real (ver WeakPointManager).
     * Cooldown 45s (no especificado, ajustable).
     */
    private static void registerOjoDeHalcon() {
        register(new PlayerAbility() {
            @Override
            public String id() {
                return "tcorigenes:ojo_de_halcon";
            }

            @Override
            public int cooldownTicks() {
                return 20 * 45;
            }

            @Override
            public ResourceLocation icon() {
                return ResourceLocation.withDefaultNamespace("textures/item/spyglass.png");
            }

            @Override
            public void activate(net.minecraft.server.level.ServerPlayer player) {
                int marked = com.tcorigenes.tcorigenes.core.WeakPointManager.markForArcher(player);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(marked > 0
                        ? "Ojo de Halcón: " + marked + " punto(s) débil(es) marcado(s) por 20 s."
                        : "Ojo de Halcón: no hay enemigos en 30 bloques."), true);
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SPYGLASS_USE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        });
    }

    /**
     * "Espíritu Ánima" (Guerrero Ánima). Version simplificada: en vez de invocar un espiritu
     * (entidad nueva, invulnerable, sin hitbox, atacando 15s — pendiente, es una entidad custom
     * completa), hace un golpe de energia inmediato a los enemigos cercanos + un buff propio de
     * 15s. Cooldown 4 minutos.
     */
    private static void registerEspirituAnima() {
        register(new PlayerAbility() {
            @Override
            public String id() {
                return "tcorigenes:espiritu_anima";
            }

            @Override
            public int cooldownTicks() {
                return 20 * 60 * 4;
            }

            @Override
            public ResourceLocation icon() {
                return ResourceLocation.withDefaultNamespace("textures/item/nether_star.png");
            }

            @Override
            public void activate(net.minecraft.server.level.ServerPlayer player) {
                AnimaSpiritTracker.summon(player);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 15, 1, false, true, true));
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        });
    }

    /** Teletransporte del Ender Warrior: 20 bloques en la direccion en la que mira. Cooldown 4 min. */
    private static void registerTeletransporteEnder() {
        register(new PlayerAbility() {
            @Override
            public String id() {
                return "tcorigenes:teletransporte_ender";
            }

            @Override
            public int cooldownTicks() {
                return 20 * 60 * 4;
            }

            @Override
            public ResourceLocation icon() {
                return ResourceLocation.withDefaultNamespace("textures/item/ender_eye.png");
            }

            @Override
            public void activate(net.minecraft.server.level.ServerPlayer player) {
                net.minecraft.world.phys.Vec3 start = player.getEyePosition();
                net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                net.minecraft.world.phys.Vec3 end = start.add(look.scale(20.0));

                net.minecraft.world.level.ClipContext clipContext = new net.minecraft.world.level.ClipContext(
                        start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, player);
                net.minecraft.world.phys.BlockHitResult hit = player.level().clip(clipContext);

                net.minecraft.world.phys.Vec3 destination = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                        ? start.add(look.scale(Math.max(0.0, start.distanceTo(hit.getLocation()) - 1.0)))
                        : end;

                player.teleportTo(destination.x, destination.y, destination.z);
                player.fallDistance = 0;
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        });
    }
}
