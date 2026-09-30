// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.crafting;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Mesas de crafteo con nivel (pedido de alejandr0): tier 2 (3x3, detalles de metal oscuro; dark metal, gravitite, zanite y
 * netherite solo se craftean aca o en una mayor) y tier 3 (5x5, detalles dorados; arcane, black steel y dragonsteel). Ver
 * CraftingGate para las reglas y CraftingMenuMixin para como se aplican.
 */
public final class ModCrafting {
    private static final String MODID = "testamentodelacarne";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    public static final RegistryObject<RecipeType<Tier3ShapedRecipe>> TIER3_TYPE = RECIPE_TYPES.register("tier3_crafting",
            () -> RecipeType.simple(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "tier3_crafting")));
    public static final RegistryObject<RecipeSerializer<Tier3ShapedRecipe>> TIER3_SERIALIZER = RECIPE_SERIALIZERS.register("tier3_shaped",
            Tier3ShapedRecipe.Serializer::new);
    public static final RegistryObject<MenuType<Tier3CraftingMenu>> TIER3_MENU = MENUS.register("tier3_crafting",
            () -> IForgeMenuType.create((id, inventory, data) -> new Tier3CraftingMenu(id, inventory)));

    public static final RegistryObject<Block> TIER2_TABLE = BLOCKS.register("tier2_crafting_table",
            () -> new Tier2Table(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> TIER3_TABLE = BLOCKS.register("tier3_crafting_table",
            () -> new Tier3Table(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE).strength(4.0F, 8.0F).lightLevel(state -> 6)));
    public static final RegistryObject<Item> TIER2_TABLE_ITEM = ITEMS.register("tier2_crafting_table",
            () -> new BlockItem(TIER2_TABLE.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TIER3_TABLE_ITEM = ITEMS.register("tier3_crafting_table",
            () -> new BlockItem(TIER3_TABLE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    private ModCrafting() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    /** Mesa tier 2: abre el mismo 3x3 de vanilla, con nivel 2. */
    public static class Tier2Table extends CraftingTableBlock {
        public Tier2Table(Properties properties) {
            super(properties);
        }

        @Override
        public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
            return new SimpleMenuProvider((id, inventory, player) ->
                    new TieredCraftingMenu(id, inventory, ContainerLevelAccess.create(level, pos), 2, this),
                    Component.translatable("container.testamentodelacarne.tier2_crafting"));
        }
    }

    /** Mesa tier 3: abre la grilla de 5x5. */
    public static class Tier3Table extends Block {
        public Tier3Table(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                    new Tier3CraftingMenu(id, inventory, ContainerLevelAccess.create(level, pos), this),
                    Component.translatable("container.testamentodelacarne.tier3_crafting")));
            return InteractionResult.CONSUME;
        }
    }
}
