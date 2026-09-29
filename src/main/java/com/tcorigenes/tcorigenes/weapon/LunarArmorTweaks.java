// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/** Pedido de alejandr0: la armadura lunar (Lunar Stone, Celestisynth) no da resistencia al retroceso. */
@EventBusSubscriber(modid = "tcorigenes")
public final class LunarArmorTweaks {
    private LunarArmorTweaks() {
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType().getType() != EquipmentSlot.Type.ARMOR) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id != null && id.getNamespace().equals("celestisynth") && id.getPath().startsWith("lunar_stone_")) {
            event.removeAttribute(Attributes.KNOCKBACK_RESISTANCE);
        }
    }
}
