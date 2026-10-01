// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: tecla para desactivar las lanzas negras aereas de la Keres (los proyectiles "keres_shadow" del estado Hellbane
 * que se lanzan solos a los enemigos). Con las lanzas activadas, cada una cuesta 2 de vida del dueño (sin matarlo: si le quedan 2 o
 * menos, esa lanza no sale). El estado se guarda por jugador (por defecto activado).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class KeresShadowControl {
    private static final String KEY = "tc_keres_shadows_off";
    private static final ResourceLocation SHADOW = ResourceLocation.fromNamespaceAndPath("celestisynth", "keres_shadow");
    public static final float COST = 2.0F;

    private KeresShadowControl() {
    }

    public static boolean enabled(Player player) {
        return !player.getPersistentData().getBoolean(KEY);
    }

    public static void set(Player player, boolean enabled) {
        if (enabled) {
            player.getPersistentData().remove(KEY);
        } else {
            player.getPersistentData().putBoolean(KEY, true);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !SHADOW.equals(ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()))) {
            return;
        }
        if (!(event.getEntity() instanceof Projectile projectile)) {
            return;
        }
        Player owner = projectile.getOwner() instanceof Player p ? p : null;
        if (owner == null) {
            // el dueño se asigna a veces despues de que aparece: se toma al jugador mas cercano con la Keres en la mano
            owner = event.getLevel().getNearestPlayer(projectile, 12.0);
            if (owner == null || !"celestisynth:keres".equals(String.valueOf(ForgeRegistries.ITEMS.getKey(owner.getMainHandItem().getItem())))) {
                return;
            }
        }
        if (!enabled(owner)) {
            event.setCanceled(true);
            return;
        }
        if (owner.isCreative() || owner.isSpectator()) {
            return;
        }
        if (owner.getHealth() <= COST) {
            event.setCanceled(true); // no alcanza la sangre
            return;
        }
        owner.setHealth(owner.getHealth() - COST);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(KEY)) {
            event.getEntity().getPersistentData().putBoolean(KEY, true);
        }
    }
}
