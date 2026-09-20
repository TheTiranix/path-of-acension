// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Todos los mobs (no solo los hostiles vanilla: tambien los de mods, animales, etc.) nacen mas fuertes cuanto mas lejos del spawn del mundo y segun la
 * dimension (Nether/End ya vienen con un bonus base). Se aplica UNA sola vez por mob (flag en
 * persistentData) como MULTIPLY_TOTAL sobre vida y daño de ataque.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class MobScaling {
    private static final String SCALED_KEY = "tc_mob_scaled";
    public static final UUID HEALTH_ID = UUID.fromString("6d1c0f52-3b0a-4a55-9c1e-2f7a8b1d0a01");
    private static final UUID DAMAGE_ID = UUID.fromString("6d1c0f52-3b0a-4a55-9c1e-2f7a8b1d0a02");
    private static final double BLOCKS_PER_LEVEL = 500.0;
    private static final int MAX_LEVEL = 20;
    public static final double BONUS_PER_LEVEL = 0.04;
    /** Cada zona (500 bloques) abre una franja de 5 niveles; cada mob sortea el suyo dentro de ella. */
    private static final int LEVEL_SPREAD = 4;
    private static final int NETHER_LEVELS = 8;
    private static final int END_LEVELS = 15;
    private static final UUID MP_HEALTH_ID = UUID.fromString("6d1c0f52-3b0a-4a55-9c1e-2f7a8b1d0a03");
    private static final UUID MP_DAMAGE_ID = UUID.fromString("6d1c0f52-3b0a-4a55-9c1e-2f7a8b1d0a04");
    private static final double EXTRA_PLAYER_BONUS = 0.35;

    /** Maximo de jugadores conectados a la vez que alcanzo este mundo (nunca baja). */
    public static final class PlayerPeak extends SavedData {
        private int peak = 1;

        static PlayerPeak load(CompoundTag tag) {
            PlayerPeak data = new PlayerPeak();
            data.peak = Math.max(1, tag.getInt("peak"));
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("peak", peak);
            return tag;
        }

        static PlayerPeak get(net.minecraft.server.MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(PlayerPeak::load, PlayerPeak::new, "tcorigenes_player_peak");
        }
    }

    private MobScaling() {
    }

    /** Cada jugador extra suma un 35% a la fuerza de los mobs nuevos; una vez alcanzado un
     *  numero de jugadores, ese efecto queda permanente aunque despues se desconecten. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            PlayerPeak data = PlayerPeak.get(player.getServer());
            int online = player.getServer().getPlayerCount();
            if (online > data.peak) {
                data.peak = online;
                data.setDirty();
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        // Todos los mobs (vanilla y de mods), salvo mascotas domesticadas y los NPC propios del mod.
        if ((mob instanceof TamableAnimal tamable && tamable.isTame()) || "tcorigenes".equals(mob.getType().builtInRegistryHolder().key().location().getNamespace())) {
            return;
        }
        if (mob.getPersistentData().getBoolean(SCALED_KEY)) {
            return;
        }
        mob.getPersistentData().putBoolean(SCALED_KEY, true);

        var spawn = event.getLevel().getSharedSpawnPos();
        double dx = mob.getX() - spawn.getX();
        double dz = mob.getZ() - spawn.getZ();
        // Zona segun la distancia al spawn: zona 0 = niveles 1-5, zona 1 = 2-6, ... (tope zona MAX_LEVEL).
        int zone = (int) Math.min(MAX_LEVEL, Math.sqrt(dx * dx + dz * dz) / BLOCKS_PER_LEVEL);
        int dimensionLevels = event.getLevel().dimension() == Level.NETHER ? NETHER_LEVELS
                : event.getLevel().dimension() == Level.END ? END_LEVELS : 0;
        int minLevel = 1 + zone + dimensionLevels;
        int level = minLevel + mob.getRandom().nextInt(LEVEL_SPREAD + 1);
        double bonus = (level - 1) * BONUS_PER_LEVEL;
        if (event.getLevel().getServer() != null) {
            double multiplayer = EXTRA_PLAYER_BONUS * (PlayerPeak.get(event.getLevel().getServer()).peak - 1);
            if (multiplayer > 0.0) {
                apply(mob.getAttribute(Attributes.MAX_HEALTH), MP_HEALTH_ID, "TC multiplayer health", multiplayer);
                apply(mob.getAttribute(Attributes.ATTACK_DAMAGE), MP_DAMAGE_ID, "TC multiplayer damage", multiplayer);
                mob.setHealth(mob.getMaxHealth());
            }
        }
        if (bonus <= 0.0) {
            return;
        }

        apply(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "TC health scaling", bonus);
        apply(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, "TC damage scaling", bonus);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void apply(AttributeInstance attribute, UUID id, String name, double bonus) {
        if (attribute != null && attribute.getModifier(id) == null) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
