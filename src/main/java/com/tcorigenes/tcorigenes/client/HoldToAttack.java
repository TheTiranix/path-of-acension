// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tudominio.testamentodelacarne.AnimaSwordItem;
import com.tudominio.testamentodelacarne.MaterialWeapons;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Mantener el click ataca de forma continua con las armas propias del mod (Espadas Anima y armas de material).
 * Better Combat ya lo hace para las armas que tienen atributos suyos (isHoldToAttackEnabled); las que no los
 * tienen (las espadas anima) solo atacaban con un click por golpe, asi que para esas se repite el ataque
 * apenas el arma recarga por completo.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class HoldToAttack {
    private static Method betterCombatAttributes;
    private static boolean betterCombatLooked;

    private HoldToAttack() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null || mc.screen != null
                || !mc.options.keyAttack.isDown() || player.isSpectator() || player.isUsingItem()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AnimaSwordItem) && !(stack.getItem() instanceof MaterialWeapons.WeaponItem)) {
            return;
        }
        if (betterCombatHandles(stack) || player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        if (mc.hitResult instanceof EntityHitResult hit) {
            mc.gameMode.attack(player, hit.getEntity());
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** true si Better Combat tiene atributos para el item (entonces es el quien maneja el mantener click). */
    private static boolean betterCombatHandles(ItemStack stack) {
        if (!betterCombatLooked) {
            betterCombatLooked = true;
            try {
                betterCombatAttributes = Class.forName("net.bettercombat.logic.WeaponRegistry")
                        .getMethod("getAttributes", ItemStack.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                betterCombatAttributes = null;
            }
        }
        if (betterCombatAttributes == null) {
            return false;
        }
        try {
            return betterCombatAttributes.invoke(null, stack) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
