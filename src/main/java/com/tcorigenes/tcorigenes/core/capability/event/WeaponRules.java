// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.core.WeaponWeights;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import com.tudominio.elementaldamage.ModAttributes;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Restricciones de armas por clase (documento de diseno v2):
 * - Ritualista sin Matrimonio de Carne: maximo 50 de destreza requerida entre ambas manos (no
 *   puede usar armas de dos manos ni escudos grandes); no puede superarlo.
 * - Ritualista con el matrimonio consagrado y Arquero: pueden superar 50, pero con -10% de daño
 *   y -15% de velocidad de ataque/carga.
 * - Guerrero Anima: solo puede usar su espada anima (ni arcos, ni hachas, ni ballestas, ni otras armas).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class WeaponRules {
    private static final UUID DAMAGE_ID = UUID.fromString("77aa0000-0001-4001-8001-000000000001");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("77aa0000-0002-4002-8002-000000000002");
    private static final UUID DRAW_SPEED_ID = UUID.fromString("77aa0000-0003-4003-8003-000000000003");

    private WeaponRules() {
    }

    private static PlayerClass classOf(Player player) {
        return player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass()).orElse(PlayerClass.NINGUNA);
    }

    private static boolean isMarried(Player player) {
        return player.getPersistentData().getBoolean("matrimonio_consagrado");
    }

    /** Ritualista sin matrimonio y con mas de 50 de requerimiento: no puede usar lo que lleva. */
    private static boolean blockedByWeight(Player player) {
        return classOf(player) == PlayerClass.RITUALISTA_ARCANO && !isMarried(player)
                && WeaponWeights.totalWeight(player) > WeaponWeights.THRESHOLD;
    }

    /** Guerrero Anima con algo que no es su espada anima en la mano principal. */
    private static boolean blockedByAnimaRule(Player player) {
        return classOf(player) == PlayerClass.GUERRERO_ANIMA
                && WeaponWeights.weightOf(player.getMainHandItem()) > 0
                && !WeaponWeights.isAnimaSword(player.getMainHandItem());
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (blockedByWeight(player)) {
            player.displayClientMessage(Component.literal("Tu equipo pide más de " + WeaponWeights.THRESHOLD
                    + " de destreza (" + WeaponWeights.totalWeight(player) + "). Consagrá el Matrimonio de Carne."), true);
            event.setCanceled(true);
        } else if (blockedByAnimaRule(player)) {
            player.displayClientMessage(Component.literal("El Guerrero Ánima solo puede usar su espada ánima."), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        var item = event.getItem().getItem();
        boolean ranged = item instanceof BowItem || item instanceof CrossbowItem;
        if (classOf(player) == PlayerClass.GUERRERO_ANIMA && ranged) {
            player.displayClientMessage(Component.literal("El Guerrero Ánima no puede usar arcos ni ballestas."), true);
            event.setCanceled(true);
        } else if ((ranged || item instanceof ShieldItem) && blockedByWeight(player)) {
            player.displayClientMessage(Component.literal("Tu equipo pide más de " + WeaponWeights.THRESHOLD + " de destreza."), true);
            event.setCanceled(true);
        }
    }

    /** Penalizacion (-10% daño, -15% velocidad de ataque y de carga) al superar 50 para quien puede hacerlo. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        PlayerClass cls = classOf(player);
        boolean canExceed = cls == PlayerClass.ARQUERO || (cls == PlayerClass.RITUALISTA_ARCANO && isMarried(player));
        boolean penalized = canExceed && WeaponWeights.totalWeight(player) > WeaponWeights.THRESHOLD;

        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, "Peso excesivo (daño)", -0.10, AttributeModifier.Operation.MULTIPLY_TOTAL, penalized);
        setModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID, "Peso excesivo (velocidad)", -0.15, AttributeModifier.Operation.MULTIPLY_TOTAL, penalized);
        setModifier(player.getAttribute(ModAttributes.DRAW_SPEED.get()), DRAW_SPEED_ID, "Peso excesivo (carga)", -0.15, AttributeModifier.Operation.ADDITION, penalized);
    }

    private static void setModifier(AttributeInstance attribute, UUID id, String name, double amount, AttributeModifier.Operation op, boolean active) {
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(id);
        if (active && existing == null) {
            attribute.addTransientModifier(new AttributeModifier(id, name, amount, op));
        } else if (!active && existing != null) {
            attribute.removeModifier(id);
        }
    }
}
