// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import com.tcorigenes.tcorigenes.core.ProgressionTier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * El Altar de Sangre sube de tier con las dimensiones (diseño de la magia): los bloques que lo mejoran solo se pueden colocar con el nivel de
 * progresion que les toca. Runas (tier 2 del altar) desde el nivel 2 (Nether / Aether); ladrillos de piedra de sangre (tier 3) desde el 3
 * (Twilight Forest / Alex's Caves); cristales y maquinas de voluntad demoniaca (tiers altos) desde el 4 (tras el Ender Dragon). Un creativo
 * con permisos no tiene limite.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class BloodMagicGate {
    private BloodMagicGate() {
    }

    private static int requiredTier(String path) {
        if (path.equals("bloodstonebrick") || path.equals("largebloodstonebrick")) {
            return 3;
        }
        if (path.contains("demoncrystal") || path.equals("demonpylon") || path.equals("demoncrucible") || path.equals("demoncrystallizer")) {
            return 4;
        }
        if (path.contains("rune") && !path.contains("ritual")) {
            return 2;
        }
        return 1;
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || (player.isCreative() && player.hasPermissions(2))) {
            return;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        if (id == null || !id.getNamespace().equals("bloodmagic")) {
            return;
        }
        int needed = requiredTier(id.getPath());
        if (needed > ProgressionTier.get(player)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("tcorigenes.magic.blood_gate", needed), true);
        }
    }
}
