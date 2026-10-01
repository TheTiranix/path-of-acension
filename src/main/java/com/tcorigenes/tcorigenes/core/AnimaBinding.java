// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import com.tudominio.testamentodelacarne.AnimaSwordItem;
import com.tudominio.testamentodelacarne.DualScythe;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido de alejandr0: las armas ánima estan atadas al alma de su dueño. Solo la clase Guerrero Ánima las puede tener (la copia marcada del
 * Ender Warrior es la unica excepcion): ninguna otra clase las levanta ni las usa (si le queda una, desaparece), y el Guerrero Ánima no
 * puede tirarlas, guardarlas en cofres u otros contenedores ni perderlas al morir (ver SoulboundItems).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class AnimaBinding {
    private AnimaBinding() {
    }

    private static boolean isAnima(ItemStack stack) {
        return stack.getItem() instanceof AnimaSwordItem;
    }

    /** Quien puede tener un arma ánima en su poder. */
    private static boolean mayOwn(Player player, ItemStack stack) {
        if (SoulboundItems.hasMarker(stack) || DualScythe.isCopy(stack)) {
            return true;
        }
        return player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass() == PlayerClass.GUERRERO_ANIMA).orElse(false);
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!isAnima(stack) || DualScythe.isCopy(stack)) {
            return;
        }
        event.setCanceled(true);
        Player player = event.getPlayer();
        if (!player.level().isClientSide()) {
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false); // inventario lleno: no queda otra (el stash de la muerte lo recupera igual)
            }
            player.displayClientMessage(Component.literal("El arma ánima está atada a tu alma: no se puede tirar."), true);
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        ItemStack stack = event.getItem().getItem();
        if (isAnima(stack) && !mayOwn(event.getEntity(), stack)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.isCreative()
                || player.isSpectator()) {
            return;
        }
        Inventory inventory = player.getInventory();
        if (player.tickCount % 10 == 0) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (isAnima(stack) && !mayOwn(player, stack)) {
                    inventory.setItem(i, ItemStack.EMPTY); // de otra clase: no puede tenerla
                }
            }
        }
        // Que no se guarde en cofres ni en ningun contenedor abierto: vuelve al inventario.
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return;
        }
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory || !slot.hasItem()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (isAnima(stack) && !DualScythe.isCopy(stack) && inventory.add(stack.copy())) {
                slot.set(ItemStack.EMPTY);
            }
        }
        ItemStack carried = menu.getCarried();
        if (isAnima(carried) && !mayOwn(player, carried)) {
            menu.setCarried(ItemStack.EMPTY);
        }
    }
}
