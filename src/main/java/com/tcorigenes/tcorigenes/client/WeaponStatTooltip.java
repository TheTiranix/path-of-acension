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
        if (id.equals(com.tcorigenes.tcorigenes.compat.EndersoulGlove.ITEM_ID)) {
            lines.add(Component.literal("Se puede equipar en el slot de guantes").withStyle(ChatFormatting.GOLD));
            lines.add(Component.literal("Como guante: +" + trim(com.tcorigenes.tcorigenes.compat.EndersoulGlove.ENDER_DAMAGE)
                    + " Daño de Ender por golpe").withStyle(elementColor(com.tudominio.elementaldamage.ModDamageTypes.ENDER_ELEMENTAL)));
        }
        WeaponBalance.Spec spec = WeaponBalance.spec(id);
        addTotalDamage(lines, stack, spec);
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
                lines.add(Component.literal((spec.perProjectile ? "Torbellinos (uno de cada elemento): " : spec.ranged ? "Ciclo de impactos: " : "Ciclo de golpes: ") + sb).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        // el bonus de set completo es solo de la armadura, no de las armas ni herramientas de la misma linea
        for (var entry : ArmorSetBonus.SETS.entrySet()) {
            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem && id.toString().startsWith(entry.getKey())) {
                lines.add(Component.literal("Set completo: +" + (int) (ArmorSetBonus.AMPLIFICATION * 100) + "% Daño de "
                        + elementName(entry.getValue())).withStyle(elementColor(entry.getValue())));
            }
        }
        return lines;
    }

    /** "Daño total": daño normal (con el 1 de base del jugador incluido) mas los daños elementales fijos del arma. */
    private static void addTotalDamage(List<Component> lines, net.minecraft.world.item.ItemStack stack, WeaponBalance.Spec spec) {
        if (spec != null && spec.ranged) {
            return; // los arcos muestran "Daño por impacto"
        }
        double normal = 0.0;
        for (var modifier : stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION) {
                normal += modifier.getAmount();
            }
        }
        if (normal <= 0.0) {
            return;
        }
        normal += 1.0; // el daño base del jugador: el total real que se hace con un golpe cargado
        float elemental = spec == null ? 0.0F : spec.extrasTotal();
        String text = "Daño total: " + trim((float) (normal + elemental));
        if (elemental > 0.0F) {
            text += " (" + trim((float) normal) + " normal + " + trim(elemental) + " elemental)";
        }
        lines.add(Component.literal(text).withStyle(ChatFormatting.RED));
    }

    private static String trim(float v) {
        return v == (long) v ? Long.toString((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<Component> lines = linesFor(event.getItemStack());
        if (!lines.isEmpty()) {
            event.getToolTip().addAll(1, lines);
        }
    }
}
