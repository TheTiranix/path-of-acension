// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Brazalete (slot "bracelet" de Curios): da protection puntos de armadura y resta 3% de velocidad.
 *  Cuero 0.25, hierro 0.5, dark metal 1. Cualquiera sirve para el combo de nado del Ender Warrior. */
public class BrazaleteItem extends Item implements ICurioItem {
    private final double protection;

    public BrazaleteItem(Properties properties, double protection) {
        super(properties);
        this.protection = protection;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid, "Brazalete armor", protection, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid, "Brazalete speed penalty", -0.03, AttributeModifier.Operation.MULTIPLY_TOTAL));
        return modifiers;
    }
}
