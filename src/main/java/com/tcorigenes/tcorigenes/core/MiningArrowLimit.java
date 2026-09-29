// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: las flechas mineras de Apotheosis no rompen obsidiana ni materiales de resistencia
 * parecida. La flecha rompe bloques a traves de un FakePlayer con la flecha en la mano (BlockUtil#breakExtraBlock),
 * asi que se cancela ese BreakEvent si el bloque es muy duro o muy resistente a explosiones.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class MiningArrowLimit {
    private static final float MAX_HARDNESS = 25.0F;
    private static final float MAX_BLAST_RESISTANCE = 1200.0F;

    private MiningArrowLimit() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof FakePlayer fake)) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(fake.getMainHandItem().getItem());
        if (id == null || !id.getNamespace().equals("apotheosis") || !id.getPath().endsWith("mining_arrow")) {
            return;
        }
        BlockState state = event.getState();
        if (state.getDestroySpeed(event.getLevel(), event.getPos()) >= MAX_HARDNESS
                || state.getBlock().getExplosionResistance() >= MAX_BLAST_RESISTANCE) {
            event.setCanceled(true);
        }
    }
}
