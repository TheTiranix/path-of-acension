// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "tcorigenes");

    public static final RegistryObject<Item> ORBE_DE_ORIGENES = ITEMS.register(
            "orbe_de_origenes", () -> new OrbOrigenesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ANILLO_DE_PURIFICACION = ITEMS.register(
            "anillo_de_purificacion", () -> new AnilloPurificacionItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> BATERIA_LUNAR = ITEMS.register(
            "bateria_lunar", () -> new BateriaLunarItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> VINCULO_DE_CARNE = ITEMS.register(
            "vinculo_de_carne", () -> new VinculoDeCarneItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> ESCAFANDRA = ITEMS.register(
            "escafandra", () -> new EscafandraItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> BRAZALETE_CUERO = ITEMS.register(
            "brazalete_cuero", () -> new BrazaleteItem(new Item.Properties().stacksTo(1), 0.25));

    public static final RegistryObject<Item> BRAZALETE_HIERRO = ITEMS.register(
            "brazalete_hierro", () -> new BrazaleteItem(new Item.Properties().stacksTo(1), 0.5));

    public static final RegistryObject<Item> BRAZALETE_DARK_METAL = ITEMS.register(
            "brazalete_dark_metal", () -> new BrazaleteItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 1.0));

    /** Huevo del dummy de daño (ver DamageDummyEntity). */
    public static final RegistryObject<Item> DAMAGE_DUMMY_EGG = ITEMS.register(
            "damage_dummy_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.tcorigenes.tcorigenes.faction.ModEntityTypes.DAMAGE_DUMMY, 0xC8A464, 0x7A4A1E, new Item.Properties()));

    /** Huevos de aparicion de los dioses jefe (colores: fondo y manchas). */
    private static net.minecraftforge.registries.RegistryObject<Item> godEgg(String name,
            net.minecraftforge.registries.RegistryObject<net.minecraft.world.entity.EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>> type,
            int base, int spots) {
        return ITEMS.register("god_" + name + "_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(type, base, spots, new Item.Properties()));
    }

    public static final java.util.List<net.minecraftforge.registries.RegistryObject<Item>> GOD_EGGS = java.util.List.of(
            godEgg("pater", com.tcorigenes.tcorigenes.faction.ModEntityTypes.GOD_PATER, 0xE8E2DA, 0xD62226),
            godEgg("luna", com.tcorigenes.tcorigenes.faction.ModEntityTypes.GOD_LUNA, 0x969498, 0xFF8C1E),
            godEgg("deiros", com.tcorigenes.tcorigenes.faction.ModEntityTypes.GOD_DEIROS, 0xD6D0C4, 0x2C282A),
            godEgg("meidris", com.tcorigenes.tcorigenes.faction.ModEntityTypes.GOD_MEIDRIS, 0x969496, 0xFF9628),
            godEgg("filis", com.tcorigenes.tcorigenes.faction.ModEntityTypes.GOD_FILIS, 0x6E185C, 0x32823A),
            godEgg("tempo", com.tcorigenes.tcorigenes.faction.ModEntityTypes.GOD_TEMPO, 0x0C0C10, 0xBE96FF));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
