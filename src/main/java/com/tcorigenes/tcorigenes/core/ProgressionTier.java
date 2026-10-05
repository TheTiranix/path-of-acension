// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Nivel de progresion del jugador (diseño de alejandr0), que por ahora usa la magia para saber que hechizos puede dominar:
 * 1 Overworld basico; 2 Nether o Aether; 3 Twilight Forest o Alex's Caves; 4 haber matado al Ender Dragon.
 * Se guarda como el maximo alcanzado (en los datos persistentes del jugador, que sobreviven a la muerte) y nunca baja.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ProgressionTier {
    private static final String KEY = "pa_progression_tier";
    private static final ResourceLocation KILL_DRAGON = ResourceLocation.fromNamespaceAndPath("minecraft", "end/kill_dragon");

    private ProgressionTier() {
    }

    public static int get(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return Math.max(1, persisted.getInt(KEY));
    }

    private static void raise(ServerPlayer player, int tier) {
        if (tier <= get(player)) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(KEY, tier);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        player.displayClientMessage(Component.translatable("tcorigenes.progress.tier_up", tier), false);
    }

    private static void checkPlace(ServerPlayer player) {
        String dimension = player.level().dimension().location().toString();
        switch (dimension) {
            case "minecraft:the_nether", "aether:the_aether" -> raise(player, 2);
            case "twilightforest:twilight_forest" -> raise(player, 3);
            default -> {
            }
        }
        if (get(player) < 3 && player.tickCount % 40 == 0) {
            player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(key -> {
                if (key.location().getNamespace().equals("alexscaves")) {
                    raise(player, 3);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            checkPlace(player);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            checkPlace(player);
        }
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAdvancement().getId().equals(KILL_DRAGON)) {
            raise(player, 4);
        }
    }

    /** Mundos ya empezados: si el jugador ya mato al dragon, se le reconoce al entrar. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            var advancement = player.getServer().getAdvancements().getAdvancement(KILL_DRAGON);
            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                raise(player, 4);
            }
        }
    }
}
