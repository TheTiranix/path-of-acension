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
 * Durabilidad propia de las armaduras de WeaponBalance#ARMOR_FIXED (0 = irrompible) y ARMOR_DURABILITY_LIKE. La durabilidad maxima vive en un campo final
 * de Item (la fija el material del mod original), asi que se cambia por reflexion al terminar de cargar.
 */
@EventBusSubscriber(modid = "tcorigenes", bus = EventBusSubscriber.Bus.MOD)
public final class ArmorDurability {
    private static final Logger LOGGER = LogManager.getLogger();
    /** SRG de Item#maxDamage (1.20.1). */
    private static final String MAX_DAMAGE_FIELD = "f_41371_";

    private ArmorDurability() {
    }

    private static void setMaxDamage(Item item, ResourceLocation id, int durability) {
        try {
            ObfuscationReflectionHelper.setPrivateValue(Item.class, item, durability, MAX_DAMAGE_FIELD);
        } catch (RuntimeException e) {
            LOGGER.warn("[tcorigenes] No se pudo cambiar la durabilidad de {}", id, e);
        }
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
                setMaxDamage(item, id, entry.getValue().durability());
            }
            // Todas las armas y armaduras de Celestisynth son irrompibles
            for (var entry : ForgeRegistries.ITEMS.getEntries()) {
                Item item = entry.getValue();
                if (entry.getKey().location().getNamespace().equals("celestisynth") && item.getMaxDamage() > 0) {
                    setMaxDamage(item, entry.getKey().location(), 0);
                }
            }
            for (var entry : WeaponBalance.ARMOR_DURABILITY_LIKE.entrySet()) {
                Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
                Item reference = ForgeRegistries.ITEMS.getValue(entry.getValue());
                if (item != null && reference != null && ForgeRegistries.ITEMS.containsKey(entry.getKey())) {
                    setMaxDamage(item, entry.getKey(), reference.getMaxDamage());
                }
            }
        });
    }
}
