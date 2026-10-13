// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/** Pedido de alejandr0: el Wroughtnaut (Mowzie's) suelta 12 de chatarra de Wrought, con la que se craftea el set de Wrought. */
@EventBusSubscriber(modid = "tcorigenes")
public final class WroughtnautDrops {
    private static final ResourceLocation WROUGHTNAUT = ResourceLocation.fromNamespaceAndPath("mowziesmobs", "ferrous_wroughtnaut");
    private static final ResourceLocation SCRAP = ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "wrought_scrap");

    private WroughtnautDrops() {
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide() || !WROUGHTNAUT.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))) {
            return;
        }
        var item = ForgeRegistries.ITEMS.getValue(SCRAP);
        if (item != null) {
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(), new ItemStack(item, 12)));
        }
    }
}
