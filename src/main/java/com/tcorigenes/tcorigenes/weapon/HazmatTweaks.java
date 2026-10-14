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
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * Hazmat (Mekanism y Alex's Caves, pedido de alejandr0): cada pieza resta 2.5 de velocidad de ataque puesta en cualquier slot (el de armadura
 * o el de ropa de Curios). Que se pueda llevar como ropa esta en los tags de Curios (data/curios/tags/items/*_clothes.json) y su proteccion
 * (1 por pieza) en WeaponBalance.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class HazmatTweaks {
    private static final double ATTACK_SPEED = -2.5;
    private static final UUID[] IDS = {
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a000000b0"), UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a000000b1"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a000000b2"), UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a000000b3")};

    private HazmatTweaks() {
    }

    private static int pieceIndex(ResourceLocation id) {
        if (id == null) {
            return -1;
        }
        String s = id.toString();
        if (s.startsWith("mekanism:hazmat_")) {
            return switch (id.getPath()) {
                case "hazmat_mask" -> 0;
                case "hazmat_gown" -> 1;
                case "hazmat_pants" -> 2;
                case "hazmat_boots" -> 3;
                default -> -1;
            };
        }
        if (s.startsWith("alexscaves:hazmat_")) {
            return switch (id.getPath()) {
                case "hazmat_mask" -> 0;
                case "hazmat_chestplate" -> 1;
                case "hazmat_leggings" -> 2;
                case "hazmat_boots" -> 3;
                default -> -1;
            };
        }
        return -1;
    }

    @SubscribeEvent
    public static void onArmorAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType().getType() != EquipmentSlot.Type.ARMOR) {
            return;
        }
        int index = pieceIndex(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()));
        if (index >= 0) {
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(IDS[index], "Hazmat", ATTACK_SPEED, AttributeModifier.Operation.ADDITION));
        }
    }

    @SubscribeEvent
    public static void onCurioAttributes(CurioAttributeModifierEvent event) {
        int index = pieceIndex(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()));
        if (index >= 0) {
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(IDS[index], "Hazmat", ATTACK_SPEED, AttributeModifier.Operation.ADDITION));
        }
    }
}
