// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage;

import com.tcorigenes.tcorigenes.core.Tr;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Cartelito flotante propio que aparece SOLO cuando el golpe fue de un elemento custom (fuego,
 * hielo, etc), mostrando cual y cuanto de ESE tipo, aparte del numero total que ya muestran los
 * mods de damage numbers instalados en el pack (esos no distinguen el tipo de daño). Reusa el
 * truco de ArmorStand "marker" + nombre visible que ya usa AnimaSpiritTracker: sin renderer
 * propio ni packets nuevos.
 */
public final class ElementalDamageIndicator {
    private static final class ActiveIndicator {
        final ArmorStand entity;
        long expireAtGameTime;
        float total;
        long lastHit;

        ActiveIndicator(ArmorStand entity, long expireAtGameTime, float total, long lastHit) {
            this.entity = entity;
            this.expireAtGameTime = expireAtGameTime;
            this.total = total;
            this.lastHit = lastHit;
        }

        ArmorStand entity() {
            return this.entity;
        }

        long expireAtGameTime() {
            return this.expireAtGameTime;
        }
    }

    private static final List<ActiveIndicator> ACTIVE = new ArrayList<>();
    /** Golpes seguidos del mismo tipo sobre el mismo mob se suman en UN solo cartel (antes cada golpe era una entidad: con armas rapidas llenaba la pantalla y laggeaba). */
    private static final java.util.Map<String, ActiveIndicator> MERGED = new java.util.HashMap<>();
    private static final int MERGE_WINDOW_TICKS = 10;
    private static final int MAX_ACTIVE = 40;
    private static final int LIFETIME_TICKS = 25;

    private ElementalDamageIndicator() {
    }

    /** Cartel del golpe critico fisico (punto debil). */
    public static void showCritical(LivingEntity target, float amount) {
        showOrMerge(target, "crit", amount, total -> Component.translatable("pa.msg.ab72b220cd", String.format("%.1f", total))
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
    }

    public static void show(LivingEntity target, ResourceKey<DamageType> element, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        boolean critical = com.tcorigenes.tcorigenes.core.WeakPointManager.isCriticalNow(target);
        showOrMerge(target, element.location().getPath() + (critical ? "|c" : ""), amount, total -> label(element, total, critical));
    }

    private static void showOrMerge(LivingEntity target, String key, float amount, java.util.function.Function<Float, Component> label) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = serverLevel.getGameTime();
        String id = target.getId() + "|" + key;
        ActiveIndicator existing = MERGED.get(id);
        if (existing != null && existing.entity.isAlive() && now - existing.lastHit <= MERGE_WINDOW_TICKS) {
            existing.total += amount;
            existing.lastHit = now;
            existing.expireAtGameTime = now + LIFETIME_TICKS;
            existing.entity.setCustomName(label.apply(existing.total));
            return;
        }
        if (ACTIVE.size() >= MAX_ACTIVE) {
            ActiveIndicator oldest = ACTIVE.remove(0);
            oldest.entity.discard();
        }
        ActiveIndicator created = spawn(target, label.apply(amount), amount, now);
        if (created != null) {
            MERGED.put(id, created);
        }
    }

    private static ActiveIndicator spawn(LivingEntity target, Component text, float amount, long now) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ArmorStand indicator = new ArmorStand(EntityType.ARMOR_STAND, serverLevel);
        CompoundTag markerTag = new CompoundTag();
        markerTag.putBoolean("Marker", true);
        indicator.load(markerTag);

        double offsetX = (target.getRandom().nextDouble() - 0.5) * 0.6;
        double offsetZ = (target.getRandom().nextDouble() - 0.5) * 0.6;
        indicator.setPos(target.getX() + offsetX, target.getY() + target.getBbHeight() + 0.4, target.getZ() + offsetZ);
        indicator.setInvisible(true);
        indicator.setInvulnerable(true);
        indicator.setNoGravity(true);
        indicator.setSilent(true);
        indicator.setCustomName(text);
        indicator.setCustomNameVisible(true);
        serverLevel.addFreshEntity(indicator);
        ActiveIndicator created = new ActiveIndicator(indicator, now + LIFETIME_TICKS, amount, now);
        ACTIVE.add(created);
        return created;
    }

    private static Component label(ResourceKey<DamageType> element, float amount, boolean critical) {
        String name;
        ChatFormatting color;
        if (element.equals(ModDamageTypes.FIRE_ELEMENTAL)) {
            name = Tr.s("Fuego"); color = ChatFormatting.RED;
        } else if (element.equals(ModDamageTypes.ICE)) {
            name = Tr.s("Hielo"); color = ChatFormatting.AQUA;
        } else if (element.equals(ModDamageTypes.WATER_ELEMENTAL)) {
            name = Tr.s("Agua"); color = ChatFormatting.BLUE;
        } else if (element.equals(ModDamageTypes.LIGHT)) {
            name = Tr.s("Luz"); color = ChatFormatting.YELLOW;
        } else if (element.equals(ModDamageTypes.ENDER_ELEMENTAL)) {
            name = Tr.s("Ender"); color = ChatFormatting.DARK_PURPLE;
        } else if (element.equals(ModDamageTypes.LUNAR)) {
            name = Tr.s("Lunar"); color = ChatFormatting.LIGHT_PURPLE;
        } else if (element.equals(ModDamageTypes.EARTH)) {
            name = Tr.s("Tierra"); color = ChatFormatting.GOLD;
        } else if (element.equals(ModDamageTypes.AIR)) {
            name = Tr.s("Aire"); color = ChatFormatting.WHITE;
        } else if (element.equals(ModDamageTypes.NATURAL)) {
            name = Tr.s("Natural"); color = ChatFormatting.GREEN;
        } else {
            name = Tr.s("Elemental"); color = ChatFormatting.GRAY;
        }
        if (critical) {
            return Component.translatable("pa.msg.41b8214b63", name, String.format("%.1f", amount)).withStyle(color, ChatFormatting.BOLD);
        }
        return Component.literal(name + " -" + String.format("%.1f", amount)).withStyle(color);
    }

    /** Llamar cada ~10 ticks: saca los carteles vencidos. La lista siempre es chica y acotada. */
    public static void tick(ServerLevel level) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        ACTIVE.removeIf(indicator -> {
            if (now >= indicator.expireAtGameTime() || !indicator.entity().isAlive()) {
                indicator.entity().discard();
                MERGED.values().remove(indicator);
                return true;
            }
            return false;
        });
    }
}
