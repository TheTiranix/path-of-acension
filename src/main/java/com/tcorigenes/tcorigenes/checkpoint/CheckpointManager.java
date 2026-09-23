// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * El punto se crea cuando el jugador SE DUERME DE VERDAD en la cama (no al colocarla): asi no se puede
 * generar puntos de guardado a lo loco solo llevando camas encima, hace falta que sea de noche/tormenta
 * y sin monstruos cerca, como cualquier cama vanilla. Las camas anteriores DEL MISMO JUGADOR siguen siendo
 * validas durante 1 dia entero (24000 ticks) mas: recien despues de eso dejan de estar en la lista. Romper
 * la cama de un punto de guardado lo invalida al toque. Todo el mundo puede reaparecer/ser revivido en
 * CUALQUIER punto activo de la lista (no solo el propio), eligiendolo con {@code /respawnpoint}.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class CheckpointManager {
    /** 1 dia de Minecraft. */
    private static final long GRACE_PERIOD_TICKS = 24000;
    /** Cooldown entre saves de cama y espera de una cama recien colocada: 1 dia de Minecraft. */
    private static final long SAVE_COOLDOWN_TICKS = 24000;
    private static final String PREFERRED_KEY = "tc_preferred_checkpoint";

    /** Quien esta durmiendo AHORA MISMO (segun el ultimo tick chequeado), para detectar el momento
     *  exacto en que empieza a dormir (isSleeping() pasa de false a true) sin duplicar el punto de
     *  guardado en cada tick mientras sigue dormido. */
    private static final Set<UUID> SLEEPING_NOW = new HashSet<>();

    private CheckpointManager() {
    }

    /** Mismo motivo que WorldRestoreManager#onServerStarting: SLEEPING_NOW es estatico y sobrevive
     *  a que el server se frene (el juego sigue vivo). Sin este reset, un jugador que quedara
     *  marcado "durmiendo" justo cuando el mundo anterior se cerro podria no generar su primer
     *  punto de guardado nuevo hasta despertarse y volver a dormir una segunda vez. */
    @SubscribeEvent
    public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        SLEEPING_NOW.clear();
    }

    /** Cooperativo: alcanza con UN jugador durmiendo para pasar la noche (y guardar). */
    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        event.getServer().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE)
                .set(0, event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        boolean sleepingNow = player.isSleeping();
        boolean wasSleeping = SLEEPING_NOW.contains(player.getUUID());
        if (sleepingNow && !wasSleeping) {
            SLEEPING_NOW.add(player.getUUID());
            openCheckpointScreen(player);
        } else if (!sleepingNow && wasSleeping) {
            SLEEPING_NOW.remove(player.getUUID());
        }
    }

    /** Abre la pantalla ANTES de crear nada (ver README/pedido): recien si el jugador elige
     *  "guardar aca" se crea el punto y arranca la copia del mundo (ChooseCheckpointPacket). */
    private static void openCheckpointScreen(ServerPlayer player) {
        sendCheckpointScreen(player, false);
    }

    /** Al entrar al mundo (singleplayer), si hay saves de cama se ofrece cargarlos; sin saves de
     *  cama no aparece nada y el mundo entra y se guarda normal. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null
                || !player.getServer().isSingleplayer()) {
            return;
        }
        MinecraftServer server = player.getServer();
        CheckpointSavedData data = CheckpointSavedData.get(server);
        if (!data.initialSnapshotTaken) {
            // Primera vez que se entra al mundo con el mod: save inicial automatico, al que se vuelve
            // si el grupo cae antes de guardar en ninguna cama.
            data.initialSnapshotTaken = true;
            data.setDirty();
            CheckpointSnapshotter.takeSnapshotAsync(server, CheckpointSnapshotter.INITIAL_ID);
        }
        if (!active(server).isEmpty()) {
            sendCheckpointScreen(player, true);
        } else {
            player.sendSystemMessage(Component.literal(
                    "No hay ningún save de cama: empezás de cero (el save inicial de este mundo). "
                            + "Dormí en una cama para crear tu primer punto de guardado.").withStyle(ChatFormatting.GOLD));
        }
    }

    private static void sendCheckpointScreen(ServerPlayer player, boolean loadMode) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID preferred = preferredIdFor(player);
        List<com.tcorigenes.tcorigenes.networking.packet.OpenCheckpointScreenPacket.CheckpointEntry> entries = new ArrayList<>();
        for (Checkpoint checkpoint : active(server)) {
            String ownerName = server.getPlayerList().getPlayers().stream()
                    .filter(p -> p.getUUID().equals(checkpoint.owner)).findFirst()
                    .map(p -> p.getName().getString())
                    .orElseGet(() -> checkpoint.owner.toString().substring(0, 8));
            entries.add(new com.tcorigenes.tcorigenes.networking.packet.OpenCheckpointScreenPacket.CheckpointEntry(
                    checkpoint.id, ownerName, checkpoint.dimension.location().toString(), checkpoint.pos,
                    checkpoint.id.equals(preferred)));
        }
        com.tcorigenes.tcorigenes.networking.Networking.sendToPlayer(player,
                new com.tcorigenes.tcorigenes.networking.packet.OpenCheckpointScreenPacket(entries, loadMode));
    }

    /** El id preferido guardado del jugador (aunque ya no este activo); null si nunca eligio ninguno. */
    private static UUID preferredIdFor(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.hasUUID(PREFERRED_KEY) ? persisted.getUUID(PREFERRED_KEY) : null;
    }

    private static String bedKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos) {
        return dimension.location() + "|" + pos.asLong();
    }

    /** Al COLOCAR una cama: se anota (hay que esperar 1 dia para guardar en ella) y la cama anterior
     *  del jugador deja de ser punto de guardado (se borra su save). */
    @SubscribeEvent
    public static void onBedPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getPlacedBlock().is(BlockTags.BEDS)
                || player.getServer() == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        CheckpointSavedData data = CheckpointSavedData.get(server);
        data.bedPlacedAt.put(bedKey(player.level().dimension(), event.getPos()), player.level().getGameTime());
        List<Checkpoint> removed = new ArrayList<>();
        data.checkpoints.removeIf(c -> {
            boolean mine = c.owner.equals(player.getUUID());
            if (mine) {
                removed.add(c);
            }
            return mine;
        });
        data.setDirty();
        if (!removed.isEmpty()) {
            removed.forEach(c -> CheckpointSnapshotter.deleteSnapshot(server, c.id));
            player.displayClientMessage(Component.literal(
                    "Colocaste una cama nueva: tu punto de guardado anterior se eliminó. Tenés que esperar 1 día de Minecraft para poder guardar en esta.")
                    .withStyle(ChatFormatting.GOLD), false);
        } else {
            player.displayClientMessage(Component.literal(
                    "Cama nueva: tenés que esperar 1 día de Minecraft para poder guardar en ella.")
                    .withStyle(ChatFormatting.GOLD), false);
        }
    }

    /** Crea un save de cama nuevo, o si replaceId != null SOBRESCRIBE ese. Respeta el cooldown de 1 dia
     *  entre saves y la espera de 1 dia de una cama recien colocada. */
    public static void createCheckpoint(ServerPlayer player, BlockPos bedPos, UUID replaceId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CheckpointSavedData data = CheckpointSavedData.get(server);
        long now = server.overworld().getGameTime();
        if (data.lastSaveTick != Long.MIN_VALUE && now - data.lastSaveTick < SAVE_COOLDOWN_TICKS) {
            player.displayClientMessage(Component.literal("Todavía no podés guardar: falta esperar "
                    + describeTicks(SAVE_COOLDOWN_TICKS - (now - data.lastSaveTick)) + " (1 día de Minecraft entre saves).")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        for (var entry : data.bedPlacedAt.entrySet()) {
            String prefix = player.level().dimension().location() + "|";
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            BlockPos placed = BlockPos.of(Long.parseLong(entry.getKey().substring(prefix.length())));
            if (placed.distManhattan(bedPos) <= 1 && now - entry.getValue() < SAVE_COOLDOWN_TICKS) {
                player.displayClientMessage(Component.literal("Esta cama es nueva: falta esperar "
                        + describeTicks(SAVE_COOLDOWN_TICKS - (now - entry.getValue())) + " para poder guardar en ella.")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
        }
        if (replaceId != null) {
            data.checkpoints.removeIf(c -> c.id.equals(replaceId));
            CheckpointSnapshotter.deleteSnapshot(server, replaceId);
        }
        Checkpoint created = new Checkpoint(UUID.randomUUID(), player.getUUID(), player.level().dimension(),
                bedPos.immutable(), now);
        data.checkpoints.add(created);
        data.lastSaveTick = now;
        data.setDirty();
        CheckpointSnapshotter.takeSnapshotAsync(server, created.id);
        setPreferred(player, created.id); // el que acabas de crear pasa a ser tu preferido
        player.displayClientMessage(Component.literal(
                (replaceId != null ? "Punto de guardado sobrescrito." : "Nuevo punto de guardado.")
                        + " Guardando una copia del mundo...").withStyle(ChatFormatting.GOLD), false);
    }

    private static String describeTicks(long ticks) {
        long seconds = ticks / 20;
        return (seconds / 60) + " min " + (seconds % 60) + " s";
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
