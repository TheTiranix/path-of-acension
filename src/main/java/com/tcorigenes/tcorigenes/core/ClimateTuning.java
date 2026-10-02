// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedidos de alejandr0 sobre clima y esfuerzo:
 * - Cada bioma tiene su propio calor/frio: la temperatura base del bioma mas una variacion fija por bioma, asi hay desiertos mas calidos que
 *   otros y bosques nevados mas frios que otros. Escala el daño de hipertermia e hipotermia.
 * - Moverse sube la perdida de hidratacion y la temperatura segun el esfuerzo: quieto 0, agachado caminando 2.5%, caminar/planear lento 5%,
 *   sprintar/planear rapido 15%, parkour 20%; golpear, bloquear con escudo, minar y colocar bloques suman muy poquito.
 * - Abrigo: llenar los 4 slots de armadura da una proteccion ligera contra el frio; llenar los 4 slots de ropa y los de guantes (Curios) da
 *   mucha mas. La ropa de hielo enfria muchisimo.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ClimateTuning {
    public static final double ARMOR_PIECE = 0.04;
    public static final double CLOTHES_PIECE = 0.15;
    public static final double GLOVE = 0.10;
    public static final double WOOL_BONUS = 0.05;
    public static final double ICE_PIECE = 0.25;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] CLOTHES_SLOTS = {"head_clothes", "chest_clothes", "legs_clothes", "feet_clothes"};

    private static final Map<UUID, Float> EXERTION = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();
    private static final Map<UUID, Float> LAST_STAMINA = new HashMap<>();
    private static final Map<UUID, Long> PARKOUR_UNTIL = new HashMap<>();

    private ClimateTuning() {
    }

    // ------------------------------------------------------------------ abrigo
    private static boolean isWool(ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("toughasnails") && id.getPath().startsWith("wool_");
    }

    private static boolean isIce(ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getPath().startsWith("ice_cloth_");
    }

    private static boolean curios() {
        return ModList.get().isLoaded("curios");
    }

    /** 0..1: proteccion contra el frio de lo que lleva puesto (armadura ligera, ropa y guantes de Curios mucho mas). */
    public static double warmth(Player player) {
        double warmth = 0.0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                warmth += ARMOR_PIECE + (isWool(stack) ? WOOL_BONUS : 0.0);
            }
        }
        if (curios()) {
            for (String slot : CLOTHES_SLOTS) {
                ItemStack stack = CuriosGloves.first(player, slot);
                if (!stack.isEmpty()) {
                    warmth += CLOTHES_PIECE + (isWool(stack) ? WOOL_BONUS : 0.0);
                }
            }
            warmth += GLOVE * CuriosGloves.all(player, "hands").size();
        }
        return Math.min(1.0, warmth);
    }

    /** 0..1: cuanto enfria la ropa de hielo puesta (en los slots de ropa de Curios). */
    public static double coolness(Player player) {
        double cool = 0.0;
        if (curios()) {
            for (String slot : CLOTHES_SLOTS) {
                if (isIce(CuriosGloves.first(player, slot))) {
                    cool += ICE_PIECE;
                }
            }
        }
        return Math.min(1.0, cool);
    }

    // ------------------------------------------------------------------ clima por bioma
    /** Temperatura efectiva del bioma donde esta el jugador: base + variacion fija por bioma (hasta +-0.25). */
    public static double biomeTemperature(Player player) {
        var holder = player.level().getBiome(player.blockPosition());
        double base = holder.value().getBaseTemperature();
        double offset = holder.unwrapKey().map(key -> ((key.location().toString().hashCode() & 0xFF) / 255.0 - 0.5) * 0.5).orElse(0.0);
        return base + offset;
    }

    public static double heatFactor(Player player) {
        return Math.max(0.6, Math.min(2.0, 1.0 + (biomeTemperature(player) - 1.5) * 0.8));
    }

    public static double coldFactor(Player player) {
        return Math.max(0.6, Math.min(2.0, 1.0 + (0.0 - biomeTemperature(player)) * 0.8));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        String path = source.typeHolder().unwrapKey().map(key -> key.location().getPath()).orElse("");
        if (path.equals("hyperthermia")) {
            event.setAmount((float) (event.getAmount() * heatFactor(player)));
        } else if (path.equals("hypothermia") || source.is(DamageTypes.FREEZE)) {
            event.setAmount((float) (event.getAmount() * coldFactor(player)));
        }
    }

    // ------------------------------------------------------------------ esfuerzo
    /** Esfuerzo actual (0..1) suavizado en el tiempo; lo usa el modificador de temperatura de TAN. */
    public static float exertion(Player player) {
        return EXERTION.getOrDefault(player.getUUID(), 0.0F);
    }

    private static boolean moving(Player player) {
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        return dx * dx + dz * dz > 1.0E-4;
    }

    private static double staminaOf(ServerPlayer player) {
        try {
            Class<?> cls = Class.forName("com.alrex.parcool.common.capability.Parkourability");
            Object ability = cls.getMethod("get", Player.class).invoke(null, player);
            Object stamina = ability.getClass().getMethod("getStamina").invoke(ability);
            return ((Number) stamina.getClass().getMethod("get").invoke(stamina)).doubleValue();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return Double.NaN;
        }
    }

    private static double activity(ServerPlayer player, long now) {
        double pct = 0.0;
        boolean mv = moving(player);
        if (player.isFallFlying()) {
            pct = player.getDeltaMovement().length() > 0.9 ? 0.15 : 0.05;
        } else if (mv && player.isSprinting()) {
            pct = 0.15;
        } else if (mv && player.isShiftKeyDown()) {
            pct = 0.025;
        } else if (mv) {
            pct = 0.05;
        }
        if (ModList.get().isLoaded("parcool")) {
            double stamina = staminaOf(player);
            Float last = LAST_STAMINA.get(player.getUUID());
            if (!Double.isNaN(stamina)) {
                if (last != null && stamina < last - 0.5) {
                    PARKOUR_UNTIL.put(player.getUUID(), now + 20);
                }
                LAST_STAMINA.put(player.getUUID(), (float) stamina);
            }
            if (PARKOUR_UNTIL.getOrDefault(player.getUUID(), 0L) > now) {
                pct = Math.max(pct, 0.20);
            }
        }
        if (now - LAST_ACTION.getOrDefault(player.getUUID(), -1000L) <= 20 || player.isBlocking()) {
            pct += 0.01; // golpear, bloquear, minar y colocar bloques: muy ligeramente
        }
        return pct;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        long now = player.level().getGameTime();
        double pct = activity(player, now);
        float smoothed = EXERTION.getOrDefault(player.getUUID(), 0.0F);
        smoothed += (float) ((pct - smoothed) * 0.12);
        EXERTION.put(player.getUUID(), smoothed);
        if (pct > 0.0 && com.tcorigenes.tcorigenes.compat.TanCompat.isLoaded()) {
            // mas esfuerzo, mas rapido se pierde la hidratacion (el Ender Warrior no tiene sed: su sed se rellena aparte)
            com.tcorigenes.tcorigenes.compat.TanCompat.addThirstExhaustion(player, (float) (pct * 0.2));
        }
    }

    private static void mark(Player player) {
        if (!player.level().isClientSide()) {
            LAST_ACTION.put(player.getUUID(), player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        mark(event.getEntity());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        mark(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            mark(player);
        }
    }
}
