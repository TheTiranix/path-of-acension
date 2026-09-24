// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import com.tudominio.elementaldamage.ModAttributes;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * Guanteletes de otros mods (Cataclysm, Mutant Monsters, Mowzie's) equipables en el slot de guantes de Curios (via el
 * tag curios:hands, ver data/curios/tags/items/hands.json) y renderizados en las manos (ver
 * client.GauntletRenderers). Las Tidal Claws, ademas, dan +10% de daño normal, de proyectiles y magico al equiparse.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class GauntletCurios {
    private static final ResourceLocation TIDAL_CLAWS = ResourceLocation.fromNamespaceAndPath("cataclysm", "tidal_claws");

    private GauntletCurios() {
    }

    @SubscribeEvent
    public static void onCurioAttributes(CurioAttributeModifierEvent event) {
        if (!TIDAL_CLAWS.equals(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()))
                || !"hands".equals(event.getSlotContext().identifier())) {
            return;
        }
        UUID uuid = event.getUuid();
        event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid, "Tidal Claws (guante)", 0.10, AttributeModifier.Operation.MULTIPLY_TOTAL));
        event.addModifier(ModAttributes.BOW_DAMAGE_MULT.get(), new AttributeModifier(uuid, "Tidal Claws (guante)", 0.10, AttributeModifier.Operation.ADDITION));
        Attribute spell = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_power"));
        if (spell != null) {
            event.addModifier(spell, new AttributeModifier(uuid, "Tidal Claws (guante)", 0.10, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        Attribute projectile = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("attributeslib", "arrow_damage"));
        if (projectile != null) {
            event.addModifier(projectile, new AttributeModifier(uuid, "Tidal Claws (guante)", 0.10, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}
