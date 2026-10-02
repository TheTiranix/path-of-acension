// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * Pedido de alejandr0: daño de cada guante individual al llevarlo en un slot de guantes de Curios (netherite, valkyrie y gravitite 1.25;
 * neptune 0.75; obsidian 1; zanite 0.5; el resto, valores propios). Cada guante suma su daño: con los dos puestos se duplica.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class GloveBonuses {
    private static final Map<String, Double> DAMAGE = new HashMap<>();

    static {
        DAMAGE.put("leather_glove", 0.25);
        DAMAGE.put("chainmail_glove", 0.5);
        DAMAGE.put("iron_glove", 0.75);
        DAMAGE.put("golden_glove", 0.5);
        DAMAGE.put("diamond_glove", 1.0);
        DAMAGE.put("netherite_glove", 1.25);
        DAMAGE.put("zanite_glove", 0.5);
        DAMAGE.put("gravitite_glove", 1.25);
        DAMAGE.put("valkyrie_glove", 1.25);
        DAMAGE.put("neptune_glove", 0.75);
        DAMAGE.put("obsidian_glove", 1.0);
        DAMAGE.put("phoenix_glove", 1.0);
    }

    private GloveBonuses() {
    }

    @SubscribeEvent
    public static void onAttributes(CurioAttributeModifierEvent event) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !id.getNamespace().equals("testamentodelacarne")) {
            return;
        }
        Double damage = DAMAGE.get(id.getPath());
        if (damage != null) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(event.getUuid(), "Guante", damage, AttributeModifier.Operation.ADDITION));
        }
    }
}
