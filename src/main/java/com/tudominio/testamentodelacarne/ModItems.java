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
            "espada_anima_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC), true));

    /** Variante exclusiva de Demonio + Guerrero Ánima (ver ClassSelection): mismas estadisticas por
     *  fase que la Espada Ánima normal, solo cambia el aspecto (alma corrompida por sangre demoniaca). */
    public static final RegistryObject<Item> ESPADA_ANIMA_DEMONIO_1 = ITEMS.register(
            "espada_anima_demonio_1", () -> new AnimaSwordItem(5.0F, -2.4F, 2.6F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_DEMONIO_2 = ITEMS.register(
            "espada_anima_demonio_2", () -> new AnimaSwordItem(8.0F, -2.2F, 2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_DEMONIO_3 = ITEMS.register(
            "espada_anima_demonio_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC), true));

    /** Variante exclusiva de Hereje + Guerrero Ánima (ver ClassSelection): mismas estadisticas por
     *  fase que la Espada Ánima normal, solo cambia el aspecto (hoja facetada con sangre). */
    public static final RegistryObject<Item> ESPADA_ANIMA_HEREJE_1 = ITEMS.register(
            "espada_anima_hereje_1", () -> new AnimaSwordItem(5.0F, -2.4F, 2.6F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_HEREJE_2 = ITEMS.register(
            "espada_anima_hereje_2", () -> new AnimaSwordItem(8.0F, -2.2F, 2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_HEREJE_3 = ITEMS.register(
            "espada_anima_hereje_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC), true));

    /** Variante exclusiva de Angel + Guerrero Ánima (ver ClassSelection): mismas estadisticas por
     *  fase que la Espada Ánima normal, solo cambia el aspecto (estilete plateado -> espada alada); la fase 3 pega 100000
     *  por golpe (pedido de alejandr0). */
    public static final RegistryObject<Item> ESPADA_ANIMA_ANGEL_1 = ITEMS.register(
            "espada_anima_angel_1", () -> new AnimaSwordItem(5.0F, -2.4F, 2.6F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_ANGEL_2 = ITEMS.register(
            "espada_anima_angel_2", () -> new AnimaSwordItem(8.0F, -2.2F, 2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_ANGEL_3 = ITEMS.register(
            "espada_anima_angel_3", () -> new AnimaSwordItem(99999.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC), true));

    /** Variante exclusiva de Siervo de la Luna + Guerrero Ánima (ver ClassSelection): guadaña ligada al alma.
     *  La fase 3 se empuña de a dos (ver DualScythe). */
    public static final RegistryObject<Item> ESPADA_ANIMA_SIERVO_1 = ITEMS.register(
            "espada_anima_siervo_1", () -> new AnimaSwordItem(5.0F, -2.4F, 2.6F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_SIERVO_2 = ITEMS.register(
            "espada_anima_siervo_2", () -> new AnimaSwordItem(8.0F, -2.2F, 2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_SIERVO_3 = ITEMS.register(
            "espada_anima_siervo_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC), true));

    /** T3 del Malnacido (espadon a dos manos en Better Combat): maldita mientras no se purifique y con
     *  esmeraldas al purificarse. Se intercambian solas segun el estado del jugador (ver MalnacidoSwordSwap). */
    public static final RegistryObject<Item> ESPADA_ANIMA_MALNACIDO_1 = ITEMS.register(
            "espada_anima_malnacido_1", () -> new AnimaSwordItem(5.0F, -2.4F, 2.6F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_MALNACIDO_2 = ITEMS.register(
            "espada_anima_malnacido_2", () -> new AnimaSwordItem(8.0F, -2.2F, 2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_MALNACIDO_3 = ITEMS.register(
            "espada_anima_malnacido_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC), true));

    public static final RegistryObject<Item> ESPADA_ANIMA_PURIFICADO_3 = ITEMS.register(
            "espada_anima_purificado_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC), true));

    /** Martillos Anima del Autómata (categoria martillo en Better Combat, ver weapon_attributes). Se entregan
     *  y se intercambian solos segun la raza (ver MalnacidoSwordSwap). */
    public static final RegistryObject<Item> ESPADA_ANIMA_AUTOMATA_1 = ITEMS.register(
            "espada_anima_automata_1", () -> new AnimaSwordItem(5.0F, -2.4F, 2.6F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_AUTOMATA_2 = ITEMS.register(
            "espada_anima_automata_2", () -> new AnimaSwordItem(8.0F, -2.2F, 2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ESPADA_ANIMA_AUTOMATA_3 = ITEMS.register(
            "espada_anima_automata_3", () -> new AnimaSwordItem(10.0F, -2.0F, 3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC), true));

    public static final RegistryObject<Item> SAPPHIRE_HELMET = ITEMS.register(
            "sapphire_helmet", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.SapphireArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> SAPPHIRE_CHESTPLATE = ITEMS.register(
            "sapphire_chestplate", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.SapphireArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> SAPPHIRE_LEGGINGS = ITEMS.register(
            "sapphire_leggings", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.SapphireArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> SAPPHIRE_BOOTS = ITEMS.register(
            "sapphire_boots", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.SapphireArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> DIASCITE_SWORD = ITEMS.register(
            "diascite_sword", () -> new net.minecraft.world.item.SwordItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.DIASCITE_TIER, 3, -2.4F, new Item.Properties()));

    public static final RegistryObject<Item> DIASCITE_HELMET = ITEMS.register(
            "diascite_helmet", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.DiasciteArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> DIASCITE_CHESTPLATE = ITEMS.register(
            "diascite_chestplate", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.DiasciteArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> DIASCITE_LEGGINGS = ITEMS.register(
            "diascite_leggings", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.DiasciteArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> DIASCITE_BOOTS = ITEMS.register(
            "diascite_boots", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.DiasciteArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    /** Gema de gravitite (pedido de alejandr0): sale de fundir el mineral de gravitite y es el material de todo lo de gravitite. */
    public static final RegistryObject<Item> GRAVITITE_GEM = ITEMS.register(
            "gravitite_gem", () -> new Item(new Item.Properties()));

    /** Encendedor de metal azul (pedido de alejandr0): se craftea con depth ingot, dark metal ingot y pedernal en la mesa tier 2. */
    public static final RegistryObject<Item> BLUE_FLINT_AND_STEEL = ITEMS.register(
            "blue_flint_and_steel", () -> new net.minecraft.world.item.FlintAndSteelItem(new Item.Properties().durability(256)));

    public static final RegistryObject<Item> ARCANE_HELMET = ITEMS.register(
            "arcane_helmet", () -> new ArcaneArmorItem(
                    com.tudominio.testamentodelacarne.util.TierArmorMaterial.ARCANE,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> BLACK_STEEL_HELMET = ITEMS.register(
            "black_steel_helmet", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.TierArmorMaterial.BLACK_STEEL,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ARCANE_CHESTPLATE = ITEMS.register(
            "arcane_chestplate", () -> new ArcaneArmorItem(
                    com.tudominio.testamentodelacarne.util.TierArmorMaterial.ARCANE,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> BLACK_STEEL_CHESTPLATE = ITEMS.register(
            "black_steel_chestplate", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.TierArmorMaterial.BLACK_STEEL,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ARCANE_LEGGINGS = ITEMS.register(
            "arcane_leggings", () -> new ArcaneArmorItem(
                    com.tudominio.testamentodelacarne.util.TierArmorMaterial.ARCANE,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> BLACK_STEEL_LEGGINGS = ITEMS.register(
            "black_steel_leggings", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.TierArmorMaterial.BLACK_STEEL,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ARCANE_BOOTS = ITEMS.register(
            "arcane_boots", () -> new ArcaneArmorItem(
                    com.tudominio.testamentodelacarne.util.TierArmorMaterial.ARCANE,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> BLACK_STEEL_BOOTS = ITEMS.register(
            "black_steel_boots", () -> new net.minecraft.world.item.ArmorItem(
                    com.tudominio.testamentodelacarne.util.TierArmorMaterial.BLACK_STEEL,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ARCANE_SWORD = ITEMS.register(
            "arcane_sword", () -> new net.minecraft.world.item.SwordItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.ARCANE_TIER, 3, -2.4F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ARCANE_PICKAXE = ITEMS.register(
            "arcane_pickaxe", () -> new net.minecraft.world.item.PickaxeItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.ARCANE_TIER, 1, -2.8F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ARCANE_AXE = ITEMS.register(
            "arcane_axe", () -> new net.minecraft.world.item.AxeItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.ARCANE_TIER, 5.0F, -3.0F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ARCANE_SHOVEL = ITEMS.register(
            "arcane_shovel", () -> new net.minecraft.world.item.ShovelItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.ARCANE_TIER, 1.5F, -3.0F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> ARCANE_HOE = ITEMS.register(
            "arcane_hoe", () -> new net.minecraft.world.item.HoeItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.ARCANE_TIER, -3, 0.0F, new Item.Properties().fireResistant()));

    /** Cuchillo de carnicero de piedra (pedido de alejandr0): para descuartizar animales desde el principio, sin el cleaver de hierro. */
    public static final RegistryObject<Item> STONE_BUTCHER_KNIFE = ITEMS.register(
            "stone_butcher_knife", () -> new net.minecraft.world.item.AxeItem(net.minecraft.world.item.Tiers.STONE, 3.0F, -2.4F,
                    new Item.Properties()));

    public static final RegistryObject<Item> NAGA_HELMET = ITEMS.register(
            "naga_helmet", () -> new NagaArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> NAGA_BOOTS = ITEMS.register(
            "naga_boots", () -> new NagaArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> DARKNESS_LEGGINGS = ITEMS.register(
            "darkness_leggings", () -> new net.minecraft.world.item.ArmorItem(com.tudominio.testamentodelacarne.util.MiscArmorMaterials.DARKNESS,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> DARKNESS_BOOTS = ITEMS.register(
            "darkness_boots", () -> new net.minecraft.world.item.ArmorItem(com.tudominio.testamentodelacarne.util.MiscArmorMaterials.DARKNESS,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> WROUGHT_CHESTPLATE = ITEMS.register(
            "wrought_chestplate", () -> new net.minecraft.world.item.ArmorItem(com.tudominio.testamentodelacarne.util.MiscArmorMaterials.WROUGHT,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> WROUGHT_LEGGINGS = ITEMS.register(
            "wrought_leggings", () -> new net.minecraft.world.item.ArmorItem(com.tudominio.testamentodelacarne.util.MiscArmorMaterials.WROUGHT,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> WROUGHT_BOOTS = ITEMS.register(
            "wrought_boots", () -> new net.minecraft.world.item.ArmorItem(com.tudominio.testamentodelacarne.util.MiscArmorMaterials.WROUGHT,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> SAPPHIRE_SWORD = ITEMS.register(
            "sapphire_sword", () -> new net.minecraft.world.item.SwordItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.SAPPHIRE_TIER, 3, -2.4F, new Item.Properties()));

    public static final RegistryObject<Item> SAPPHIRE_PICKAXE = ITEMS.register(
            "sapphire_pickaxe", () -> new net.minecraft.world.item.PickaxeItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.SAPPHIRE_TIER, 1, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> SAPPHIRE_AXE = ITEMS.register(
            "sapphire_axe", () -> new net.minecraft.world.item.AxeItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.SAPPHIRE_TIER, 5.0F, -3.0F, new Item.Properties()));

    public static final RegistryObject<Item> SAPPHIRE_SHOVEL = ITEMS.register(
            "sapphire_shovel", () -> new net.minecraft.world.item.ShovelItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.SAPPHIRE_TIER, 1.5F, -3.0F, new Item.Properties()));

    public static final RegistryObject<Item> SAPPHIRE_HOE = ITEMS.register(
            "sapphire_hoe", () -> new net.minecraft.world.item.HoeItem(
                    com.tudominio.testamentodelacarne.util.ModTiers.SAPPHIRE_TIER, -3, 0.0F, new Item.Properties()));

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
