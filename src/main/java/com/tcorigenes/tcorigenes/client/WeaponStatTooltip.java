// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.weapon.ArmorSetBonus;
import com.tcorigenes.tcorigenes.weapon.WeaponBalance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Muestra en el tooltip TODAS las stats que agrega el pack a armas y armaduras (daño elemental fijo, ciclos de
 * golpes, dos manos, bonus de set), para que sean visibles aunque el mod original no las muestre.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class WeaponStatTooltip {
    private WeaponStatTooltip() {
    }

    public static String elementName(ResourceKey<DamageType> element) {
        if (element == null) {
            return "Normal";
        }
        return switch (element.location().getPath()) {
            case "light" -> "Luz";
            case "fire_elemental" -> "Fuego";
            case "water_elemental" -> "Agua";
            case "lunar" -> "Lunar";
            case "ender_elemental" -> "Ender";
            case "ice" -> "Hielo";
            case "earth" -> "Tierra";
            case "air" -> "Aire";
            case "natural" -> "Natural";
            default -> element.location().getPath();
        };
    }

    public static ChatFormatting elementColor(ResourceKey<DamageType> element) {
        if (element == null) {
            return ChatFormatting.WHITE;
        }
        return switch (element.location().getPath()) {
            case "light" -> ChatFormatting.YELLOW;
            case "fire_elemental" -> ChatFormatting.RED;
            case "water_elemental" -> ChatFormatting.AQUA;
            case "lunar" -> ChatFormatting.DARK_PURPLE;
            case "ender_elemental" -> ChatFormatting.LIGHT_PURPLE;
            case "ice" -> ChatFormatting.BLUE;
            case "earth" -> ChatFormatting.GOLD;
            case "air" -> ChatFormatting.WHITE;
            case "natural" -> ChatFormatting.GREEN;
            default -> ChatFormatting.GRAY;
        };
    }

    /** Lineas propias del pack para un item (vacio si no tiene nada que mostrar). */
    public static List<Component> linesFor(net.minecraft.world.item.ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return lines;
        }
        WeaponBalance.Spec spec = WeaponBalance.spec(id);
        if (spec != null) {
            if (Boolean.TRUE.equals(spec.twoHanded)) {
                lines.add(Component.literal("A dos manos").withStyle(ChatFormatting.GOLD));
            }
            if (spec.ranged && spec.damage != null) {
                lines.add(Component.literal("Daño por impacto: " + trim(spec.damage.floatValue()))
                        .withStyle(ChatFormatting.RED));
            }
            for (WeaponBalance.Extra extra : spec.extras) {
                lines.add(Component.literal("+" + trim(extra.amount()) + " Daño de " + elementName(extra.element()))
                        .withStyle(elementColor(extra.element())));
            }
            if (spec.cycle != null && !spec.cycle.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ResourceKey<DamageType> slot : spec.cycle) {
                    sb.append(sb.length() == 0 ? "" : " → ").append(elementName(slot));
                }
                lines.add(Component.literal((spec.ranged ? "Ciclo de impactos: " : "Ciclo de golpes: ") + sb).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        for (var entry : ArmorSetBonus.SETS.entrySet()) {
            if (id.toString().startsWith(entry.getKey())) {
                lines.add(Component.literal("Set completo: +" + (int) (ArmorSetBonus.AMPLIFICATION * 100) + "% Daño de "
                        + elementName(entry.getValue())).withStyle(elementColor(entry.getValue())));
            }
        }
        return lines;
    }

    private static String trim(float v) {
        return v == (long) v ? Long.toString((long) v) : Float.toString(v);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<Component> lines = linesFor(event.getItemStack());
        if (!lines.isEmpty()) {
            event.getToolTip().addAll(1, lines);
        }
    }
}
