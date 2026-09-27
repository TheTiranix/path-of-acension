// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: la armadura de Create y Mekanism (incluye MekanismTools y el MekaSuit) le da un
 * aumento MUY considerable de estadisticas al Autómata (su raza "hermana" tematica: engranajes,
 * mecanismos); a cualquier otra raza no le hace nada de mas (ni de menos: sigue siendo la armadura
 * comun y corriente). No se puede hacer con ItemAttributeModifierEvent (el mismo item se ve igual para
 * TODOS, sin importar quien se lo pone): se revisa cada tick que hay puesto y se suma como atributo del
 * JUGADOR (igual que el resto de bonos de raza, ver RaceAttributeManager/OriginBonuses).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class AutomataMachineArmor {
    private static final String[] MACHINE_NAMESPACES = {"create", "mekanism", "mekanismtools"};
    private static final double ARMOR_PER_PIECE = 12.0;
    private static final double TOUGHNESS_PER_PIECE = 6.0;
    private static final double KNOCKBACK_RES_PER_PIECE = 0.20;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private AutomataMachineArmor() {
    }

    private static UUID armorId(EquipmentSlot slot) {
        return UUID.nameUUIDFromBytes(("tcorigenes:automata_machine_armor:" + slot.getName())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static UUID toughnessId(EquipmentSlot slot) {
        return UUID.nameUUIDFromBytes(("tcorigenes:automata_machine_toughness:" + slot.getName())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static UUID knockbackId(EquipmentSlot slot) {
        return UUID.nameUUIDFromBytes(("tcorigenes:automata_machine_knockback:" + slot.getName())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static boolean isMachineGear(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        for (String namespace : MACHINE_NAMESPACES) {
            if (id.getNamespace().equals(namespace)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        for (EquipmentSlot slot : SLOTS) {
            boolean bonus = race == Race.AUTOMATA && isMachineGear(player.getItemBySlot(slot));
            apply(armor, armorId(slot), "Autómata: engranaje en " + slot.getName(), ARMOR_PER_PIECE, bonus);
            apply(toughness, toughnessId(slot), "Autómata: engranaje en " + slot.getName(), TOUGHNESS_PER_PIECE, bonus);
            apply(knockbackResistance, knockbackId(slot), "Autómata: engranaje en " + slot.getName(), KNOCKBACK_RES_PER_PIECE, bonus);
        }
    }

    private static void apply(AttributeInstance instance, UUID id, String name, double amount, boolean shouldHave) {
        if (instance == null) {
            return;
        }
        boolean has = instance.getModifier(id) != null;
        if (shouldHave && !has) {
            instance.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        } else if (!shouldHave && has) {
            instance.removeModifier(id);
        }
    }
}
