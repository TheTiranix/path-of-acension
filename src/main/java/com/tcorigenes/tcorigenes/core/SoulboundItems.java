// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.util.Iterator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Items que un jugador "no puede perder por nada" (ahora mismo, la Espada Ánima del Ender Warrior):
 * ni tirandolos a mano (ver AnimaSwordItem#onDroppedByPlayer, que consulta isSoulbound) ni muriendo.
 * Al morir se sacan de la lista de drops ANTES de que lleguen a spawnear en el mundo y se guardan en
 * persistentData (que, a diferencia de las Capabilities, sobrevive a que Forge reconstruya el Player
 * entero al morir, ver ModEvents#onPlayerDeathSnapshot); al respawnear se devuelven al inventario.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class SoulboundItems {
    private static final String MARKER_TAG = "tc_soulbound";
    private static final String STASH_KEY = "tc_soulbound_stash";

    private SoulboundItems() {
    }

    public static void markSoulbound(ItemStack stack) {
        stack.getOrCreateTag().putBoolean(MARKER_TAG, true);
    }

    public static boolean isSoulbound(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(MARKER_TAG);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ListTag stash = null;
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next().getItem();
            if (isSoulbound(stack)) {
                if (stash == null) {
                    stash = new ListTag();
                }
                stash.add(stack.save(new CompoundTag()));
                it.remove();
            }
        }
        if (stash != null) {
            CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            persisted.put(STASH_KEY, stash);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(STASH_KEY)) {
            return;
        }
        ListTag stash = persisted.getList(STASH_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < stash.size(); i++) {
            ItemStack stack = ItemStack.of(stash.getCompound(i));
            if (!stack.isEmpty()) {
                player.getInventory().add(stack);
            }
        }
        persisted.remove(STASH_KEY);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
