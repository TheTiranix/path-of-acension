// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/** Burnt: las nubes de humo toxico (entidad "smoke", con hitbox grande) golpeaban a distancia y descolocaban la hitbox: ya no aparecen. */
@EventBusSubscriber(modid = "tcorigenes")
public final class BurntSmokeOff {
    private static final ResourceLocation SMOKE = ResourceLocation.fromNamespaceAndPath("burnt", "smoke");

    private BurntSmokeOff() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && SMOKE.equals(ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()))) {
            event.setCanceled(true);
        }
    }
}
