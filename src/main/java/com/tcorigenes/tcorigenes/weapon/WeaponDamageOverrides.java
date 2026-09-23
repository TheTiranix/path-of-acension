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

    private static final Map<ResourceLocation, Override> OVERRIDES = new java.util.HashMap<>();

    /** Items cuyo daño de fabrica se MULTIPLICA (en vez de fijarse), para escalar en bloque un set. */
    private static final Map<ResourceLocation, Double> SCALES = new java.util.HashMap<>();

    private static void put(String ns, String path, double damage, boolean twoHanded) {
        OVERRIDES.put(rl(ns, path), new Override(damage, null, null, twoHanded));
    }

    static {
        put("aether", "gravitite_sword", 12.0, false);
        put("aether", "lightning_sword", 12.0, false);
        put("aether", "holy_sword", 12.0, false);
        OVERRIDES.put(rl("aether", "valkyrie_lance"), new Override(9.0, 1.5, 3.5, true));
        put("celestisynth", "aquaflora", 1800.0, false);
        put("celestisynth", "breezebreaker", 1700.0, false);
        put("born_in_chaos_v1", "spiritual_sword", 6.0, false);
        put("iceandfire", "silver_sword", 6.0, false);
        put("cataclysm", "khopesh", 6.5, false);
        put("iceandfire", "myrmex_desert_sword", 6.0, false);
        put("iceandfire", "myrmex_jungle_sword", 6.0, false);
        put("iceandfire", "myrmex_desert_sword_venom", 6.0, false);
        put("iceandfire", "myrmex_jungle_sword_venom", 6.0, false);
        put("iceandfire", "dread_sword", 6.5, false);
        put("iceandfire", "amphithere_macuahuitl", 7.0, false);
        put("mekanismtools", "lapis_lazuli_sword", 5.5, false);
        put("aether", "hammer_of_kingbdogz", 9.0, false);
        put("aether", "pig_slayer", 10.0, false);
        put("aether", "zanite_sword", 13.0, false);
        put("born_in_chaos_v1", "frostbitten_blade", 12.0, true);
        put("eeeabsmobs", "immortal_sword", 8.0, false);
        put("scary_mobs", "lunar_axe", 9.0, true);
        put("cataclysm", "coral_spear", 4.0, false);
        put("iceandfire", "hippogryph_sword", 6.0, false);
        put("seadwellers", "depth_sword", 6.0, false);
        put("aether", "flaming_sword", 9.0, false);
        // Resto del set Zanite: mismo factor que la espada (13 sobre los 6 de hierro de la que parte).
        for (String tool : new String[] {"zanite_axe", "zanite_pickaxe", "zanite_shovel", "zanite_hoe"}) {
            SCALES.put(rl("aether", tool), 13.0 / 6.0);
        }
    }

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
        Double scale = id == null ? null : SCALES.get(id);
        if (scale != null) {
            java.util.List<AttributeModifier> originals = new java.util.ArrayList<>(event.getOriginalModifiers().get(Attributes.ATTACK_DAMAGE));
            event.removeAttribute(Attributes.ATTACK_DAMAGE);
            for (AttributeModifier original : originals) {
                double amount = original.getOperation() == AttributeModifier.Operation.ADDITION
                        ? original.getAmount() * scale : original.getAmount();
                event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        original.getId(), original.getName(), amount, original.getOperation()));
            }
            return;
        }
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
