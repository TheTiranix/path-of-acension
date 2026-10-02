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

/** Pedido de alejandr0: la velocidad de ataque de los cuchillos de carnicero (el de piedra y el cleaver de Butcher's Delight) es 1.4. */
@EventBusSubscriber(modid = "tcorigenes")
public final class KnifeSpeed {
    private static final UUID BASE_ATTACK_SPEED = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    public static final double SPEED = 1.4;

    private KnifeSpeed() {
    }

    private static boolean isButcherKnife(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return id.equals(ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "stone_butcher_knife"))
                || id.getNamespace().equals("butchersdelight") && (path.equals("cleaver") || path.contains("knife"));
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND || !isButcherKnife(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()))) {
            return;
        }
        event.removeAttribute(Attributes.ATTACK_SPEED);
        event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED, "Weapon modifier", SPEED - 4.0, AttributeModifier.Operation.ADDITION));
    }
}
