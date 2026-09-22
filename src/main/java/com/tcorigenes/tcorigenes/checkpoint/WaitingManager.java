// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * "Fuera de combate en grupo": si morís y hay OTRO jugador vivo en el servidor, quedás en espera (fantasma
 * en modo espectador) hasta que:
 * a) alguien te revive en un {@link com.tcorigenes.tcorigenes.block.PlayerRespawnBlock}, o
 * b) el ultimo jugador vivo tambien cae: ahi TODOS los que estaban esperando (y el que acaba de caer)
 *    reaparecen sanos en un punto de guardado (ver CheckpointManager).
 * Si jugás solo (o nadie mas esta vivo), morir te manda directo al punto de guardado, sin espera.
 * <p>
 * Simplificacion consciente: esto NO revierte el mundo (bloques rotos, mobs muertos, cofres saqueados
 * siguen como estan); solo reubica a los jugadores. Un "cargar un save" de verdad implicaria reiniciar el
 * servidor con una copia vieja del mundo, algo que un mod no puede hacer con seguridad mientras corre.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class WaitingManager {
    private static final String AWAITING_KEY = "tc_awaiting_revive";
    private static final String PENDING_CHECKPOINT_KEY = "tc_pending_checkpoint";

    private WaitingManager() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        boolean anyoneElseAlive = server.getPlayerList().getPlayers().stream()
                .anyMatch(sp -> sp != player && sp.getHealth() > 0);
        if (anyoneElseAlive) {
            setAwaiting(player, true);
            player.displayClientMessage(Component.literal(
                    "Estás fuera de combate. Esperá que te revivan, o a que el resto también caiga.").withStyle(ChatFormatting.RED), false);
        } else {
            setAwaiting(player, true);
            onWipe(server);
        }
    }

    /** Cae el ultimo jugador vivo: todos (los que esperaban y el que acaba de caer) van al punto de guardado. */
    private static void onWipe(MinecraftServer server) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "El grupo cayó entero. Reapareciendo en el punto de guardado...").withStyle(ChatFormatting.DARK_RED), false);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            setAwaiting(sp, false);
            Checkpoint checkpoint = CheckpointManager.pickFor(sp);
            if (sp.getHealth() > 0) {
                CheckpointManager.teleportToCheckpoint(sp, checkpoint);
            } else {
                setPendingCheckpoint(sp, checkpoint);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID pending = consumePendingCheckpoint(player);
        if (pending != null) {
            CheckpointManager.teleportToCheckpoint(player, findCheckpoint(player.getServer(), pending));
        } else if (isAwaiting(player)) {
            player.setGameMode(GameType.SPECTATOR); // sigue esperando: fantasma, todavia no se resuelve nada
        } else {
            CheckpointManager.teleportToCheckpoint(player, CheckpointManager.pickFor(player));
        }
    }

    /** Llamado por el bloque de reanimacion. */
    public static boolean revive(ServerPlayer reviver, UUID deadPlayerId, Checkpoint checkpoint) {
        ServerPlayer target = reviver.getServer().getPlayerList().getPlayer(deadPlayerId);
        if (target == null || !isAwaiting(target)) {
            return false;
        }
        setAwaiting(target, false);
        if (target.getHealth() > 0) {
            CheckpointManager.teleportToCheckpoint(target, checkpoint);
        } else {
            setPendingCheckpoint(target, checkpoint);
        }
        target.displayClientMessage(Component.literal("¡" + reviver.getName().getString() + " te revivió!")
                .withStyle(ChatFormatting.GREEN), false);
        reviver.displayClientMessage(Component.literal("Revivicaste a " + target.getName().getString() + ".")
                .withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    public static boolean isAwaiting(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(AWAITING_KEY);
    }

    private static void setAwaiting(Player player, boolean awaiting) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(AWAITING_KEY, awaiting);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static void setPendingCheckpoint(Player player, Checkpoint checkpoint) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (checkpoint != null) {
            persisted.putUUID(PENDING_CHECKPOINT_KEY, checkpoint.id);
        } else {
            persisted.remove(PENDING_CHECKPOINT_KEY);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static UUID consumePendingCheckpoint(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.hasUUID(PENDING_CHECKPOINT_KEY)) {
            return null;
        }
        UUID id = persisted.getUUID(PENDING_CHECKPOINT_KEY);
        persisted.remove(PENDING_CHECKPOINT_KEY);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return id;
    }

    private static Checkpoint findCheckpoint(MinecraftServer server, UUID id) {
        for (Checkpoint checkpoint : CheckpointSavedData.get(server).checkpoints) {
            if (checkpoint.id.equals(id)) {
                return checkpoint;
            }
        }
        return CheckpointManager.active(server).stream().findFirst().orElse(null);
    }
}
