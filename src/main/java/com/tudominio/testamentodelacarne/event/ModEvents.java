// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.event;

import com.tudominio.testamentodelacarne.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "testamentodelacarne")
public class ModEvents {

    @SubscribeEvent
    public static void onLootTablesLoad(LootTableLoadEvent event) {
        String tableId = event.getName().toString();
        LootTable table = event.getTable();

        // Piedra novedosa: 8% de soltar un Fragmento de Memoria al minar piedra.
        if (tableId.equals("minecraft:blocks/stone")) {
            LootPool pool = LootPool.lootPool()
                    .when(LootItemRandomChanceCondition.randomChance(0.08F))
                    .add(LootItem.lootTableItem((ItemLike) ModItems.FRAGMENTO_DE_MEMORIA.get()))
                    .name("fragmento_memoria_pool")
                    .build();
            table.addPool(pool);
        }

        // Boss de Mowzie's Mobs (Foliaath): drop garantizado de su semilla, botín de progresión.
        if (tableId.equals("mowzies_mobs:entities/foliaath")) {
            LootPool pool = LootPool.lootPool()
                    .add(LootItem.lootTableItem((ItemLike) ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("mowzies_mobs:foliaath_seed"))))
                    .name("foliaath_seed_pool")
                    .build();
            table.addPool(pool);
        }

        // Mobs hostiles basicos: 75% de soltar Alma Corrupta (moneda de progresion temprana).
        List<String> mobs = List.of("minecraft:entities/zombie", "minecraft:entities/skeleton", "minecraft:entities/spider", "minecraft:entities/creeper");
        if (mobs.contains(tableId)) {
            LootPool pool = LootPool.lootPool()
                    .when(LootItemRandomChanceCondition.randomChance(0.75F))
                    .add(LootItem.lootTableItem((ItemLike) ModItems.ALMA_CORRUPTA.get()))
                    .name("alma_corrupta_pool")
                    .build();
            table.addPool(pool);
        }
    }

    /**
     * Reescribe la activacion del portal al Aether: en vez de agua sobre piedra luminosa,
     * requiere consumir una Lagrima Consagrada haciendo click derecho sobre Glowstone.
     */
    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !ModList.get().isLoaded("aether")) {
            return;
        }

        InteractionHand hand = event.getHand();
        ServerPlayer player = (ServerPlayer) event.getEntity();

        if (player.getItemInHand(hand).is((Item) ModItems.LAGRIMA_CONSAGRADA.get())
                && level.getBlockState(event.getPos()).is(Blocks.GLOWSTONE)) {
            BlockPos activationPos = event.getPos().relative(event.getFace());
            if (level.isEmptyBlock(activationPos)) {
                net.minecraft.world.level.block.state.BlockState aetherPortal =
                        ((net.minecraft.world.level.block.Block) ForgeRegistries.BLOCKS.getValue(
                                ResourceLocation.fromNamespaceAndPath("aether", "aether_portal"))).defaultBlockState();
                level.setBlock(activationPos, aetherPortal, 3);

                if (!player.getAbilities().instabuild) {
                    player.getItemInHand(hand).shrink(1);
                }

                level.playSound(null, event.getPos(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                event.setCanceled(true);
            }
        }
    }
}
