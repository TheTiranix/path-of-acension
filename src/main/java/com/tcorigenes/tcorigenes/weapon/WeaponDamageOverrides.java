// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Aplica la tabla de balance (WeaponBalance) a los atributos de las armas/armaduras de mods de terceros.
 * No se pueden tocar sus clases sin parchear los jars, asi que se reemplazan los modifiers cada vez que
 * se consultan (tooltip, combate) via ItemAttributeModifierEvent. El daño elemental fijo esta en
 * WeaponElemental (es un golpe APARTE, no un atributo).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class WeaponDamageOverrides {
    private static final double BASE_DAMAGE = 1.0;
    private static final double BASE_SPEED = 4.0;
    private static final double BASE_REACH = 3.0;

    private static final UUID DAMAGE_UUID = UUID.fromString("c15a5a00-0001-4a55-8a00-000000000001");
    private static final UUID SPEED_UUID = UUID.fromString("c15a5a00-0002-4a55-8a00-000000000002");
    private static final UUID REACH_UUID = UUID.fromString("c15a5a00-0003-4a55-8a00-000000000003");

    private static final Map<ResourceLocation, Double> FACTOR_CACHE = new HashMap<>();

    private WeaponDamageOverrides() {
    }

    /** Usado por TwoHandedWeapons para saber si hay que vaciarle el offhand a quien la empuña. */
    public static boolean isTwoHanded(ResourceLocation itemId) {
        return WeaponBalance.isTwoHanded(itemId);
    }

    /** Suma de modifiers ADDITION de un atributo en la mano principal, de fabrica, mas la base del jugador. */
    static double originalTotal(ResourceLocation itemId, Attribute attribute, double base, EquipmentSlot slot) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return Double.NaN;
        }
        double total = base;
        for (AttributeModifier modifier : item.getDefaultAttributeModifiers(slot).get(attribute)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                total += modifier.getAmount();
            }
        }
        return total;
    }

    private static double sum(java.util.Collection<AttributeModifier> modifiers) {
        double total = 0.0;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                total += modifier.getAmount();
            }
        }
        return total;
    }

    /** Daño normal total final (ya rebalanceado) de un item de mano principal, sin necesitar el evento. */
    public static double finalDamage(ResourceLocation id) {
        WeaponBalance.Spec spec = WeaponBalance.spec(id);
        if (spec != null && spec.damage != null) {
            return spec.damage;
        }
        if (spec != null && spec.damageLike != null) {
            return originalTotal(spec.damageLike, Attributes.ATTACK_DAMAGE, BASE_DAMAGE, EquipmentSlot.MAINHAND);
        }
        double original = originalTotal(id, Attributes.ATTACK_DAMAGE, BASE_DAMAGE, EquipmentSlot.MAINHAND);
        WeaponBalance.Family family = familyOf(id);
        if (Double.isNaN(original)) {
            return 6.0;
        }
        return family != null ? original * familyFactor(family) : original;
    }

    /** Velocidad de ataque total final de un item de mano principal. */
    public static double finalSpeed(ResourceLocation id) {
        WeaponBalance.Spec spec = WeaponBalance.spec(id);
        if (spec != null && spec.speed != null) {
            return spec.speed;
        }
        double original = originalTotal(id, Attributes.ATTACK_SPEED, BASE_SPEED, EquipmentSlot.MAINHAND);
        return Double.isNaN(original) ? 1.6 : original;
    }

    private static Double familyFactor(WeaponBalance.Family family) {
        return FACTOR_CACHE.computeIfAbsent(family.ref(), ref -> {
            double original = originalTotal(ref, Attributes.ATTACK_DAMAGE, BASE_DAMAGE, EquipmentSlot.MAINHAND);
            return Double.isNaN(original) || original <= 0.0 ? 1.0 : family.refTarget() / original;
        });
    }

    private static WeaponBalance.Family familyOf(ResourceLocation id) {
        for (WeaponBalance.Family family : WeaponBalance.FAMILIES) {
            if (family.members().contains(id)) {
                return family;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null) {
            return;
        }
        EquipmentSlot slot = event.getSlotType();
        if (slot != EquipmentSlot.MAINHAND) {
            applyArmor(event, id);
            return;
        }
        WeaponBalance.Spec spec = WeaponBalance.spec(id);
        if (spec != null && spec.ranged) {
            return; // el daño de un arco es por impacto del proyectil (ver WeaponElemental), no un atributo
        }
        WeaponBalance.Family family = familyOf(id);

        // Daño normal total.
        double newDamageTotal = Double.NaN;
        if (spec != null && spec.damage != null) {
            newDamageTotal = spec.damage;
        } else if (spec != null && spec.damageLike != null) {
            newDamageTotal = originalTotal(spec.damageLike, Attributes.ATTACK_DAMAGE, BASE_DAMAGE, EquipmentSlot.MAINHAND);
        } else if (family != null) {
            double original = BASE_DAMAGE + sum(event.getOriginalModifiers().get(Attributes.ATTACK_DAMAGE));
            newDamageTotal = original * familyFactor(family);
        }
        if (id.getPath().endsWith("_paxel") && id.getNamespace().equals("mekanismtools")) {
            double original = BASE_DAMAGE + sum(event.getOriginalModifiers().get(Attributes.ATTACK_DAMAGE));
            newDamageTotal = Math.max(BASE_DAMAGE, (Double.isNaN(newDamageTotal) ? original : newDamageTotal) - 5.0);
        }
        if (!Double.isNaN(newDamageTotal)) {
            event.removeAttribute(Attributes.ATTACK_DAMAGE);
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    DAMAGE_UUID, "Rebalanceo de arma", newDamageTotal - BASE_DAMAGE, AttributeModifier.Operation.ADDITION));
        }

        // Velocidad de ataque total.
        double newSpeedTotal = Double.NaN;
        if (spec != null && spec.speed != null) {
            newSpeedTotal = spec.speed;
        } else if (WeaponBalance.SPEED_FAMILY_REF.containsKey(id)) {
            ResourceLocation ref = WeaponBalance.SPEED_FAMILY_REF.get(id);
            WeaponBalance.Spec refSpec = WeaponBalance.spec(ref);
            double refOriginal = originalTotal(ref, Attributes.ATTACK_SPEED, BASE_SPEED, EquipmentSlot.MAINHAND);
            if (refSpec != null && refSpec.speed != null && !Double.isNaN(refOriginal) && refOriginal > 0.0) {
                newSpeedTotal = (BASE_SPEED + sum(event.getOriginalModifiers().get(Attributes.ATTACK_SPEED))) * refSpec.speed / refOriginal;
            }
        }
        if (!Double.isNaN(newSpeedTotal)) {
            event.removeAttribute(Attributes.ATTACK_SPEED);
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                    SPEED_UUID, "Rebalanceo de arma", newSpeedTotal - BASE_SPEED, AttributeModifier.Operation.ADDITION));
        }

        // Alcance.
        if (spec != null && spec.reach != null) {
            Attribute reachAttribute = ForgeMod.ENTITY_REACH.get();
            event.removeAttribute(reachAttribute);
            event.addModifier(reachAttribute, new AttributeModifier(
                    REACH_UUID, "Rebalanceo de arma", spec.reach - BASE_REACH, AttributeModifier.Operation.ADDITION));
        }
        if (spec != null && spec.removeBlockReach) {
            event.removeAttribute(ForgeMod.BLOCK_REACH.get());
        }
    }

    /** Armaduras: proteccion igualada a otra pieza (diamante) o escalada por la relacion del dark metal. */
    private static void applyArmor(ItemAttributeModifierEvent event, ResourceLocation id) {
        WeaponBalance.ArmorFixed fixed = WeaponBalance.ARMOR_FIXED.get(id);
        if (fixed != null) {
            Item reference = ForgeRegistries.ITEMS.getValue(fixed.uuidRef());
            if (reference != null) {
                event.removeAttribute(Attributes.ARMOR);
                event.removeAttribute(Attributes.ARMOR_TOUGHNESS);
                for (var entry : reference.getDefaultAttributeModifiers(event.getSlotType()).entries()) {
                    double amount = entry.getKey() == Attributes.ARMOR ? fixed.defense()
                            : entry.getKey() == Attributes.ARMOR_TOUGHNESS ? fixed.toughness() : Double.NaN;
                    if (!Double.isNaN(amount)) {
                        // mismo UUID por ranura que la pieza de referencia (evita choques entre piezas equipadas)
                        event.addModifier(entry.getKey(), new AttributeModifier(
                                entry.getValue().getId(), entry.getValue().getName(), amount, AttributeModifier.Operation.ADDITION));
                    }
                }
            }
            return;
        }
        ResourceLocation like = WeaponBalance.ARMOR_LIKE.get(id);
        if (like != null) {
            Item reference = ForgeRegistries.ITEMS.getValue(like);
            if (reference != null) {
                event.removeAttribute(Attributes.ARMOR);
                event.removeAttribute(Attributes.ARMOR_TOUGHNESS);
                for (var entry : reference.getDefaultAttributeModifiers(event.getSlotType()).entries()) {
                    if (entry.getKey() == Attributes.ARMOR || entry.getKey() == Attributes.ARMOR_TOUGHNESS) {
                        // mismo UUID por ranura que la pieza de referencia (evita choques entre piezas equipadas)
                        event.addModifier(entry.getKey(), entry.getValue());
                    }
                }
            }
            return;
        }
        ResourceLocation scaleRef = WeaponBalance.ARMOR_SCALE_REF.get(id);
        if (scaleRef != null) {
            WeaponBalance.Spec refSpec = WeaponBalance.spec(scaleRef);
            double original = originalTotal(scaleRef, Attributes.ATTACK_DAMAGE, BASE_DAMAGE, EquipmentSlot.MAINHAND);
            if (refSpec != null && refSpec.damage != null && !Double.isNaN(original) && original > 0.0) {
                double factor = refSpec.damage / original;
                List<AttributeModifier> originals = new java.util.ArrayList<>(event.getOriginalModifiers().get(Attributes.ARMOR));
                event.removeAttribute(Attributes.ARMOR);
                for (AttributeModifier modifier : originals) {
                    double amount = modifier.getOperation() == AttributeModifier.Operation.ADDITION
                            ? modifier.getAmount() * factor : modifier.getAmount();
                    event.addModifier(Attributes.ARMOR, new AttributeModifier(
                            modifier.getId(), modifier.getName(), amount, modifier.getOperation()));
                }
            }
        }
    }
}
