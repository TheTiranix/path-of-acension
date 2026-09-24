// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.coinflip;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.CloseCoinflipPacket;
import com.tcorigenes.tcorigenes.networking.packet.CoinflipResultPacket;
import com.tcorigenes.tcorigenes.networking.packet.OpenCoinflipPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Cara o cruz: la primera vez que un jugador abre un cofre de botin (estructuras, incluidos los de Lootr)
 * hay un 35% de que aparezca el lanzamiento de moneda. El jugador elige cara o cruz: si acierta el cofre trae
 * la mejor version de su loot (ver LootUpgrade); si falla, se abre vacio. Los cofres puestos por jugadores
 * nunca entran (solo los que tienen tabla de botin). El resultado lo decide siempre el servidor.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class CoinflipManager {
    public static final double CHANCE = 0.35;
    private static final String DONE_KEY = "tc_coinflip_done";
    private static final int FLIP_TICKS = 62; // lo que dura la animacion en el cliente
    private static final int MAX_WAIT_TICKS = 20 * 45;

    private static final class Pending {
        ResourceKey<Level> dimension;
        BlockPos pos;
        BlockHitResult hit;
        ResourceLocation table;
        long createdAt;
        Boolean win; // null = esperando que elija
        long openAt;
        boolean opening;
        long openedAt;
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private CoinflipManager() {
    }

    // ------------------------------------------------------------ deteccion
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) {
            return;
        }
        if (PENDING.containsKey(player.getUUID())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        LootInfo info = lootInfo(level, pos);
        if (info == null) {
            return;
        }
        String key = level.dimension().location() + "|" + pos.asLong();
        if (isDone(player, key)) {
            return;
        }
        markDone(player, key);
        if (player.getRandom().nextDouble() >= CHANCE) {
            return;
        }
        Pending pending = new Pending();
        pending.dimension = level.dimension();
        pending.pos = pos.immutable();
        pending.hit = event.getHitVec();
        pending.table = info.table();
        pending.createdAt = level.getGameTime();
        PENDING.put(player.getUUID(), pending);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Networking.sendToPlayer(player, new OpenCoinflipPacket());
    }

    private record LootInfo(ResourceLocation table) {
    }

    /** Devuelve datos si el bloque es un contenedor de botin (Lootr o con tabla de botin pendiente); null si no. */
    private static LootInfo lootInfo(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId != null && blockId.getNamespace().equals("lootr")) {
            return new LootInfo(lootrTable(be));
        }
        if (be instanceof RandomizableContainerBlockEntity) {
            CompoundTag tag = be.saveWithoutMetadata();
            if (tag.contains("LootTable", Tag.TAG_STRING)) {
                return new LootInfo(ResourceLocation.tryParse(tag.getString("LootTable")));
            }
        }
        return null;
    }

    /** Lootr guarda la tabla original en la entidad de bloque; se lee por reflexion para no depender de su jar. */
    private static ResourceLocation lootrTable(BlockEntity be) {
        try {
            Object table = be.getClass().getMethod("getTable").invoke(be);
            return table instanceof ResourceLocation location ? location : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------ persistencia
    private static boolean isDone(Player player, String key) {
        ListTag list = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(DONE_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static void markDone(Player player, String key) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag list = persisted.getList(DONE_KEY, Tag.TAG_STRING);
        list.add(StringTag.valueOf(key));
        persisted.put(DONE_KEY, list);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    // ------------------------------------------------------------ eleccion
    public static void onChoice(ServerPlayer player, boolean choseHeads) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || pending.win != null) {
            return;
        }
        boolean flipHeads = player.getRandom().nextBoolean();
        pending.win = flipHeads == choseHeads;
        pending.openAt = player.level().getGameTime() + FLIP_TICKS;
        Networking.sendToPlayer(player, new CoinflipResultPacket(flipHeads, pending.win));
    }

    // ------------------------------------------------------------ apertura
    /** Mientras hay una moneda en el aire el mundo queda "en pausa" para todos: nadie (jugadores ni mobs) hace tick,
     *  y se frenan el ciclo de dia y el clima. (El tiempo del server sigue corriendo porque la moneda lo usa.) */
    private static boolean worldPaused = false;
    private static boolean savedDaylight;
    private static boolean savedWeather;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!PENDING.isEmpty() && !event.getEntity().level().isClientSide()) {
            event.setCanceled(true);
        }
    }

    private static void updatePause(net.minecraft.server.MinecraftServer server) {
        boolean shouldPause = !PENDING.isEmpty();
        if (shouldPause && !worldPaused) {
            worldPaused = true;
            savedDaylight = server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
            savedWeather = server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE);
            server.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
            server.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, server);
        } else if (!shouldPause && worldPaused) {
            worldPaused = false;
            server.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(savedDaylight, server);
            server.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(savedWeather, server);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            updatePause(event.getServer());
        }
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> entry = it.next();
            Pending pending = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            ServerLevel level = event.getServer().getLevel(pending.dimension);
            if (player == null || level == null) {
                it.remove();
                continue;
            }
            // Toda salida sin abrir el cofre cierra la pantalla del cliente (antes podia quedar trabada).
            long now = level.getGameTime();
            if (pending.win == null) {
                if (now - pending.createdAt > MAX_WAIT_TICKS) {
                    it.remove();
                    Networking.sendToPlayer(player, new CloseCoinflipPacket());
                }
            } else if (!pending.opening) {
                if (now >= pending.openAt) {
                    pending.opening = true;
                    pending.openedAt = now;
                    BlockState state = level.getBlockState(pending.pos);
                    InteractionResult result = state.use(level, player, InteractionHand.MAIN_HAND, pending.hit);
                    if (!result.consumesAction()) {
                        it.remove();
                        Networking.sendToPlayer(player, new CloseCoinflipPacket());
                    }
                }
            } else if (now - pending.openedAt > 40) {
                it.remove(); // el cofre no llego a abrirse (tapado, etc.)
                Networking.sendToPlayer(player, new CloseCoinflipPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.opening) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) {
                slots.add(slot);
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        PENDING.remove(player.getUUID());
        ServerLevel level = (ServerLevel) player.level();

        if (Boolean.TRUE.equals(pending.win)) {
            List<ItemStack> base = new ArrayList<>();
            for (Slot slot : slots) {
                if (!slot.getItem().isEmpty()) {
                    base.add(slot.getItem().copy());
                }
            }
            List<ItemStack> loot = LootUpgrade.best(level, pending.pos, player, pending.table, base);
            for (Slot slot : slots) {
                slot.set(ItemStack.EMPTY);
            }
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) {
                order.add(i);
            }
            Collections.shuffle(order, new java.util.Random(level.getRandom().nextLong()));
            int next = 0;
            for (ItemStack stack : loot) {
                if (stack.isEmpty() || next >= order.size()) {
                    continue;
                }
                slots.get(order.get(next++)).set(stack);
            }
            player.displayClientMessage(Component.literal("La moneda cayó de tu lado: el cofre te sonríe.")
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            for (Slot slot : slots) {
                slot.set(ItemStack.EMPTY);
            }
            player.displayClientMessage(Component.literal("La moneda cayó en tu contra: el cofre está vacío.")
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
        for (Slot slot : slots) {
            slot.container.setChanged();
        }
        menu.broadcastChanges();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }
}
