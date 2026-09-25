// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Durabilidad propia de las armaduras de WeaponBalance#ARMOR_FIXED. La durabilidad maxima vive en un campo final
 * de Item (la fija el material del mod original), asi que se cambia por reflexion al terminar de cargar.
 */
@EventBusSubscriber(modid = "tcorigenes", bus = EventBusSubscriber.Bus.MOD)
public final class ArmorDurability {
    private static final Logger LOGGER = LogManager.getLogger();
    /** SRG de Item#maxDamage (1.20.1). */
    private static final String MAX_DAMAGE_FIELD = "f_41371_";

    private ArmorDurability() {
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            for (var entry : WeaponBalance.ARMOR_FIXED.entrySet()) {
                ResourceLocation id = entry.getKey();
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item == null || !ForgeRegistries.ITEMS.containsKey(id)) {
                    continue;
                }
                try {
                    ObfuscationReflectionHelper.setPrivateValue(Item.class, item, entry.getValue().durability(), MAX_DAMAGE_FIELD);
                } catch (RuntimeException e) {
                    LOGGER.warn("[tcorigenes] No se pudo cambiar la durabilidad de {}", id, e);
                }
            }
        });
    }
}
