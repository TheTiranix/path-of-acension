// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage;

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
    private record ActiveIndicator(ArmorStand entity, long expireAtGameTime) {
    }

    private static final List<ActiveIndicator> ACTIVE = new ArrayList<>();
    private static final int LIFETIME_TICKS = 25;

    private ElementalDamageIndicator() {
    }

    public static void show(LivingEntity target, ResourceKey<DamageType> element, float amount) {
        if (!(target.level() instanceof ServerLevel serverLevel) || amount <= 0.0F) {
            return;
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
        indicator.setCustomName(label(element, amount));
        indicator.setCustomNameVisible(true);
        serverLevel.addFreshEntity(indicator);
        ACTIVE.add(new ActiveIndicator(indicator, serverLevel.getGameTime() + LIFETIME_TICKS));
    }

    private static Component label(ResourceKey<DamageType> element, float amount) {
        String name;
        ChatFormatting color;
        if (element.equals(ModDamageTypes.FIRE_ELEMENTAL)) {
            name = "Fuego"; color = ChatFormatting.RED;
        } else if (element.equals(ModDamageTypes.ICE)) {
            name = "Hielo"; color = ChatFormatting.AQUA;
        } else if (element.equals(ModDamageTypes.WATER_ELEMENTAL)) {
            name = "Agua"; color = ChatFormatting.BLUE;
        } else if (element.equals(ModDamageTypes.LIGHT)) {
            name = "Luz"; color = ChatFormatting.YELLOW;
        } else if (element.equals(ModDamageTypes.ENDER_ELEMENTAL)) {
            name = "Ender"; color = ChatFormatting.DARK_PURPLE;
        } else if (element.equals(ModDamageTypes.LUNAR)) {
            name = "Lunar"; color = ChatFormatting.LIGHT_PURPLE;
        } else if (element.equals(ModDamageTypes.EARTH)) {
            name = "Tierra"; color = ChatFormatting.GOLD;
        } else if (element.equals(ModDamageTypes.AIR)) {
            name = "Aire"; color = ChatFormatting.WHITE;
        } else if (element.equals(ModDamageTypes.NATURAL)) {
            name = "Natural"; color = ChatFormatting.GREEN;
        } else {
            name = "Elemental"; color = ChatFormatting.GRAY;
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
                return true;
            }
            return false;
        });
    }
}
