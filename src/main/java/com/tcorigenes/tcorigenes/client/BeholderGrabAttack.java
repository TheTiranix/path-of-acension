// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * El Beholder (Scary Mobs) agarra a los jugadores y los deja como PASAJEROS suyos; el juego no deja apuntar ni
 * pegarle a la montura que uno esta montando (Entity#canRiderInteract es false), asi que era inatacable mientras
 * te tenia en el aire. Aca el click de ataque (y mantener el click) se manda directo contra el vehiculo cuando
 * es un Beholder. El servidor no tiene esa restriccion: solo valida el alcance.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class BeholderGrabAttack {
    private static final ResourceLocation BEHOLDER = ResourceLocation.fromNamespaceAndPath("scary_mobs", "beholder");

    private BeholderGrabAttack() {
    }

    private static Entity grabber(LocalPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle != null && BEHOLDER.equals(ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType()))) {
            return vehicle;
        }
        return null;
    }

    private static void hit(Minecraft mc, LocalPlayer player, Entity target) {
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!event.isAttack() || player == null || mc.gameMode == null || mc.screen != null) {
            return;
        }
        Entity target = grabber(player);
        if (target != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
            hit(mc, player, target);
        }
    }

    /** Mantener el click: repite el golpe apenas el arma recarga. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || mc.screen != null || !mc.options.keyAttack.isDown()
                || player.isSpectator() || player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        Entity target = grabber(player);
        if (target != null) {
            hit(mc, player, target);
        }
    }
}
