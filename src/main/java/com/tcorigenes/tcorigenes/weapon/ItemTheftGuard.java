// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido de alejandr0: ningun mob puede sacarle o robarle al jugador un arma, herramienta o armadura (hay mobs de Born in
 * Chaos, como el Krampus, que desarman). Los casos conocidos se cortan en origen (mixins de los procedimientos del
 * Krampus); esta es la red general: si el item de la mano principal, la secundaria o una ranura de armadura desaparece
 * sin que el jugador lo haya movido (no esta en su inventario, ni en el cursor, ni lo tiro el con Q) y aparece tirado al
 * lado, se vuelve a poner en su lugar y se borra el que quedo en el suelo (asi tampoco se duplica).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ItemTheftGuard {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int TOSS_WINDOW_TICKS = 6;
    private static final Map<UUID, ItemStack[]> LAST = new HashMap<>();
    private static final Map<UUID, Integer> LAST_TOSS = new HashMap<>();

    private ItemTheftGuard() {
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        LAST_TOSS.put(event.getPlayer().getUUID(), event.getPlayer().tickCount);
    }

    private static boolean heldSomewhere(ServerPlayer player, ItemStack stack) {
        for (ItemStack other : player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(other, stack)) {
                return true;
            }
        }
        for (ItemStack other : player.getInventory().offhand) {
            if (ItemStack.isSameItemSameTags(other, stack)) {
                return true;
            }
        }
        for (ItemStack other : player.getInventory().armor) {
            if (ItemStack.isSameItemSameTags(other, stack)) {
                return true;
            }
        }
        if (ItemStack.isSameItemSameTags(player.containerMenu.getCarried(), stack)) {
            return true;
        }
        // en un contenedor abierto (cofre, etc.): el jugador lo dejo ahi
        for (var slot : player.containerMenu.slots) {
            if (ItemStack.isSameItemSameTags(slot.getItem(), stack)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            LAST.remove(player.getUUID());
            return;
        }
        ItemStack[] last = LAST.computeIfAbsent(player.getUUID(), id -> new ItemStack[SLOTS.length]);
        Integer toss = LAST_TOSS.get(player.getUUID());
        boolean recentlyTossed = toss != null && player.tickCount - toss <= TOSS_WINDOW_TICKS;
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack now = player.getItemBySlot(SLOTS[i]);
            ItemStack before = last[i];
            if (before != null && !before.isEmpty() && !ItemStack.isSameItemSameTags(before, now) && !recentlyTossed
                    && !heldSomewhere(player, before)) {
                List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6.0),
                        drop -> ItemStack.isSameItemSameTags(drop.getItem(), before) && drop.getAge() <= 3);
                if (!drops.isEmpty()) {
                    drops.forEach(ItemEntity::discard);
                    if (!now.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(now.copy());
                    }
                    player.setItemSlot(SLOTS[i], before.copy());
                    now = player.getItemBySlot(SLOTS[i]);
                }
            }
            last[i] = now.isEmpty() ? null : now.copy();
        }
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
        LAST_TOSS.remove(event.getEntity().getUUID());
    }
}
