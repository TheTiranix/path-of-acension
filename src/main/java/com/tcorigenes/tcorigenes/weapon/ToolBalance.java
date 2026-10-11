// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Velocidad y nivel de minado de las herramientas (pedido de alejandr0), respecto del diamante (velocidad 8, nivel 3):
 * zafiro +3% y el mismo nivel; dark metal +10% y un nivel mas; netherite y gravitite +15% y el nivel del dark metal; fiery y wintry +20% y
 * un nivel mas que el dark metal. Los tiers propios (zafiro, dark metal, wintry) ya traen la velocidad exacta; la de los tiers de otros
 * mods (netherite, gravitite, fiery) no se puede cambiar en el tier, asi que se corrige al romper el bloque.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ToolBalance {
    public static final double DIAMOND_SPEED = 8.0;

    /** speedFactor: multiplo de la velocidad del diamante; level: nivel de minado que se muestra (diamante = 3). */
    public record Stat(double speedFactor, int level) {
        public double speed() {
            return DIAMOND_SPEED * speedFactor;
        }
    }

    private ToolBalance() {
    }

    private static boolean isMiningTool(String path) {
        return path.endsWith("_pickaxe") || path.endsWith("_axe") || path.endsWith("_shovel") || path.endsWith("_hoe");
    }

    /** Stats de minado de la herramienta, o null si no es una de las del pedido. */
    public static Stat statOf(ResourceLocation id) {
        if (id == null || !isMiningTool(id.getPath())) {
            return null;
        }
        String s = id.toString();
        if (s.startsWith("testamentodelacarne:sapphire_")) {
            return new Stat(1.03, 3);
        }
        if (s.startsWith("testamentodelacarne:dark_metal_")) {
            return new Stat(1.10, 4);
        }
        if (s.startsWith("minecraft:netherite_") || s.startsWith("aether:gravitite_")) {
            return new Stat(1.15, 4);
        }
        if (s.startsWith("twilightforest:fiery_") || s.startsWith("testamentodelacarne:wintry_")) {
            return new Stat(1.20, 5);
        }
        return null;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (!(tool.getItem() instanceof TieredItem tiered)) {
            return;
        }
        Stat stat = statOf(ForgeRegistries.ITEMS.getKey(tool.getItem()));
        if (stat == null || tool.getDestroySpeed(event.getState()) <= 1.0F) {
            return; // herramienta ajena al pedido o bloque que no es de su tipo (ahi no usa la velocidad del tier)
        }
        double tierSpeed = tiered.getTier().getSpeed();
        if (tierSpeed > 0.0 && Math.abs(stat.speed() / tierSpeed - 1.0) > 0.001) {
            event.setNewSpeed((float) (event.getNewSpeed() * stat.speed() / tierSpeed));
        }
    }
}
