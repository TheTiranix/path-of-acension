// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Bonus de set completo al poder de hechizos de Iron's Spellbooks (pedido de alejandr0): el set Nightmare da +50% de poder lunar (la escuela
 * eldritch, la magia de la Luna) y el set Holy da +50% de poder de luz (la escuela holy).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class SetSpellPower {
    private record Bonus(String prefix, String attribute, UUID id, double amount) {
    }

    private static final Bonus[] BONUSES = {
            new Bonus("born_in_chaos_v1:nightmare_mantleofthe_night_", "irons_spellbooks:eldritch_spell_power",
                    UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a000000a1"), 0.5),
            new Bonus("testamentodelacarne:holy_", "irons_spellbooks:holy_spell_power",
                    UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a000000a2"), 0.5)};

    private SetSpellPower() {
    }

    private static boolean fullSet(ServerPlayer player, String prefix) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getItemBySlot(slot).getItem());
            if (id == null || !id.toString().startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        for (Bonus bonus : BONUSES) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse(bonus.attribute()));
            AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            boolean want = fullSet(player, bonus.prefix());
            boolean has = instance.getModifier(bonus.id()) != null;
            if (want && !has) {
                instance.addTransientModifier(new AttributeModifier(bonus.id(), "Set bonus", bonus.amount(), AttributeModifier.Operation.MULTIPLY_BASE));
            } else if (!want && has) {
                instance.removeModifier(bonus.id());
            }
        }
    }
}
