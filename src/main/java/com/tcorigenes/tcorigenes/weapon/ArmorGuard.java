// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Las armaduras de WeaponBalance#GUARDED_ARMOR (Solar Crystal y Lunar Stone) son irrompibles (ver ArmorDurability) y
 * ningun mob puede sacarselas al jugador: si una pieza equipada desaparece de su ranura sin que el jugador la haya
 * movido (no esta en su inventario, ni en el cursor, ni la tiro el mismo), se vuelve a equipar y se borra el objeto
 * que el mob haya dejado tirado, asi tampoco se duplica.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ArmorGuard {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, ItemStack[]> LAST = new HashMap<>();

    private ArmorGuard() {
    }

    private static boolean guarded(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && WeaponBalance.GUARDED_ARMOR.contains(id);
    }

    private static boolean heldSomewhere(ServerPlayer player, ItemStack stack) {
        for (ItemStack other : player.getInventory().items) {
            if (ItemStack.isSameItem(other, stack)) {
                return true;
            }
        }
        for (ItemStack other : player.getInventory().offhand) {
            if (ItemStack.isSameItem(other, stack)) {
                return true;
            }
        }
        for (ItemStack other : player.getInventory().armor) {
            if (ItemStack.isSameItem(other, stack)) {
                return true;
            }
        }
        return ItemStack.isSameItem(player.containerMenu.getCarried(), stack);
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
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack now = player.getItemBySlot(SLOTS[i]);
            ItemStack before = last[i];
            if (before != null && !ItemStack.isSameItem(before, now) && !heldSomewhere(player, before)) {
                // la pieza desaparecio de la ranura sin estar en el inventario: la sacaron
                List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class,
                        player.getBoundingBox().inflate(8.0), drop -> ItemStack.isSameItem(drop.getItem(), before));
                boolean thrownByPlayer = false;
                for (ItemEntity drop : drops) {
                    if (drop.getOwner() == player) {
                        thrownByPlayer = true;
                    }
                }
                if (!thrownByPlayer) {
                    drops.forEach(ItemEntity::discard);
                    if (!now.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(now.copy());
                    }
                    player.setItemSlot(SLOTS[i], before.copy());
                    now = player.getItemBySlot(SLOTS[i]);
                }
            }
            last[i] = guarded(now) ? now.copy() : null;
        }
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
