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
        Item target;
        if (race == Race.MALNACIDO) {
            target = player.getPersistentData().getBoolean("malnacido_purificado")
                    ? ModItems.ESPADA_ANIMA_PURIFICADO_3.get() : ModItems.ESPADA_ANIMA_MALNACIDO_3.get();
        } else {
            target = ModItems.ESPADA_ANIMA_3.get();
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.getItem() == target || !isFamily(stack.getItem(), race)) {
                continue;
            }
            ItemStack swapped = new ItemStack(target, stack.getCount());
            swapped.setTag(stack.getTag() == null ? null : stack.getTag().copy());
            inv.setItem(i, swapped);
        }
    }

    /** Solo la T3 normal se convierte a variante de Malnacido; las variantes vuelven a la normal si ya no lo es. */
    private static boolean isFamily(Item item, Race race) {
        boolean variant = item == ModItems.ESPADA_ANIMA_MALNACIDO_3.get() || item == ModItems.ESPADA_ANIMA_PURIFICADO_3.get();
        return race == Race.MALNACIDO ? (variant || item == ModItems.ESPADA_ANIMA_3.get()) : variant;
    }
}
