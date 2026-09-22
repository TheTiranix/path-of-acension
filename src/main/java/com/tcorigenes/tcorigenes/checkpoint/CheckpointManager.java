// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Sistema de "puntos de guardado": el respawn ya no depende de la ultima cama en la que dormiste (eso se
 * ignora a proposito, ver README), sino de una lista COMPARTIDA de camas que los jugadores colocaron.
 * <p>
 * Colocar una cama la agrega a la lista como punto activo. Las camas anteriores DEL MISMO JUGADOR siguen
 * siendo validas durante 1 dia entero (24000 ticks) mas: recien despues de eso dejan de estar en la lista.
 * Romper la cama de un punto de guardado lo invalida al toque. Todo el mundo puede reaparecer/ser revivido
 * en CUALQUIER punto activo de la lista (no solo el propio), eligiendolo con {@code /respawnpoint}.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class CheckpointManager {
    /** 1 dia de Minecraft. */
    private static final long GRACE_PERIOD_TICKS = 24000;
    private static final String PREFERRED_KEY = "tc_preferred_checkpoint";

    private CheckpointManager() {
    }

    @SubscribeEvent
    public static void onBedPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getPlacedBlock().is(BlockTags.BEDS)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CheckpointSavedData data = CheckpointSavedData.get(server);
        long now = player.level().getGameTime();
        for (Checkpoint checkpoint : data.checkpoints) {
            if (checkpoint.owner.equals(player.getUUID()) && checkpoint.isActive(now)) {
                checkpoint.supersededAtTick = now + GRACE_PERIOD_TICKS;
            }
        }
        Checkpoint created = new Checkpoint(UUID.randomUUID(), player.getUUID(), player.level().dimension(),
                event.getPos().immutable(), now);
        data.checkpoints.add(created);
        data.setDirty();
        CheckpointSnapshotter.takeSnapshotAsync(server, created.id);
        player.displayClientMessage(Component.literal(
                "Nuevo punto de guardado. Guardando una copia del mundo... Tus camas anteriores dejan de servir en 1 día.")
                .withStyle(ChatFormatting.GOLD), false);
    }

    @SubscribeEvent
    public static void onRespawnBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !event.getPlacedBlock().is(com.tcorigenes.tcorigenes.block.ModBlocks.PLAYER_RESPAWN_BLOCK.get())) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (!hasActiveNear(server, player.level().dimension(), event.getPos(),
                com.tcorigenes.tcorigenes.block.PlayerRespawnBlock.MAX_RANGE_TO_CHECKPOINT)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(
                    "Necesita un punto de guardado activo a 30 bloques o menos.").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onBedBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getState().is(BlockTags.BEDS)) {
            return;
        }
        MinecraftServer server = level.getServer();
        CheckpointSavedData data = CheckpointSavedData.get(server);
        List<Checkpoint> removed = new ArrayList<>();
        data.checkpoints.removeIf(checkpoint -> {
            boolean match = checkpoint.dimension.equals(level.dimension()) && checkpoint.pos.equals(event.getPos());
            if (match) {
                removed.add(checkpoint);
            }
            return match;
        });
        if (!removed.isEmpty()) {
            data.setDirty();
            removed.forEach(checkpoint -> CheckpointSnapshotter.deleteSnapshot(server, checkpoint.id));
        }
    }

    /** Cada 5 minutos: saca de la lista (y borra su copia del mundo) los puntos que ya vencieron del
     *  todo, para no acumular snapshots viejos para siempre. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server.getTickCount() % (20 * 60 * 5) != 0) {
            return;
        }
        CheckpointSavedData data = CheckpointSavedData.get(server);
        long now = server.overworld().getGameTime();
        List<Checkpoint> expired = new ArrayList<>();
        data.checkpoints.removeIf(checkpoint -> {
            boolean expiredNow = !checkpoint.isActive(now);
            if (expiredNow) {
                expired.add(checkpoint);
            }
            return expiredNow;
        });
        if (!expired.isEmpty()) {
            data.setDirty();
            expired.forEach(checkpoint -> CheckpointSnapshotter.deleteSnapshot(server, checkpoint.id));
        }
    }

    /** Sin espera ni fantasma: cualquier respawn "de verdad" (desangrado del todo, te rendiste, o
     *  PlayerRevive no esta instalado) va al punto de guardado, no al spawn del mundo. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            teleportToCheckpoint(player, pickFor(player));
        }
    }

    /** Todos los puntos activos ahora mismo, mas nuevo primero. */
    public static List<Checkpoint> active(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        List<Checkpoint> list = new ArrayList<>();
        for (Checkpoint checkpoint : CheckpointSavedData.get(server).checkpoints) {
            if (checkpoint.isActive(now)) {
                list.add(checkpoint);
            }
        }
        list.sort(Comparator.comparingLong((Checkpoint c) -> c.placedAtTick).reversed());
        return list;
    }

    /** True si hay al menos un punto de guardado activo a `range` bloques de `pos` en esa dimension. */
    public static boolean hasActiveNear(MinecraftServer server, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                        BlockPos pos, double range) {
        for (Checkpoint checkpoint : active(server)) {
            if (checkpoint.dimension.equals(dimension) && Math.sqrt(checkpoint.pos.distSqr(pos)) <= range) {
                return true;
            }
        }
        return false;
    }

    public static void setPreferred(Player player, UUID checkpointId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putUUID(PREFERRED_KEY, checkpointId);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** El punto preferido del jugador si sigue activo; si no, el mas nuevo de todos; null si no hay ninguno. */
    public static Checkpoint pickFor(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        List<Checkpoint> activeList = active(server);
        if (activeList.isEmpty()) {
            return null;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.hasUUID(PREFERRED_KEY)) {
            UUID preferred = persisted.getUUID(PREFERRED_KEY);
            for (Checkpoint checkpoint : activeList) {
                if (checkpoint.id.equals(preferred)) {
                    return checkpoint;
                }
            }
        }
        return activeList.get(0);
    }

    /** Aparece/revive al jugador en el punto de guardado, sano y en modo supervivencia. Si no hay ninguno,
     *  se usa el spawn del mundo (ultimo recurso, no deberia pasar si ya se coloco alguna cama). */
    public static void teleportToCheckpoint(ServerPlayer player, Checkpoint checkpoint) {
        ServerLevel level;
        BlockPos pos;
        if (checkpoint != null) {
            level = player.getServer().getLevel(checkpoint.dimension);
            pos = checkpoint.pos.above();
            if (level == null) {
                level = player.serverLevel();
                pos = level.getSharedSpawnPos();
            }
        } else {
            level = player.getServer().overworld();
            pos = level.getSharedSpawnPos();
        }
        if (player.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        }
        player.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.clearFire();
        player.removeAllEffects();
    }
}
