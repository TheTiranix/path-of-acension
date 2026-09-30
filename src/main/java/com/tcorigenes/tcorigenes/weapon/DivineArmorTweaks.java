// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import com.tudominio.elementaldamage.ModAttributes;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Divine Solar Armor (Solar Crystal) y Divine Lunar Armor (Lunar Stone), de Celestisynth (pedidos de alejandr0):
 * 750 de tenacidad de armadura por pieza; ninguna tiene spell resistance ni resistencia magica al fuego; la solar suma
 * 2.5% de poder de hechizos de fuego por pieza y la lunar 2.5% de poder de hechizos lunares por pieza (atributo propio);
 * la lunar no da resistencia al retroceso.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class DivineArmorTweaks {
    private static final double TOUGHNESS = 750.0;
    private static final double SPELL_POWER = 0.025;
    private static final UUID[] TOUGHNESS_IDS = {
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000001"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000002"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000003"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000004")};
    private static final UUID[] POWER_IDS = {
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000011"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000012"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000013"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000014")};

    private static final UUID[] YETI_IDS = {
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000021"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000022"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000023"),
            UUID.fromString("5b1f1c30-0a52-4b1e-9d2a-7e1a00000024")};

    private DivineArmorTweaks() {
    }

    private static int index(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            default -> 3;
        };
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType().getType() != EquipmentSlot.Type.ARMOR) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id != null && id.getNamespace().equals("twilightforest") && id.getPath().startsWith("yeti_")) {
            // Yeti: 10% de resistencia al retroceso por pieza (la proteccion y la tenacidad estan en WeaponBalance).
            event.removeAttribute(Attributes.KNOCKBACK_RESISTANCE);
            event.addModifier(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    YETI_IDS[index(event.getSlotType())], "Armadura de yeti", 0.10, AttributeModifier.Operation.ADDITION));
            return;
        }
        if (id == null || !id.getNamespace().equals("celestisynth")) {
            return;
        }
        boolean solar = id.getPath().startsWith("solar_crystal_");
        boolean lunar = id.getPath().startsWith("lunar_stone_");
        if (!solar && !lunar) {
            return;
        }
        int slot = index(event.getSlotType());
        for (String name : new String[] {"spell_resist", "fire_magic_resist"}) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", name));
            if (attribute != null) {
                event.removeAttribute(attribute);
            }
        }
        event.removeAttribute(Attributes.ARMOR_TOUGHNESS);
        event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                TOUGHNESS_IDS[slot], "Armadura divina", TOUGHNESS, AttributeModifier.Operation.ADDITION));
        if (solar) {
            Attribute firePower = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_spell_power"));
            if (firePower != null) {
                event.addModifier(firePower, new AttributeModifier(
                        POWER_IDS[slot], "Armadura solar divina", SPELL_POWER, AttributeModifier.Operation.MULTIPLY_BASE));
            }
        } else {
            event.removeAttribute(Attributes.KNOCKBACK_RESISTANCE);
            event.addModifier(ModAttributes.LUNAR_SPELL_POWER.get(), new AttributeModifier(
                    POWER_IDS[slot], "Armadura lunar divina", SPELL_POWER, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}
