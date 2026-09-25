// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.weapon.WeaponBalance;
import com.tudominio.elementaldamage.PendingElementalHits;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
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
 *   indicado (el daño total del golpe pasa a ser de ese elemento); los "normal" pegan como siempre. La
 *   cuenta es fija por jugador+arma, nunca al azar.
 */
public final class WeaponElemental {
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

    public static void apply(LivingHurtEvent event, Player attacker) {
        if (event.getAmount() <= 0.0F || attacker.level().isClientSide()) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(weapon.getItem());
        WeaponBalance.Spec spec = id == null ? null : WeaponBalance.spec(id);
        if (spec == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        ResourceKey<DamageType> converted = ElementalRestriction.converterElement(attacker);
        ResourceKey<DamageType> active = ElementalRestriction.activeElement(attacker, spec);

        if (spec.cycle != null && !spec.cycle.isEmpty()) {
            ResourceKey<DamageType> slot = nextSlot(attacker, id, spec.cycle);
            if (slot != null) {
                if (converted != null) {
                    slot = converted; // el Prisma Convertidor manda
                } else if (!slot.equals(active)) {
                    slot = null; // elemento que no le funciona a este jugador: el golpe queda normal
                }
            }
            if (slot != null) {
                float total = event.getAmount();
                event.setCanceled(true); // este golpe entero pasa a ser el elemental, no se suma aparte
                extra(target, attacker, slot, total);
                return;
            }
        }
        for (WeaponBalance.Extra extra : spec.extras) {
            if (converted != null) {
                extra(target, attacker, converted, extra.amount());
            } else if (extra.element().equals(active)) {
                extra(target, attacker, extra.element(), extra.amount());
            } // cualquier otro elemento del equipo directamente no funciona (ver ElementalRestriction)
        }
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
    private static ResourceKey<DamageType> nextSlot(Player attacker, ResourceLocation itemId, List<ResourceKey<DamageType>> cycle) {
        CompoundTag data = attacker.getPersistentData();
        CompoundTag all = data.getCompound(CYCLE_KEY);
        CompoundTag entry = all.getCompound(itemId.toString());
        long now = attacker.level().getGameTime();
        long since = now - entry.getLong("tick");
        int slot;
        if (entry.contains("tick") && since >= 0 && since < SWING_GROUP_TICKS) {
            slot = Math.floorMod(entry.getInt("slot"), cycle.size());
        } else {
            slot = Math.floorMod(entry.getInt("next"), cycle.size());
            entry.putInt("slot", slot);
            entry.putInt("next", (slot + 1) % cycle.size());
            entry.putLong("tick", now);
        }
        all.put(itemId.toString(), entry);
        data.put(CYCLE_KEY, all);
        return cycle.get(slot);
    }
}
