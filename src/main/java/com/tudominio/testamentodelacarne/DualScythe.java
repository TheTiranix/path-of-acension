// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Guadaña Ánima del Siervo de la Luna T3: al tenerla en la mano principal se equipa sola una segunda
 * copia en la mano secundaria (Better Combat pega con las dos: ambas son categoria "sickle", ver
 * weapon_attributes). La copia va marcada; se retira sola al cambiar de item, no se puede tirar y no
 * queda al morir. Solo se equipa si la otra mano esta vacia (nunca pisa un item del jugador).
 */
@EventBusSubscriber(modid = TestamentoDeLaCarne.MODID)
public final class DualScythe {
    private static final String COPY_TAG = "tc_dual_copy";

    private DualScythe() {
    }

    public static boolean isCopy(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(COPY_TAG);
    }

    private static boolean isScythe(ItemStack stack) {
        return stack.is(ModItems.ESPADA_ANIMA_SIERVO_3.get());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean holdingReal = isScythe(main) && !isCopy(main);

        if (holdingReal) {
            if (off.isEmpty()) {
                ItemStack copy = new ItemStack(ModItems.ESPADA_ANIMA_SIERVO_3.get());
                copy.getOrCreateTag().putBoolean(COPY_TAG, true);
                player.setItemSlot(EquipmentSlot.OFFHAND, copy);
            }
        } else if (isCopy(off)) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        if (player.tickCount % 10 == 0) {
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                if (isCopy(player.getInventory().items.get(i))) {
                    player.getInventory().items.set(i, ItemStack.EMPTY);
                }
            }
            if (isCopy(main)) {
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> isCopy(drop.getItem()));
    }
}
