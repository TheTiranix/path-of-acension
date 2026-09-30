// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido de alejandr0: los dragones salvajes van a buscar al jugador que abre un cofre de su nido o rompe un bloque de su
 * nido (Ice and Fire solo lo hacia con cofres y pilas de oro a 30 bloques). Nido = alrededor de su posicion "home". Los
 * dragones domesticados no reaccionan, y los esqueletos de dragon (isModelDead) tampoco.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class DragonNestAggro {
    /** Radio alrededor del home del dragon que cuenta como "su nido". */
    private static final double NEST_RADIUS = 40.0;
    /** A que distancia del bloque se buscan dragones. */
    private static final double SEARCH_RADIUS = 96.0;

    private DragonNestAggro() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof Level level) {
            alert(level, event.getPos(), player);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Level level = event.getLevel();
        BlockEntity entity = level.getBlockEntity(event.getPos());
        if (entity instanceof Container) {
            alert(level, event.getPos(), player);
        }
    }

    private static void alert(Level level, BlockPos pos, Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        AABB area = new AABB(pos).inflate(SEARCH_RADIUS);
        for (EntityDragonBase dragon : level.getEntitiesOfClass(EntityDragonBase.class, area)) {
            if (dragon.isTame() || dragon.isModelDead() || !dragon.isAlive()) {
                continue;
            }
            BlockPos home = dragon.hasHomePosition && dragon.homePos != null ? dragon.homePos.getPosition() : dragon.blockPosition();
            if (home.distSqr(pos) > NEST_RADIUS * NEST_RADIUS) {
                continue;
            }
            dragon.setInSittingPose(false);
            dragon.setOrderedToSit(false);
            dragon.setTarget(player);
        }
    }
}
