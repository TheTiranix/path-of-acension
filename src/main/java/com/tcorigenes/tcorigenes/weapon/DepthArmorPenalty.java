// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: cada pieza de la armadura Depth (Sea Dwellers) resta 5% de velocidad de movimiento y 2.5% de velocidad de
 * ataque, pero solo en tierra (fuera del agua). Es condicional, asi que se aplica por tick como modificador transitorio.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class DepthArmorPenalty {
    private static final UUID SPEED = UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000051");
    private static final UUID ATTACK = UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000052");
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private DepthArmorPenalty() {
    }

    private static boolean isDepth(ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("seadwellers") && id.getPath().startsWith("depth_")
                && (id.getPath().endsWith("helmet") || id.getPath().endsWith("chestplate") || id.getPath().endsWith("leggings") || id.getPath().endsWith("boots"));
    }

    private static void apply(Player player, Attribute attribute, UUID id, String name, double perPiece, int pieces) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        double wanted = -perPiece * pieces;
        if (pieces == 0) {
            if (current != null) {
                instance.removeModifier(id);
            }
        } else if (current == null || current.getAmount() != wanted) {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, name, wanted, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || event.player.tickCount % 5 != 0) {
            return;
        }
        Player player = event.player;
        int pieces = 0;
        if (!player.isInWater() && !player.isSwimming()) {
            for (EquipmentSlot slot : ARMOR) {
                if (isDepth(player.getItemBySlot(slot))) {
                    pieces++;
                }
            }
        }
        apply(player, Attributes.MOVEMENT_SPEED, SPEED, "Depth (tierra)", 0.05, pieces);
        apply(player, Attributes.ATTACK_SPEED, ATTACK, "Depth (tierra)", 0.025, pieces);
    }
}
