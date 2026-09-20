// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.item;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.favor.Deity;
import com.tcorigenes.tcorigenes.favor.FavorManager;
import com.tcorigenes.tcorigenes.playerclass.ClassAttributeManager;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * "Matrimonio de Carne", version simplificada y consensuada: dos jugadores tienen que usar
 * este item a menos de 3 bloques de distancia, dentro de una ventana de 5 segundos entre
 * ambos, para consagrar el ritual. Una vez consagrado, el Ritualista Arcano pierde sus
 * debuffs (ver ClassAttributeManager) y gana los bonos post-ritual.
 */
public class VinculoDeCarneItem extends Item {
    private static final int WINDOW_TICKS = 20 * 5;
    private static final double MAX_DISTANCE = 3.0;
    private static final int MEIDRIS_FAVOR_PER_RITUAL = 25;
    private static final Map<UUID, Long> PENDING = new HashMap<>();

    public VinculoDeCarneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }

        boolean isMalnacido = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(raceInfo -> raceInfo.getRace() == Race.MALNACIDO)
                .orElse(false);
        if (isMalnacido) {
            player.displayClientMessage(Component.literal("Tu maldición te hizo sexualmente incapaz. No podés consagrar este ritual."), true);
            return InteractionResultHolder.fail(stack);
        }

        long now = level.getGameTime();
        UUID partnerId = findPartner(player, now);

        if (partnerId == null) {
            PENDING.put(player.getUUID(), now);
            player.displayClientMessage(Component.literal("Esperando a tu compañero espiritual..."), true);
            return InteractionResultHolder.success(stack);
        }

        PENDING.remove(player.getUUID());
        PENDING.remove(partnerId);

        Player partner = level.getServer().getPlayerList().getPlayer(partnerId);
        consecrate(player);
        if (partner != null) {
            consecrate(partner);
            partner.displayClientMessage(Component.literal("El Matrimonio de Carne ha sido consagrado."), false);
        }
        player.displayClientMessage(Component.literal("El Matrimonio de Carne ha sido consagrado."), false);
        return InteractionResultHolder.success(stack);
    }

    private UUID findPartner(Player player, long now) {
        for (Map.Entry<UUID, Long> entry : PENDING.entrySet()) {
            if (entry.getKey().equals(player.getUUID())) {
                continue;
            }
            if (now - entry.getValue() > WINDOW_TICKS) {
                continue;
            }
            Player other = player.level().getServer().getPlayerList().getPlayer(entry.getKey());
            if (other != null && other.distanceTo(player) <= MAX_DISTANCE) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void consecrate(Player player) {
        player.getPersistentData().putBoolean("matrimonio_consagrado", true);
        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).ifPresent(data -> {
            if (data.getPlayerClass() == PlayerClass.RITUALISTA_ARCANO) {
                ClassAttributeManager.updateAttributes(player, PlayerClass.RITUALISTA_ARCANO);
            }
        });
        if (player instanceof ServerPlayer serverPlayer) {
            FavorManager.addFavor(serverPlayer, Deity.MEIDRIS, MEIDRIS_FAVOR_PER_RITUAL);
        }
    }
}
