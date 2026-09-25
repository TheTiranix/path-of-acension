// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Reparte el ciclo de elementos entre los proyectiles que un arma invoca juntos (ver WeaponElemental#onProjectileSpawn). */
@EventBusSubscriber(modid = "tcorigenes")
public final class ProjectileSlotAssigner {
    private ProjectileSlotAssigner() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        WeaponElemental.onProjectileSpawn(event.getEntity());
    }
}
