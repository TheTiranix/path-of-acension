// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Al terminar de cargar, avisa en el log que items de la tabla de balance no existen (mod faltante o id mal escrito). */
@EventBusSubscriber(modid = "tcorigenes", bus = EventBusSubscriber.Bus.MOD)
public final class BalanceValidator {
    private static final Logger LOGGER = LogManager.getLogger();

    private BalanceValidator() {
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        int missing = 0;
        for (ResourceLocation id : WeaponBalance.SPECS.keySet()) {
            if (!ForgeRegistries.ITEMS.containsKey(id)) {
                LOGGER.warn("[tcorigenes] Balance de armas: el item {} no existe (mod faltante o id incorrecto)", id);
                missing++;
            }
        }
        for (WeaponBalance.Family family : WeaponBalance.FAMILIES) {
            for (ResourceLocation id : family.members()) {
                if (!ForgeRegistries.ITEMS.containsKey(id)) {
                    LOGGER.warn("[tcorigenes] Balance de armas: el item {} (familia de {}) no existe", id, family.ref());
                    missing++;
                }
            }
        }
        LOGGER.info("[tcorigenes] Balance de armas cargado: {} armas, {} familias, {} ids inexistentes",
                WeaponBalance.SPECS.size(), WeaponBalance.FAMILIES.size(), missing);
    }
}
