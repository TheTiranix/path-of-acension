// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tudominio.elementaldamage.ModDamageTypes;
import com.tudominio.elementaldamage.PendingElementalHits;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Daño elemental fijo de ciertas armas de mods de terceros (ver WeaponDamageOverrides para el
 * daño normal/velocidad/alcance de esas mismas armas), a pedido de alejandr0 en Discord. Dos
 * formas, igual filosofia que RacialElemental (un golpe elemental es SIEMPRE aparte, nunca sumado
 * al mismo evento, para que resistencias/indicador se calculen solos):
 * - "+X de <elemento>": se SUMA un golpe elemental aparte en cada golpe (Gravitite, Lightning
 *   Sword, Holy Sword, Valkyrie Lance, Breezebreaker).
 * - "1 de cada N golpes es elemental": en vez de sumar, cada N-esimo golpe se CONVIERTE entero en
 *   un golpe elemental, con el mismo monto (Aquaflora: 1 de cada 4 es agua). La cuenta es fija y
 *   por jugador+arma (nunca al azar, tal como se pidio).
 */
public final class WeaponElemental {
    private record FlatExtra(ResourceKey<DamageType> element, float amount) {
    }

    private record Conversion(ResourceKey<DamageType> element, int everyNth) {
    }

    private static final Map<ResourceLocation, List<FlatExtra>> FLAT_EXTRA = Map.ofEntries(
            Map.entry(rl("aether", "gravitite_sword"), List.of(new FlatExtra(ModDamageTypes.LIGHT, 2.0F))),
            Map.entry(rl("aether", "lightning_sword"), List.of(new FlatExtra(ModDamageTypes.LIGHT, 2.0F))),
            Map.entry(rl("aether", "holy_sword"), List.of(new FlatExtra(ModDamageTypes.LIGHT, 2.0F))),
            Map.entry(rl("aether", "valkyrie_lance"), List.of(new FlatExtra(ModDamageTypes.LIGHT, 3.0F))),
            Map.entry(rl("celestisynth", "breezebreaker"), List.of(new FlatExtra(ModDamageTypes.EARTH, 300.0F))),
            Map.entry(rl("aether", "hammer_of_kingbdogz"), List.of(new FlatExtra(ModDamageTypes.LIGHT, 3.0F))),
            Map.entry(rl("aether", "pig_slayer"), List.of(new FlatExtra(ModDamageTypes.LIGHT, 2.0F), new FlatExtra(ModDamageTypes.EARTH, 2.0F))),
            Map.entry(rl("aether", "flaming_sword"), List.of(new FlatExtra(ModDamageTypes.LIGHT, 3.0F))),
            Map.entry(rl("scary_mobs", "lunar_axe"), List.of(new FlatExtra(ModDamageTypes.LUNAR, 10.0F))),
            Map.entry(rl("cataclysm", "coral_spear"), List.of(new FlatExtra(ModDamageTypes.WATER_ELEMENTAL, 2.5F))),
            Map.entry(rl("iceandfire", "hippogryph_sword"), List.of(new FlatExtra(ModDamageTypes.AIR, 3.0F))),
            Map.entry(rl("seadwellers", "depth_sword"), List.of(new FlatExtra(ModDamageTypes.WATER_ELEMENTAL, 3.0F)))
    );

    /** Suma del daño elemental fijo por golpe de un arma (para el ranking de daño total). */
    public static float flatExtraTotal(ResourceLocation itemId) {
        float total = 0.0F;
        for (FlatExtra extra : FLAT_EXTRA.getOrDefault(itemId, List.of())) {
            total += extra.amount();
        }
        return total;
    }

    private static final Map<ResourceLocation, Conversion> CONVERSION = Map.of(
            rl("celestisynth", "aquaflora"), new Conversion(ModDamageTypes.WATER_ELEMENTAL, 4)
    );

    /** Cuenta de golpes por jugador y por arma, para la alternancia fija de la conversion. */
    private static final Map<UUID, Map<ResourceLocation, Integer>> HIT_COUNTS = new HashMap<>();

    private WeaponElemental() {
    }

    public static void apply(LivingHurtEvent event, Player attacker) {
        if (event.getAmount() <= 0.0F || attacker.level().isClientSide()) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(weapon.getItem());
        if (id == null) {
            return;
        }
        LivingEntity target = event.getEntity();

        Conversion conversion = CONVERSION.get(id);
        if (conversion != null && isNth(attacker, id, conversion.everyNth())) {
            float total = event.getAmount();
            event.setCanceled(true); // este golpe entero pasa a ser el elemental, no se suma aparte
            extra(target, attacker, conversion.element(), total);
            return;
        }

        for (FlatExtra extra : FLAT_EXTRA.getOrDefault(id, List.of())) {
            extra(target, attacker, extra.element(), extra.amount());
        }
    }

    private static void extra(LivingEntity target, Player attacker, ResourceKey<DamageType> element, float amount) {
        PendingElementalHits.queue(target, attacker, element, amount,
                target.level().getGameTime() + PendingElementalHits.SAFE_DELAY_TICKS);
    }

    /** true en el golpe N, N*2, N*3... (empieza a contar desde el primer golpe con esa arma). */
    private static boolean isNth(Player attacker, ResourceLocation itemId, int everyNth) {
        Map<ResourceLocation, Integer> perItem = HIT_COUNTS.computeIfAbsent(attacker.getUUID(), k -> new HashMap<>());
        int count = perItem.merge(itemId, 1, Integer::sum);
        if (count >= everyNth) {
            perItem.put(itemId, 0);
            return true;
        }
        return false;
    }

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
