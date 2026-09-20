// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage.event;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import com.tudominio.elementaldamage.ElementalDamageIndicator;
import com.tudominio.elementaldamage.ElementalProcCooldowns;
import com.tudominio.elementaldamage.ElementalStackManager;
import com.tudominio.elementaldamage.ModAttributes;
import com.tudominio.elementaldamage.ModDamageTypes;
import com.tudominio.elementaldamage.ModEntityTags;
import com.tudominio.elementaldamage.PendingElementalHits;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Map;

/**
 * Maneja los 9 elementos: esquive/critico/resistencia base (como antes) mas el efecto propio
 * de cada uno. Fuego/lunar/natural se resuelven con mecanicas vanilla (fuego, wither, veneno
 * escalonado) leyendo el daño de ESE golpe. Hielo y ender aplican un debuff (velocidad / armadura)
 * que hay que ir re-calculando con el tiempo (ver ElementalStackManager). Agua acumula stack y
 * se lee al curar (ver onLivingHeal). Tierra y aire son "procs" con cooldown propio por atacante
 * (stun y rayo, ver ElementalProcCooldowns), no debuffs acumulables.
 * Fuego/luz/agua/lunar ademas tienen bonus de daño fijo contra ciertos tags de enemigo.
 */
@EventBusSubscriber(modid = "elementaldamage")
public class ElementalDamageEvents {

    private static final Map<ResourceKey<DamageType>, java.util.function.Supplier<net.minecraftforge.registries.RegistryObject<Attribute>>> RESISTANCE_BY_TYPE = Map.of(
            ModDamageTypes.LIGHT, () -> ModAttributes.RESIST_LIGHT,
            ModDamageTypes.FIRE_ELEMENTAL, () -> ModAttributes.RESIST_FIRE,
            ModDamageTypes.WATER_ELEMENTAL, () -> ModAttributes.RESIST_WATER,
            ModDamageTypes.LUNAR, () -> ModAttributes.RESIST_LUNAR,
            ModDamageTypes.ENDER_ELEMENTAL, () -> ModAttributes.RESIST_ENDER
    );

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        boolean isArrow = event.getSource().getDirectEntity() instanceof AbstractArrow;
        LivingEntity target = event.getEntity();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity le ? le : null;

        // --- Esquive: chance base del jugador (ya no depende de ningun stack elemental) ---
        if (target instanceof Player player) {
            Attribute dodgeAttribute = isArrow ? ModAttributes.ARROW_DODGE_CHANCE.get() : ModAttributes.DODGE_CHANCE.get();
            var dodgeInstance = player.getAttribute(dodgeAttribute);
            if (dodgeInstance != null && player.getRandom().nextDouble() < Math.max(0.0, dodgeInstance.getValue())) {
                event.setCanceled(true);
                return;
            }
        }

        ResourceKey<DamageType> elementKey = event.getSource().typeHolder().unwrapKey().orElse(null);
        // "Daño elemental base": el monto tal cual llega, antes de que crítico/tag/resistencia
        // lo inflen. Todas las formulas de "cada X de daño" (fuego, hielo, agua, ender, tierra,
        // aire, natural) se calculan sobre ESTO, no sobre el daño final ya modificado.
        float baseDamage = event.getAmount();

        // --- Critico del atacante: chance propia (la vulnerabilidad ender ahora es reduccion de armadura, no critico) ---
        if (attacker instanceof Player playerAttacker) {
            var critChanceInstance = playerAttacker.getAttribute(ModAttributes.CRIT_CHANCE.get());
            double baseCritChance = critChanceInstance != null ? critChanceInstance.getValue() : 0.0;

            // Arquero: +50% chance de critico y +50% daño critico, pero SOLO con arco/ballesta
            // (por eso no es un atributo plano: afectaria tambien al cuerpo a cuerpo).
            boolean archerBowBonus = isArrow && playerAttacker.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                    .map(data -> data.getPlayerClass() == PlayerClass.ARQUERO).orElse(false);
            double archerCritChanceBonus = archerBowBonus ? 0.50 : 0.0;
            double archerCritDamageBonus = archerBowBonus ? 0.50 : 0.0;

            boolean meleeHit = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK);
            // Chance aditiva (atributo + bono del Arquero con arco) y luego "menos chance de NO critico":
            // cada fuente multiplica la chance de fallar, asi 10% menos de fallo con 0% base da 10%, y con
            // 40% base da 46%. Berserker (solo melee), Guerrero Anima y Malnacido purificado (ver critFailFactor).
            double critChance = Math.min(1.0, Math.max(0.0, baseCritChance + archerCritChanceBonus));
            critChance = 1.0 - (1.0 - critChance) * critFailFactor(playerAttacker, meleeHit);
            if (playerAttacker.getRandom().nextDouble() < critChance) {
                var critDamageInstance = playerAttacker.getAttribute(ModAttributes.CRIT_DAMAGE.get());
                double multiplier = (critDamageInstance != null ? critDamageInstance.getValue() : 1.5) + archerCritDamageBonus;
                event.setAmount((float) (event.getAmount() * multiplier));
            }
            if (isArrow) {
                var bowMultInstance = playerAttacker.getAttribute(ModAttributes.BOW_DAMAGE_MULT.get());
                if (bowMultInstance != null) {
                    event.setAmount((float) (event.getAmount() * bowMultInstance.getValue()));
                }
            }
        }

        // --- Bonus de daño fijo contra el "tipo" de enemigo, segun el elemento que pega ---
        if (elementKey != null) {
            if (elementKey.equals(ModDamageTypes.FIRE_ELEMENTAL) && target.getType().is(ModEntityTags.ICE_TYPE)) {
                event.setAmount((float) (event.getAmount() * 1.15));
            } else if (elementKey.equals(ModDamageTypes.LIGHT)) {
                if (target.getType().is(ModEntityTags.FIRE_TYPE)) {
                    event.setAmount((float) (event.getAmount() * 1.40));
                }
                if (target.getType().is(ModEntityTags.LUNAR_TYPE)) {
                    event.setAmount((float) (event.getAmount() * 1.20));
                }
            } else if (elementKey.equals(ModDamageTypes.WATER_ELEMENTAL) && target.getType().is(ModEntityTags.ENDER_TYPE)) {
                event.setAmount((float) (event.getAmount() * 1.20));
            } else if (elementKey.equals(ModDamageTypes.LUNAR) && target.getType().is(ModEntityTags.NATURAL_TYPE)) {
                event.setAmount((float) (event.getAmount() * 1.15));
            }
        }

        // --- Resistencia elemental de la victima (solo los 5 elementos con atributo de raza) ---
        if (elementKey != null && target instanceof Player) {
            var attributeSupplier = RESISTANCE_BY_TYPE.get(elementKey);
            if (attributeSupplier != null) {
                var resistInstance = target.getAttribute(attributeSupplier.get().get());
                if (resistInstance != null) {
                    event.setAmount((float) (event.getAmount() * (1.0 - resistInstance.getValue())));
                }
            }
        }

        if (elementKey != null && ModDamageTypes.ALL.contains(elementKey)) {
            applyOnHitEffect(target, attacker, elementKey, baseDamage);
            ElementalDamageIndicator.show(target, elementKey, event.getAmount());
        } else {
            // No es uno de nuestros 9 tipos (golpe normal de jugador o de mob): si quien pega es
            // un mob con afinidad, le agrega un daño elemental APARTE (ver MobElementalAttackHandler);
            // si es un jugador, su raza puede sumar/convertir daño elemental (ver RacialElemental).
            if (attacker instanceof Player racialAttacker) {
                com.tcorigenes.tcorigenes.core.RacialElemental.apply(event, racialAttacker);
            }
            MobElementalAttackHandler.applyExtraElementalDamage(target, event.getSource(), baseDamage);
        }
    }

    /** Multiplicador de la chance de NO hacer critico segun raza/clase (1.0 = sin cambio, 0.5 = la mitad de fallos). */
    private static double critFailFactor(Player player, boolean meleeHit) {
        double factor = 1.0;
        PlayerClass cls = player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass()).orElse(PlayerClass.NINGUNA);
        if ((cls == PlayerClass.BERSERKER && meleeHit) || cls == PlayerClass.GUERRERO_ANIMA) {
            factor *= 0.90;
        }
        boolean purifiedMalnacido = player.getPersistentData().getBoolean("malnacido_purificado")
                && player.getCapability(com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                        .map(info -> info.getRace() == com.tcorigenes.tcorigenes.core.Race.MALNACIDO).orElse(false);
        if (purifiedMalnacido) {
            factor *= 0.50;
        }
        return factor;
    }

    /** Efecto propio de cada elemento, en escala CONTINUA sobre el daño BASE (antes de
     *  critico/tag/resistencia): a diferencia de una formula "por cada X de daño" con
     *  redondeo hacia abajo, un golpe chico SI genera una fraccion de efecto en vez de nada.
     *  Eso importa para el extra elemental de los mobs (chico, casi nunca llega al umbral de una).
     *  Visibilidad de paquete: tambien la usa MobElementalAttackHandler (con attacker null, porque
     *  el extra de un mob no tiene un "atacante" propio para los procs de tierra/aire). */
    static void applyOnHitEffect(LivingEntity target, LivingEntity attacker, ResourceKey<DamageType> element, float baseDamage) {
        if (element.equals(ModDamageTypes.FIRE_ELEMENTAL)) {
            // 2s cada 2 de daño = 20 ticks por punto de daño, tope 20s.
            int addedTicks = Math.round(baseDamage * 20.0F);
            if (addedTicks > 0) {
                int cappedTicks = Math.min(target.getRemainingFireTicks() + addedTicks, 20 * 20);
                target.setRemainingFireTicks(cappedTicks);
            }
        } else if (element.equals(ModDamageTypes.ICE)) {
            double accumulated = ElementalStackManager.registerHit(target, element, baseDamage);
            ElementalStackManager.applyIceSlow(target, accumulated);
        } else if (element.equals(ModDamageTypes.ENDER_ELEMENTAL)) {
            double accumulated = ElementalStackManager.registerHit(target, element, baseDamage);
            ElementalStackManager.applyEnderArmorReduction(target, accumulated);
        } else if (element.equals(ModDamageTypes.LUNAR)) {
            // 2s cada 10 de daño = 4 ticks por punto de daño, tope 20s.
            int addedTicks = Math.round(baseDamage * 4.0F);
            if (addedTicks > 0) {
                MobEffectInstance current = target.getEffect(MobEffects.WITHER);
                int currentDuration = current != null ? current.getDuration() : 0;
                int newDuration = Math.min(currentDuration + addedTicks, 20 * 20);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, newDuration, 0, false, true));
            }
        } else if (element.equals(ModDamageTypes.NATURAL)) {
            // 1s (20 ticks) cada 5 de daño = 4 ticks por punto de daño.
            int addedTicks = Math.round(baseDamage * 4.0F);
            if (addedTicks > 0) {
                MobEffectInstance current = target.getEffect(MobEffects.POISON);
                int currentDuration = current != null ? current.getDuration() : 0;
                int newDuration = Math.min(currentDuration + addedTicks, 15 * 20);
                int amplifier = newDuration >= 10 * 20 ? 4 : newDuration >= 5 * 20 ? 2 : 0;
                target.addEffect(new MobEffectInstance(MobEffects.POISON, newDuration, amplifier, false, true));
            }
        } else if (element.equals(ModDamageTypes.WATER_ELEMENTAL)) {
            // Sin efecto inmediato: solo acumula stack, se lee en onLivingHeal.
            ElementalStackManager.registerHit(target, element, baseDamage);
        } else if (element.equals(ModDamageTypes.EARTH)) {
            if (attacker != null) {
                applyEarthStun(target, attacker, baseDamage);
            }
        } else if (element.equals(ModDamageTypes.AIR)) {
            if (attacker != null) {
                applyAirLightning(target, attacker, baseDamage);
            }
        }
    }

    /** Tierra: stunea 1s cada 20 de daño base (tope 3s), con cooldown de 20s por atacante.
     *  "Stun" = 95%+ menos de velocidad (Slowness alto) + no poder atacar (ver ModEvents#onAttackEntity). */
    private static void applyEarthStun(LivingEntity target, LivingEntity attacker, float baseDamage) {
        long now = target.level().getGameTime();
        if (!ElementalProcCooldowns.isReady(attacker, ElementalProcCooldowns.Kind.EARTH, now)) {
            return;
        }
        int stunTicks = Math.min(Math.round(baseDamage * 1.0F), 3 * 20);
        if (stunTicks <= 0) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 6, false, true, true));
        ElementalProcCooldowns.markStunned(target, now + stunTicks);
        ElementalProcCooldowns.startCooldown(attacker, ElementalProcCooldowns.Kind.EARTH, now + 20 * 20);
    }

    /** Aire: invoca un rayo (solo visual/sonido, sin el daño/fuego vanilla) que hace 5 de daño
     *  cada 10 de daño base de aire (tope 50), como golpe elemental aparte (PendingElementalHits).
     *  Cooldown de 10s por atacante. */
    private static void applyAirLightning(LivingEntity target, LivingEntity attacker, float baseDamage) {
        long now = target.level().getGameTime();
        if (!ElementalProcCooldowns.isReady(attacker, ElementalProcCooldowns.Kind.AIR, now)) {
            return;
        }
        float boltDamage = Math.min(baseDamage * 0.5F, 50.0F);
        if (boltDamage <= 0.0F) {
            return;
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                serverLevel.addFreshEntity(bolt);
            }
        }
        PendingElementalHits.queue(target, attacker, ModDamageTypes.AIR, boltDamage, now + PendingElementalHits.SAFE_DELAY_TICKS);
        ElementalProcCooldowns.startCooldown(attacker, ElementalProcCooldowns.Kind.AIR, now + 10 * 20);
    }

    /** Reduce toda curacion (natural, pociones, robo de vida) segun el stack de agua acumulado. */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity target = event.getEntity();
        double waterPenalty = cappedPercent(ElementalStackManager.peek(target, ModDamageTypes.WATER_ELEMENTAL), 0.03, 0.30, 0.45, target);
        if (waterPenalty > 0.0) {
            event.setAmount((float) (event.getAmount() * (1.0 - waterPenalty)));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // Todos los ticks: la cola de golpes elementales diferidos necesita precision de 1 tick.
        if (event.player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.tudominio.elementaldamage.PendingElementalHits.tick(serverLevel.getGameTime());
        }
        // Throttle a ~1 vez por segundo: re-baja los debuffs elementales y limpia carteles vencidos.
        if (event.player.tickCount % 20 == 0) {
            ElementalStackManager.tickIceDecay();
            ElementalStackManager.tickEnderDecay();
            if (event.player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel2) {
                ElementalDamageIndicator.tick(serverLevel2);
            }
        }
    }

    /** stack de daño acumulado -> porcentaje, en escala continua (X% cada 10 de daño, sin
     *  redondear hacia abajo) y tope jugador/general. */
    private static double cappedPercent(double accumulatedDamage, double percentPer10, double playerCap, double generalCap, LivingEntity target) {
        double cap = target instanceof Player ? playerCap : generalCap;
        return Math.min(percentPer10 * (accumulatedDamage / 10.0), cap);
    }
}
