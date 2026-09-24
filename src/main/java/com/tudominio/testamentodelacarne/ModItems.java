// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TestamentoDeLaCarne.MODID);

    public static final RegistryObject<Item> FRAGMENTO_DE_MEMORIA = ITEMS.register(
            "fragmento_de_memoria", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.testamentodelacarne.fragmento_de_memoria").withStyle(ChatFormatting.GRAY));
                }
            });

    public static final RegistryObject<Item> CORAZON_DE_PIEDRA_INERTE = ITEMS.register(
            "corazon_de_piedra_inerte", () -> new Item(new Item.Properties().rarity(Rarity.COMMON)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.testamentodelacarne.corazon_de_piedra_inerte").withStyle(ChatFormatting.GRAY));
                }
            });

    public static final RegistryObject<Item> ENGRANAJE_ARCANO = ITEMS.register(
            "engranaje_arcano", () -> new Item(new Item.Properties().rarity(Rarity.RARE)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.testamentodelacarne.engranaje_arcano").withStyle(ChatFormatting.GRAY));
                }
            });

    public static final RegistryObject<Item> LENTE_DE_LA_VERDAD_OCULTA = ITEMS.register(
            "lente_de_la_verdad_oculta", () -> new Item(new Item.Properties().rarity(Rarity.RARE)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.testamentodelacarne.lente_de_la_verdad_oculta").withStyle(ChatFormatting.DARK_PURPLE));
                }
            });

    public static final RegistryObject<Item> LAGRIMA_CONSAGRADA = ITEMS.register(
            "lagrima_consagrada", () -> new Item(new Item.Properties().rarity(Rarity.RARE)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }

                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.testamentodelacarne.lagrima_consagrada").withStyle(ChatFormatting.AQUA));
                }
            });

    public static final RegistryObject<Item> BUSCADOR_DE_ECOS = ITEMS.register(
            "buscador_de_ecos", () -> new BuscadorDeEcosItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> CHASIS_ENGRANAJE = ITEMS.register(
            "chasis_engranaje", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> GEMA_SANGRE_IMBUIDA = ITEMS.register(
            "gema_sangre_imbuida", () -> new Item(new Item.Properties().rarity(Rarity.RARE)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> RELIQUIA_DEL_APOSTATA = ITEMS.register(
            "reliquia_del_apostata", () -> new ReliquiaDelApostataItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> NUCLEO_DE_AUTOMATA = ITEMS.register(
            "nucleo_de_automata", () -> new Item(new Item.Properties().rarity(Rarity.RARE)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.literal("El corazón de una máquina ancestral. Zumba con un poder inimaginable.")
                            .withStyle(ChatFormatting.DARK_AQUA));
                }
            });

    public static final RegistryObject<Item> ALMA_CORRUPTA = ITEMS.register(
            "alma_corrupta", () -> new Item(new Item.Properties().rarity(Rarity.COMMON)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.testamentodelacarne.alma_corrupta").withStyle(ChatFormatting.GRAY));
                }
            });

    public static final RegistryObject<Item> ESPADA_ANIMA_1 = ITEMS.register(
            "espada_anima_1", () -> new AnimaSwordItem(5.0F, -2.4F, 2.6F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_2 = ITEMS.register(
            "espada_anima_2", () -> new AnimaSwordItem(8.0F, -2.2F, 2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_3 = ITEMS.register(
            "espada_anima_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC)));

    /** Variante exclusiva de Demonio + Guerrero Ánima (ver ClassSelection): mismas estadisticas por
     *  fase que la Espada Ánima normal, solo cambia el aspecto (alma corrompida por sangre demoniaca). */
    public static final RegistryObject<Item> ESPADA_ANIMA_DEMONIO_1 = ITEMS.register(
            "espada_anima_demonio_1", () -> new AnimaSwordItem(5.0F, -2.4F, 2.6F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_DEMONIO_2 = ITEMS.register(
            "espada_anima_demonio_2", () -> new AnimaSwordItem(8.0F, -2.2F, 2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_DEMONIO_3 = ITEMS.register(
            "espada_anima_demonio_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> DARK_METAL_PICKAXE = ITEMS.register(
            "dark_metal_pickaxe", () -> new net.minecraft.world.item.PickaxeItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.DARK_METAL_TIER, 1, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> PRISMA_CONVERTIDOR = ITEMS.register(
            "prisma_convertidor", () -> new ElementalConverterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static void register(IEventBus eventBus) {
        MaterialWeapons.registerAll(ITEMS);
        ITEMS.register(eventBus);
    }
}
