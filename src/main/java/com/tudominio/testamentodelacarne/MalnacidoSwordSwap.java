// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Las Espadas Ánima T1/T2/T3 tienen un aspecto (y una categoria de Better Combat) propio segun la raza:
 * Malnacido (espadon; T3 maldita o purificada) y Autómata (martillo). Las recetas dan la version normal; aca se
 * cambia por la variante que corresponde al jugador (y vuelve a la normal si cambia de raza), conservando el NBT.
 */
@EventBusSubscriber(modid = TestamentoDeLaCarne.MODID)
public final class MalnacidoSwordSwap {
    private MalnacidoSwordSwap() {
    }

    private static List<Item> family(int tier) {
        return switch (tier) {
            case 1 -> List.of(ModItems.ESPADA_ANIMA_1.get(), ModItems.ESPADA_ANIMA_MALNACIDO_1.get(), ModItems.ESPADA_ANIMA_AUTOMATA_1.get());
            case 2 -> List.of(ModItems.ESPADA_ANIMA_2.get(), ModItems.ESPADA_ANIMA_MALNACIDO_2.get(), ModItems.ESPADA_ANIMA_AUTOMATA_2.get());
            default -> List.of(ModItems.ESPADA_ANIMA_3.get(), ModItems.ESPADA_ANIMA_MALNACIDO_3.get(),
                    ModItems.ESPADA_ANIMA_PURIFICADO_3.get(), ModItems.ESPADA_ANIMA_AUTOMATA_3.get());
        };
    }

    private static Item target(int tier, Race race, boolean purified) {
        if (race == Race.MALNACIDO) {
            return switch (tier) {
                case 1 -> ModItems.ESPADA_ANIMA_MALNACIDO_1.get();
                case 2 -> ModItems.ESPADA_ANIMA_MALNACIDO_2.get();
                default -> purified ? ModItems.ESPADA_ANIMA_PURIFICADO_3.get() : ModItems.ESPADA_ANIMA_MALNACIDO_3.get();
            };
        }
        if (race == Race.AUTOMATA) {
            return switch (tier) {
                case 1 -> ModItems.ESPADA_ANIMA_AUTOMATA_1.get();
                case 2 -> ModItems.ESPADA_ANIMA_AUTOMATA_2.get();
                default -> ModItems.ESPADA_ANIMA_AUTOMATA_3.get();
            };
        }
        return switch (tier) {
            case 1 -> ModItems.ESPADA_ANIMA_1.get();
            case 2 -> ModItems.ESPADA_ANIMA_2.get();
            default -> ModItems.ESPADA_ANIMA_3.get();
        };
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 20 != 0) {
            return;
        }
        Player player = event.player;
        Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace()).orElse(Race.HUMANO);
        boolean purified = player.getPersistentData().getBoolean("malnacido_purificado");
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (int tier = 1; tier <= 3; tier++) {
                if (!family(tier).contains(stack.getItem())) {
                    continue;
                }
                Item target = target(tier, race, purified);
                if (stack.getItem() != target) {
                    ItemStack swapped = new ItemStack(target, stack.getCount());
                    swapped.setTag(stack.getTag() == null ? null : stack.getTag().copy());
                    inv.setItem(i, swapped);
                }
                break;
            }
        }
    }
}
