// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.weapon.WeaponBalance;
import com.tudominio.elementaldamage.PendingElementalHits;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Daño elemental fijo de las armas de mods de terceros (datos en WeaponBalance), a pedido de alejandr0.
 * Igual filosofia que RacialElemental (un golpe elemental es SIEMPRE aparte, nunca sumado al mismo
 * evento, para que resistencias/indicador se calculen solos):
 * - "+X de <elemento>": se SUMA un golpe elemental aparte en cada golpe.
 * - Ciclo (ej. fuego, lunar, normal, normal): el golpe N del ciclo se CONVIERTE entero en el elemento
 *   indicado (el daño total del golpe pasa a ser de ese elemento); los "normal" pegan como siempre. Si el
 *   elemento del golpe no le funciona al jugador (ver ElementalRestriction) ese golpe NO hace daño, salvo que
 *   lleve el Prisma Convertidor, que lo transforma en su elemento. La
 *   cuenta es fija por jugador+arma, nunca al azar.
 */
public final class WeaponElemental {
    /** Entidades-proyectil propias de un arma (no son flechas comunes) -> item del arma. Cada impacto avanza el ciclo. */
    private static final Map<ResourceLocation, ResourceLocation> SHOT_ENTITIES = Map.of(
            ResourceLocation.fromNamespaceAndPath("celestisynth", "rainfall_arrow"),
            ResourceLocation.fromNamespaceAndPath("celestisynth", "rainfall_serenity"),
            ResourceLocation.fromNamespaceAndPath("cataclysm", "phantom_arrow"),
            ResourceLocation.fromNamespaceAndPath("cataclysm", "cursed_bow"),
            ResourceLocation.fromNamespaceAndPath("cataclysm", "cursed_sandstorm"),
            ResourceLocation.fromNamespaceAndPath("cataclysm", "wrath_of_the_desert"));
    private static final String CYCLE_KEY = "tc_weapon_cycle";
    /** Impactos dentro de esta ventana (ticks) desde el que avanzo el ciclo cuentan como el mismo ataque. */
    private static final int SWING_GROUP_TICKS = 4;

    private WeaponElemental() {
    }

    /** Suma del daño elemental fijo por golpe de un arma (para el ranking de daño total). */
    public static float flatExtraTotal(ResourceLocation itemId) {
        WeaponBalance.Spec spec = WeaponBalance.spec(itemId);
        return spec == null ? 0.0F : spec.extrasTotal();
    }

    /** Arma de la que sale este golpe y si es el impacto de un proyectil suyo (ver SHOT_ENTITIES y los arcos con daño por impacto). */
    private record Source(ResourceLocation id, boolean shot) {
    }

    private static Source sourceOf(LivingHurtEvent event, Player attacker) {
        ResourceLocation id;
        boolean shot = false;
        Entity direct = event.getSource().getDirectEntity();
        // Dreadbow: las flechas oscuras (las que llueven del cielo tambien) pegan con un tipo de daño propio de Alex's Caves cuyo
        // causante directo es el propio jugador: no pasan por la deteccion de proyectil y hacian el daño casi sin modificar
        if (event.getSource().typeHolder().unwrapKey().map(k -> k.location().toString().equals("alexscaves:dark_arrow")).orElse(false)) {
            return new Source(ResourceLocation.fromNamespaceAndPath("alexscaves", "dreadbow"), true);
        }
        ResourceLocation directId = direct == null || direct == attacker ? null
                : ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
        if (directId != null && SHOT_ENTITIES.containsKey(directId)) {
            // impacto de un proyectil propio del arma (entidad distinta de una flecha comun): el daño es el de la spec
            id = SHOT_ENTITIES.get(directId);
            shot = true;
        } else {
            ItemStack weapon = attacker.getMainHandItem();
            id = ForgeRegistries.ITEMS.getKey(weapon.getItem());
            if (direct instanceof net.minecraft.world.entity.projectile.AbstractArrow && id != null) {
                WeaponBalance.Spec bow = WeaponBalance.spec(id);
                shot = bow != null && bow.ranged; // flecha comun disparada con un arco que tiene daño por impacto
            }
        }
        return new Source(id, shot);
    }

    /**
     * Fija el daño por impacto de los arcos (WeaponBalance) ANTES de que se tire el critico: pedido de alejandr0, el critico
     * de los arcos tiene que funcionar igual que el de las espadas (antes el daño por impacto se ponia despues y pisaba el
     * critico). Lo llama ElementalDamageEvents al principio del golpe.
     */
    public static void applyShotDamage(LivingHurtEvent event, Player attacker) {
        if (event.getAmount() <= 0.0F || attacker.level().isClientSide()) {
            return;
        }
        Source source = sourceOf(event, attacker);
        WeaponBalance.Spec spec = source.id() == null ? null : WeaponBalance.spec(source.id());
        if (source.shot() && spec != null && spec.damage != null) {
            event.setAmount(spec.damage.floatValue());
        }
    }

    public static void apply(LivingHurtEvent event, Player attacker) {
        if (event.getAmount() <= 0.0F || attacker.level().isClientSide()) {
            return;
        }
        Source source = sourceOf(event, attacker);
        ResourceLocation id = source.id();
        boolean shot = source.shot();
        Entity direct = event.getSource().getDirectEntity();
        WeaponBalance.Spec spec = id == null ? null : WeaponBalance.spec(id);
        if (!shot && direct == attacker) {
            applyGlove(attacker, event.getEntity(), spec);
        }
        if (spec == null || (spec.ranged && !shot)) {
            return; // un arco solo cuenta por el impacto de su proyectil
        }
        LivingEntity target = event.getEntity();
        ResourceKey<DamageType> converted = ElementalRestriction.converterElement(attacker);
        ResourceKey<DamageType> active = ElementalRestriction.activeElement(attacker, spec);
        ResourceKey<DamageType> absorb = converted == null ? ElementalRestriction.skillAbsorbElement(attacker, spec) : null;

        if (spec.cycle != null && !spec.cycle.isEmpty()) {
            ResourceKey<DamageType> slot = spec.perProjectile && shot
                    ? spec.cycle.get(projectileSlot(direct, attacker, id, spec.cycle.size()))
                    : nextSlot(attacker, id, spec.cycle, !shot, isCelestisynthSkill(event) ? 1 : SWING_GROUP_TICKS);
            if (slot != null) {
                if (converted != null) {
                    slot = converted; // el Prisma Convertidor manda: el golpe se transforma en ese elemento
                } else if (absorb != null) {
                    slot = absorb; // la habilidad de absorcion: el golpe entero pasa al elemento de la raza / el mayor
                } else if (!slot.equals(active)) {
                    event.setCanceled(true); // elemento que este jugador no puede usar: ese golpe no hace daño
                    return;
                }
                float total = event.getAmount();
                event.setCanceled(true); // este golpe entero pasa a ser el elemental, no se suma aparte
                // Un solo tipo de daño elemental y nada de daño normal antes: sin delay (pedido de alejandr0).
                PendingElementalHits.hurtNow(target, attacker, slot, total);
                return;
            }
        }
        for (WeaponBalance.Extra extra : spec.extras) {
            if (converted != null) {
                extra(target, attacker, converted, extra.amount());
            } else if (absorb != null) {
                // absorbido: pega como el elemento que absorbe; solo el del propio elemento dispara el efecto especial
                long fireAt = target.level().getGameTime() + PendingElementalHits.SAFE_DELAY_TICKS;
                if (extra.element().equals(absorb)) {
                    PendingElementalHits.queue(target, attacker, absorb, extra.amount(), fireAt);
                } else {
                    PendingElementalHits.queueNoEffect(target, attacker, absorb, extra.amount(), fireAt);
                }
            } else if (extra.element().equals(active)) {
                extra(target, attacker, extra.element(), extra.amount());
            } // cualquier otro elemento del equipo directamente no funciona (ver ElementalRestriction)
        }
    }

    /** Endersoul Hand equipada como guante: +30 de daño elemental ender por golpe (sujeto a la restriccion de elemento). */
    private static void applyGlove(Player attacker, LivingEntity target, WeaponBalance.Spec weaponSpec) {
        if (!com.tcorigenes.tcorigenes.compat.EndersoulGlove.isEquipped(attacker)) {
            return;
        }
        ResourceKey<DamageType> converted = ElementalRestriction.converterElement(attacker);
        ResourceKey<DamageType> element = com.tudominio.elementaldamage.ModDamageTypes.ENDER_ELEMENTAL;
        if (converted != null) {
            element = converted;
        } else {
            ResourceKey<DamageType> active = ElementalRestriction.activeElement(attacker, weaponSpec);
            if (active != null && !active.equals(element)) {
                return; // el jugador solo puede usar otro elemento
            }
        }
        extra(target, attacker, element, com.tcorigenes.tcorigenes.compat.EndersoulGlove.ENDER_DAMAGE);
    }

    private static void extra(LivingEntity target, Player attacker, ResourceKey<DamageType> element, float amount) {
        PendingElementalHits.queue(target, attacker, element, amount,
                target.level().getGameTime() + PendingElementalHits.SAFE_DELAY_TICKS);
    }

    /**
     * Devuelve el elemento del golpe que toca (null = normal). La memoria del ultimo golpe se guarda en los datos
     * persistentes del jugador, por arma (no en el NBT del item: cada cambio de NBT en la mano hace que el cliente
     * repita la animacion de equipar). Todos los impactos de un mismo ataque (barrido, habilidades de varios
     * golpes) caen dentro de SWING_GROUP_TICKS y comparten elemento; recien el ataque siguiente avanza el ciclo.
     */
    /**
     * Los golpes de las habilidades de Celestisynth salen con tipos de daño propios del mod. Cada golpe de una habilidad de varios golpes
     * tiene que avanzar el ciclo (pedido de alejandr0), asi que en esos casos solo se agrupan los que caen en el MISMO tick (un area que
     * le pega a varios enemigos a la vez cuenta como un golpe); el barrido comun de la espada sigue agrupando toda la tanda.
     */
    private static boolean isCelestisynthSkill(LivingHurtEvent event) {
        return event.getSource().typeHolder().unwrapKey().map(k -> k.location().getNamespace().equals("celestisynth")).orElse(false);
    }

    private static ResourceKey<DamageType> nextSlot(Player attacker, ResourceLocation itemId, List<ResourceKey<DamageType>> cycle,
            boolean groupSwing, int groupWindowTicks) {
        return cycle.get(nextIndex(attacker, itemId, cycle.size(), groupSwing, groupWindowTicks));
    }

    private static int nextIndex(Player attacker, ResourceLocation itemId, int size, boolean groupSwing) {
        return nextIndex(attacker, itemId, size, groupSwing, SWING_GROUP_TICKS);
    }

    private static int nextIndex(Player attacker, ResourceLocation itemId, int size, boolean groupSwing, int groupWindowTicks) {
        CompoundTag data = attacker.getPersistentData();
        CompoundTag all = data.getCompound(CYCLE_KEY);
        CompoundTag entry = all.getCompound(itemId.toString());
        long now = attacker.level().getGameTime();
        long since = now - entry.getLong("tick");
        int slot;
        if (groupSwing && entry.contains("tick") && since >= 0 && since < groupWindowTicks) {
            slot = Math.floorMod(entry.getInt("slot"), size);
        } else {
            slot = Math.floorMod(entry.getInt("next"), size);
            entry.putInt("slot", slot);
            entry.putInt("next", (slot + 1) % size);
            entry.putLong("tick", now);
        }
        all.put(itemId.toString(), entry);
        data.put(CYCLE_KEY, all);
        return slot;
    }

    private static final String PROJECTILE_SLOT_KEY = "tc_cycle_slot";

    /** Posicion del ciclo que lleva este proyectil: se asigna una sola vez (al aparecer o en su primer impacto). */
    private static int projectileSlot(Entity projectile, Player owner, ResourceLocation itemId, int size) {
        CompoundTag data = projectile.getPersistentData();
        if (!data.contains(PROJECTILE_SLOT_KEY)) {
            data.putInt(PROJECTILE_SLOT_KEY, nextIndex(owner, itemId, size, false));
        }
        return Math.floorMod(data.getInt(PROJECTILE_SLOT_KEY), size);
    }

    /** Al aparecer un proyectil de un arma con elemento por proyectil (torbellinos): le toca el siguiente del ciclo. */
    public static void onProjectileSpawn(Entity entity) {
        if (entity.level().isClientSide() || !(entity instanceof net.minecraft.world.entity.projectile.Projectile projectile)
                || !(projectile.getOwner() instanceof Player owner)) {
            return;
        }
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        ResourceLocation itemId = typeId == null ? null : SHOT_ENTITIES.get(typeId);
        WeaponBalance.Spec spec = itemId == null ? null : WeaponBalance.spec(itemId);
        if (spec != null && spec.perProjectile && spec.cycle != null && !spec.cycle.isEmpty()) {
            projectileSlot(entity, owner, itemId, spec.cycle.size());
        }
    }
}
