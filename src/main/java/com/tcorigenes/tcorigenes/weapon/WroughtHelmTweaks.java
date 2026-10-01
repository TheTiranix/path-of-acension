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

/** Pedido de alejandr0: cada pieza del set de Wrought (casco, peto, pantalones y botas) resta 5% de velocidad y 2.5% de velocidad de ataque (la proteccion esta en WeaponBalance). */
@EventBusSubscriber(modid = "tcorigenes")
public final class WroughtHelmTweaks {
    // un UUID por ranura: las cuatro piezas puestas se suman (5% de velocidad y 2.5% de velocidad de ataque cada una)
    private static final UUID[] SPEED = {
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000041"), UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000043"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000044"), UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000045")};
    private static final UUID[] ATTACK_SPEED = {
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000042"), UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000046"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000047"), UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000048")};

    private WroughtHelmTweaks() {
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        EquipmentSlot slot = event.getSlotType();
        if (slot.getType() != EquipmentSlot.Type.ARMOR) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null) {
            return;
        }
        String s = id.toString();
        boolean helm = s.equals("mowziesmobs:wrought_helmet") && slot == EquipmentSlot.HEAD;
        boolean ours = s.equals("testamentodelacarne:wrought_chestplate") && slot == EquipmentSlot.CHEST
                || s.equals("testamentodelacarne:wrought_leggings") && slot == EquipmentSlot.LEGS
                || s.equals("testamentodelacarne:wrought_boots") && slot == EquipmentSlot.FEET;
        if (helm || ours) {
            int index = switch (slot) {
                case HEAD -> 0;
                case CHEST -> 1;
                case LEGS -> 2;
                default -> 3;
            };
            event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPEED[index], "Wrought", -0.05, AttributeModifier.Operation.MULTIPLY_TOTAL));
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED[index], "Wrought", -0.025, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
