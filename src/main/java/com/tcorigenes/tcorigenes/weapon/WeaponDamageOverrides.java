// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Rebalanceo de daño/velocidad/alcance de armas de mods de terceros (Aether, Celestisynth), a
 * pedido de alejandr0 en Discord. Estas armas no son nuestras: la única forma de tocar sus
 * atributos sin parchear el jar de esos mods es reemplazarlos via ItemAttributeModifierEvent cada
 * vez que se consultan (tooltip, combate). El daño elemental fijo de estas mismas armas está en
 * WeaponElemental, no acá (ese daño es un golpe APARTE, no un atributo de item).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class WeaponDamageOverrides {
    private record Override(double damage, Double attackSpeed, Double reach, boolean twoHanded) {
    }

    /** Base vanilla de un jugador sin nada en la mano: daño 1, velocidad 4, alcance 3. Los
     *  modifiers son ADDITION, asi que hay que restar la base para llegar al total pedido. */
    private static final double BASE_DAMAGE = 1.0;
    private static final double BASE_SPEED = 4.0;
    private static final double BASE_REACH = 3.0;

    private static final UUID DAMAGE_UUID = UUID.fromString("c15a5a00-0001-4a55-8a00-000000000001");
    private static final UUID SPEED_UUID = UUID.fromString("c15a5a00-0002-4a55-8a00-000000000002");
    private static final UUID REACH_UUID = UUID.fromString("c15a5a00-0003-4a55-8a00-000000000003");

    private static final Map<ResourceLocation, Override> OVERRIDES = Map.of(
            rl("aether", "gravitite_sword"), new Override(12.0, null, null, false),
            rl("aether", "lightning_sword"), new Override(12.0, null, null, false),
            rl("aether", "holy_sword"), new Override(12.0, null, null, false),
            rl("aether", "valkyrie_lance"), new Override(9.0, 1.5, 3.5, true),
            rl("celestisynth", "aquaflora"), new Override(1800.0, null, null, false),
            rl("celestisynth", "breezebreaker"), new Override(1700.0, null, null, false)
    );

    private WeaponDamageOverrides() {
    }

    /** Usado por TwoHandedWeapons para saber si hay que vaciarle el offhand a quien la empuña. */
    public static boolean isTwoHanded(ResourceLocation itemId) {
        Override override = OVERRIDES.get(itemId);
        return override != null && override.twoHanded();
    }

    @SubscribeEvent
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        Override override = id == null ? null : OVERRIDES.get(id);
        if (override == null) {
            return;
        }
        event.removeAttribute(Attributes.ATTACK_DAMAGE);
        event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                DAMAGE_UUID, "Rebalanceo de arma", override.damage() - BASE_DAMAGE, AttributeModifier.Operation.ADDITION));

        if (override.attackSpeed() != null) {
            event.removeAttribute(Attributes.ATTACK_SPEED);
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                    SPEED_UUID, "Rebalanceo de arma", override.attackSpeed() - BASE_SPEED, AttributeModifier.Operation.ADDITION));
        }
        if (override.reach() != null) {
            Attribute reachAttribute = ForgeMod.ENTITY_REACH.get();
            event.removeAttribute(reachAttribute);
            event.addModifier(reachAttribute, new AttributeModifier(
                    REACH_UUID, "Rebalanceo de arma", override.reach() - BASE_REACH, AttributeModifier.Operation.ADDITION));
        }
    }

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
