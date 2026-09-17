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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
