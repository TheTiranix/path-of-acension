// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/** Pedido de alejandr0: el Wrought Helm resta 5% de velocidad y 2.5% de velocidad de ataque (la proteccion esta en WeaponBalance). */
@EventBusSubscriber(modid = "tcorigenes")
public final class WroughtHelmTweaks {
    private static final UUID SPEED = UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000041");
    private static final UUID ATTACK_SPEED = UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000042");

    private WroughtHelmTweaks() {
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.HEAD) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id != null && id.toString().equals("mowziesmobs:wrought_helmet")) {
            event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPEED, "Wrought Helm", -0.05, AttributeModifier.Operation.MULTIPLY_TOTAL));
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED, "Wrought Helm", -0.025, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
