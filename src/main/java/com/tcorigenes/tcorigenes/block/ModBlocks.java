// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.block;

import com.tcorigenes.tcorigenes.favor.Deity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "tcorigenes");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "tcorigenes");

    public static final RegistryObject<Block> ALTAR_PATER = BLOCKS.register(
            "altar_pater", () -> new AltarBlock(Deity.PATER, altarProperties()));
    public static final RegistryObject<Block> ALTAR_FILIS = BLOCKS.register(
            "altar_filis", () -> new AltarBlock(Deity.FILIS, altarProperties()));
    public static final RegistryObject<Block> ALTAR_LUNA = BLOCKS.register(
            "altar_luna", () -> new AltarBlock(Deity.LUNA, altarProperties()));
    public static final RegistryObject<Block> ALTAR_MEIDRIS = BLOCKS.register(
            "altar_meidris", () -> new AltarBlock(Deity.MEIDRIS, altarProperties()));
    public static final RegistryObject<Block> ALTAR_DEIROS = BLOCKS.register(
            "altar_deiros", () -> new AltarBlock(Deity.DEIROS, altarProperties()));
    public static final RegistryObject<Block> ALTAR_TEMPO = BLOCKS.register(
            "altar_tempo", () -> new AltarBlock(Deity.TEMPO, altarProperties()));

    public static final RegistryObject<Item> ALTAR_PATER_ITEM = ITEMS.register(
            "altar_pater", () -> new BlockItem(ALTAR_PATER.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALTAR_FILIS_ITEM = ITEMS.register(
            "altar_filis", () -> new BlockItem(ALTAR_FILIS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALTAR_LUNA_ITEM = ITEMS.register(
            "altar_luna", () -> new BlockItem(ALTAR_LUNA.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALTAR_MEIDRIS_ITEM = ITEMS.register(
            "altar_meidris", () -> new BlockItem(ALTAR_MEIDRIS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALTAR_DEIROS_ITEM = ITEMS.register(
            "altar_deiros", () -> new BlockItem(ALTAR_DEIROS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALTAR_TEMPO_ITEM = ITEMS.register(
            "altar_tempo", () -> new BlockItem(ALTAR_TEMPO.get(), new Item.Properties()));

    public static final RegistryObject<Block> PLAYER_RESPAWN_BLOCK = BLOCKS.register(
            "player_respawn_block", () -> new PlayerRespawnBlock(altarProperties()));
    public static final RegistryObject<Item> PLAYER_RESPAWN_BLOCK_ITEM = ITEMS.register(
            "player_respawn_block", () -> new BlockItem(PLAYER_RESPAWN_BLOCK.get(), new Item.Properties()));

    private static BlockBehaviour.Properties altarProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(4.0F, 12.0F).sound(SoundType.STONE);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
