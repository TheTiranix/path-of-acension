// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.coinflip;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * "La mejor version del loot que te puede tocar": se tira la tabla del cofre varias veces con mucha suerte
 * y se elige la tirada de mayor valor. Ademas, al azar, se le suma una bendicion (doble raciones, encantados
 * o una ofrenda extra), asi que ganar nunca da dos veces lo mismo.
 */
public final class LootUpgrade {
    private static final int LUCK = 8;

    private LootUpgrade() {
    }

    /** @param table tabla original del cofre (puede ser null); @param fallback contenido ya generado, se usa si no hay tabla. */
    public static List<ItemStack> best(ServerLevel level, BlockPos pos, ServerPlayer player, ResourceLocation table,
                                       List<ItemStack> fallback) {
        RandomSource random = level.getRandom();
        List<ItemStack> best = null;
        if (table != null) {
            LootTable lootTable = level.getServer().getLootData().getLootTable(table);
            if (lootTable != LootTable.EMPTY) {
                LootParams params = new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withLuck(LUCK + player.getLuck())
                        .create(LootContextParamSets.CHEST);
                int candidates = 4 + random.nextInt(5);
                double bestScore = -1;
                for (int i = 0; i < candidates; i++) {
                    List<ItemStack> roll = new ArrayList<>(lootTable.getRandomItems(params));
                    double score = score(roll);
                    if (score > bestScore) {
                        bestScore = score;
                        best = roll;
                    }
                }
            }
        }
        boolean fromTable = best != null && !best.isEmpty();
        if (!fromTable) {
            best = new ArrayList<>();
            for (ItemStack stack : fallback) {
                best.add(stack.copy());
            }
        }
        bless(best, random, !fromTable);
        return best;
    }

    private static double score(List<ItemStack> items) {
        double total = 0;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            Rarity rarity = stack.getRarity();
            double base = switch (rarity) {
                case COMMON -> 1;
                case UNCOMMON -> 4;
                case RARE -> 12;
                case EPIC -> 30;
            };
            total += base * Math.sqrt(stack.getCount());
            if (stack.isEnchanted()) {
                total += 4 + EnchantmentHelper.getEnchantments(stack).size() * 3;
            }
            if (stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || stack.is(Items.NETHERITE_SCRAP)
                    || stack.is(Items.NETHERITE_INGOT) || stack.is(Items.ENDER_PEARL) || stack.is(Items.GOLDEN_APPLE)
                    || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.TOTEM_OF_UNDYING)) {
                total += 6 * stack.getCount();
            }
        }
        return total;
    }

    /** Bendicion al azar; si no hubo tabla (loot base pobre) siempre se agrega al menos una ofrenda. */
    private static void bless(List<ItemStack> items, RandomSource random, boolean forceOffering) {
        int roll = random.nextInt(4);
        switch (roll) {
            case 1 -> { // doble racion de todo lo apilable
                for (ItemStack stack : items) {
                    if (stack.getMaxStackSize() > 1) {
                        stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() * 2));
                    }
                }
            }
            case 2 -> { // hasta 2 objetos encantados
                int enchanted = 0;
                for (int i = 0; i < items.size() && enchanted < 2; i++) {
                    ItemStack stack = items.get(i);
                    if (!stack.isEmpty() && stack.isEnchantable() && !stack.isEnchanted()) {
                        items.set(i, EnchantmentHelper.enchantItem(random, stack, 20 + random.nextInt(11), true));
                        enchanted++;
                    }
                }
            }
            case 3 -> items.add(offering(random));
            default -> {
            }
        }
        if (forceOffering && roll != 3) {
            items.add(offering(random));
        }
    }

    private static ItemStack offering(RandomSource random) {
        return switch (random.nextInt(9)) {
            case 0 -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case 1 -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
            case 2 -> new ItemStack(Items.DIAMOND, 2 + random.nextInt(3));
            case 3 -> new ItemStack(Items.NETHERITE_SCRAP, 1 + random.nextInt(3));
            case 4 -> new ItemStack(Items.EMERALD, 4 + random.nextInt(7));
            case 5 -> new ItemStack(Items.ENDER_PEARL, 2 + random.nextInt(4));
            case 6 -> new ItemStack(Items.EXPERIENCE_BOTTLE, 6 + random.nextInt(7));
            case 7 -> new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(2));
            default -> new ItemStack(Items.GOLD_INGOT, 6 + random.nextInt(7));
        };
    }
}
