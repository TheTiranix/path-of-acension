// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: el cuerpo de un dragon de Ice and Fire solo se recolecta con la cuchilla de carnicero de hierro (la de Butcher's
 * Delight), y cada click derecho la gasta bastante (5 usos).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class DragonCorpseHarvest {
    private static final ResourceLocation CLEAVER = ResourceLocation.fromNamespaceAndPath("butchersdelight", "cleaver");
    private static final int USES_PER_CLICK = 5;

    private DragonCorpseHarvest() {
    }

    /** Las partes del dragon (cabeza, cuello, cuerpo, cola) son entidades aparte: el click en una de ellas le llega al dragon por getParent(). */
    private static net.minecraft.world.entity.Entity ownerOf(net.minecraft.world.entity.Entity target) {
        if (target instanceof net.minecraftforge.entity.PartEntity<?> part) {
            return part.getParent();
        }
        try {
            Object parent = target.getClass().getMethod("getParent").invoke(target);
            if (parent instanceof net.minecraft.world.entity.Entity entity) {
                return entity;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // no es una parte
        }
        return target;
    }

    private static boolean isDragonCorpse(net.minecraft.world.entity.Entity target) {
        return ownerOf(target) instanceof LivingEntity living && WeakPointAnchor.isDragon(living) && WeakPointAnchor.isCorpse(living);
    }

    private static boolean deny(PlayerInteractEvent event) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (CLEAVER.equals(ForgeRegistries.ITEMS.getKey(held.getItem()))) {
            return false;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!player.level().isClientSide()) {
            player.displayClientMessage(Component.literal(Tr.s("El cuerpo del dragon solo se recolecta con una cuchilla de carnicero de hierro.")), true);
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isDragonCorpse(event.getTarget())) {
            deny(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isDragonCorpse(event.getTarget()) || deny(event) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!player.level().isClientSide() && !player.getAbilities().instabuild) {
            held.hurtAndBreak(USES_PER_CLICK, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
    }
}
