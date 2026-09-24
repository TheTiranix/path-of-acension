// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.weapon.WeaponBalance;
import com.tudominio.elementaldamage.ModDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Un jugador solo puede usar UN elemento a la vez (pedido de alejandr0):
 * - Con Orbe/Prisma Convertidor en el inventario: todo el daño elemental del equipo se convierte a ese elemento.
 * - Si su raza tiene un daño elemental base (Demonio fuego, Angel luz, Siervo de la Luna lunar, Ender Warrior
 *   ender, Gigante Rocoso tierra), solo funciona ESE elemento: el resto de lo que agrega el equipo directamente
 *   no funciona (no pega, no solo "sin efecto especial").
 * - Sin elemento racial, funciona el elemento mas grande que aporte el arma en mano.
 */
public final class ElementalRestriction {
    public static final String CONVERTER_TAG = "tc_convert_element";

    private ElementalRestriction() {
    }

    public static ResourceKey<DamageType> raceElement(Player player) {
        Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
        return switch (race) {
            case DEMONIO -> ModDamageTypes.FIRE_ELEMENTAL;
            case ANGEL -> ModDamageTypes.LIGHT;
            case SIERVO_DE_LA_LUNA -> ModDamageTypes.LUNAR;
            case ENDER_WARRIOR -> ModDamageTypes.ENDER_ELEMENTAL;
            case STONE_GIANT -> ModDamageTypes.EARTH;
            default -> null;
        };
    }

    /** Elemento al que convierte el Prisma Convertidor que lleva el jugador (null si no lleva ninguno configurado). */
    public static ResourceKey<DamageType> converterElement(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.hasTag() && stack.getTag().contains(CONVERTER_TAG)) {
                String path = stack.getTag().getString(CONVERTER_TAG);
                if (!path.isEmpty()) {
                    return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("elementaldamage", path));
                }
            }
        }
        return null;
    }

    /** El elemento que le funciona a este jugador con esta arma (null = ninguno de los elementales del equipo). */
    public static ResourceKey<DamageType> activeElement(Player player, WeaponBalance.Spec spec) {
        ResourceKey<DamageType> race = raceElement(player);
        if (race != null) {
            return race;
        }
        // sin elemento racial: el mas grande que aporta el arma
        ResourceKey<DamageType> best = null;
        float bestAmount = -1.0F;
        if (spec != null) {
            for (WeaponBalance.Extra extra : spec.extras) {
                if (extra.amount() > bestAmount) {
                    bestAmount = extra.amount();
                    best = extra.element();
                }
            }
            if (spec.cycle != null && spec.damage != null) {
                for (ResourceKey<DamageType> slot : spec.cycle) {
                    if (slot != null && spec.damage > bestAmount) {
                        bestAmount = spec.damage.floatValue();
                        best = slot;
                    }
                }
            }
        }
        return best;
    }
}
