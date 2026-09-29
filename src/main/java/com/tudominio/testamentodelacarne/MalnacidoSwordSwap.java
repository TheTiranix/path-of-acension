// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * La Espada Ánima T3 del Malnacido tiene su propio aspecto (y atributos de espadon a dos manos en
 * Better Combat): maldita mientras no este purificado, con esmeraldas al purificarse. La receta de T3 da la
 * espada normal; aca se cambia por la variante que corresponde al jugador (y vuelve a la normal si deja de
 * ser Malnacido), conservando el NBT del item.
 */
@EventBusSubscriber(modid = TestamentoDeLaCarne.MODID)
public final class MalnacidoSwordSwap {
    private MalnacidoSwordSwap() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 20 != 0) {
            return;
        }
        Player player = event.player;
        Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace()).orElse(Race.HUMANO);
        boolean malnacido = race == Race.MALNACIDO;
        Item t3 = malnacido
                ? (player.getPersistentData().getBoolean("malnacido_purificado")
                        ? ModItems.ESPADA_ANIMA_PURIFICADO_3.get() : ModItems.ESPADA_ANIMA_MALNACIDO_3.get())
                : ModItems.ESPADA_ANIMA_3.get();
        Item t2 = malnacido ? ModItems.ESPADA_ANIMA_MALNACIDO_2.get() : ModItems.ESPADA_ANIMA_2.get();
        Item t1 = malnacido ? ModItems.ESPADA_ANIMA_MALNACIDO_1.get() : ModItems.ESPADA_ANIMA_1.get();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            Item target = null;
            if (isT3Family(item) && item != t3) {
                target = t3;
            } else if (isT2Family(item) && item != t2) {
                target = t2;
            } else if (isT1Family(item) && item != t1) {
                target = t1;
            }
            if (target == null) {
                continue;
            }
            ItemStack swapped = new ItemStack(target, stack.getCount());
            swapped.setTag(stack.getTag() == null ? null : stack.getTag().copy());
            inv.setItem(i, swapped);
        }
    }

    private static boolean isT3Family(Item item) {
        return item == ModItems.ESPADA_ANIMA_3.get() || item == ModItems.ESPADA_ANIMA_MALNACIDO_3.get()
                || item == ModItems.ESPADA_ANIMA_PURIFICADO_3.get();
    }

    private static boolean isT2Family(Item item) {
        return item == ModItems.ESPADA_ANIMA_2.get() || item == ModItems.ESPADA_ANIMA_MALNACIDO_2.get();
    }

    private static boolean isT1Family(Item item) {
        return item == ModItems.ESPADA_ANIMA_1.get() || item == ModItems.ESPADA_ANIMA_MALNACIDO_1.get();
    }
}
