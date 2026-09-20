// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/**
 * Casco especial del Ender Warrior: misma proteccion y durabilidad que un casco de diamante
 * (usa ese mismo ArmorMaterial, asi que tambien se renderiza y repara igual), pero resta 3% de
 * velocidad de movimiento y da respiracion acuatica mientras esta puesto (ver ModEvents.onPlayerTick).
 * Es UNA de las dos piezas nuevas que el Ender Warrior necesita para poder nadar sin daño; la
 * otra (brazalete) requiere un slot de Curios que todavia no esta integrado en el mod.
 */
public class EscafandraItem extends ArmorItem {
    private static final double SPEED_MULTIPLIER = -0.03;
    // UUID FIJO: sin el, cada consulta genera uno nuevo y los modificadores se acumulan sin poder quitarse.
    private static final java.util.UUID SPEED_MODIFIER_ID = java.util.UUID.fromString("5c0b2f4e-7a11-4d3b-9e57-3a2f1c8d6b90");

    public EscafandraItem(Properties properties) {
        super(ArmorMaterials.DIAMOND, Type.HELMET, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create(super.getAttributeModifiers(slot, stack));
        if (slot == Type.HELMET.getSlot()) {
            modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                    SPEED_MODIFIER_ID, "Escafandra speed penalty", SPEED_MULTIPLIER, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        return modifiers;
    }
}
