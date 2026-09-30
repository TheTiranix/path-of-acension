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

    /**
     * Lineas propias del pack, para meter DENTRO del bloque de atributos ("When in Main Hand:", "When on Body:"...) con el
     * mismo estilo que el resto (pedido de alejandr0: nada suelto en la descripcion). Vacio si el item no tiene nada.
     */
    public static List<Component> linesFor(net.minecraft.world.item.ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return lines;
        }
        if (id.equals(com.tcorigenes.tcorigenes.compat.EndersoulGlove.ITEM_ID)) {
            lines.add(attributeLine("+" + trim(com.tcorigenes.tcorigenes.compat.EndersoulGlove.ENDER_DAMAGE) + " Daño Elemental de Ender"));
            return lines;
        }
        WeaponBalance.Spec spec = WeaponBalance.spec(id);
        if (spec != null) {
            if (spec.ranged && spec.damage != null) {
                lines.add(attributeLine("+" + trim(spec.damage.floatValue()) + " Daño por impacto"));
            }
            for (WeaponBalance.Extra extra : spec.extras) {
                lines.add(attributeLine("+" + trim(extra.amount()) + " Daño Elemental de " + elementName(extra.element())));
            }
            if (spec.cycle != null && !spec.cycle.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ResourceKey<DamageType> slot : spec.cycle) {
                    sb.append(sb.length() == 0 ? "" : " → ").append(elementName(slot));
                }
                lines.add(attributeLine((spec.perProjectile ? "Torbellinos: " : spec.ranged ? "Ciclo de impactos: " : "Ciclo de golpes: ") + sb));
            }
        }
        // el bonus de set completo es solo de la armadura, no de las armas ni herramientas de la misma linea
        for (var entry : ArmorSetBonus.SETS.entrySet()) {
            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem && id.toString().startsWith(entry.getKey())) {
                lines.add(attributeLine("Set completo: +" + (int) (ArmorSetBonus.AMPLIFICATION * 100) + "% Daño de " + elementName(entry.getValue())));
            }
        }
        return lines;
    }

    private static Component attributeLine(String text) {
        return Component.literal(" " + text).withStyle(ChatFormatting.BLUE);
    }

    /** Clave del encabezado del bloque de atributos donde van las lineas ("item.modifiers.mainhand", ".chest"...). */
    private static String headerKey(net.minecraft.world.item.ItemStack stack, ResourceLocation id) {
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor) {
            return "item.modifiers." + armor.getEquipmentSlot().getName();
        }
        return "item.modifiers.mainhand";
    }

    private static String trim(float v) {
        return v == (long) v ? Long.toString((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<Component> lines = linesFor(event.getItemStack());
        if (lines.isEmpty()) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        boolean glove = id != null && id.equals(com.tcorigenes.tcorigenes.compat.EndersoulGlove.ITEM_ID);
        String key = glove ? "item.modifiers.hand" : headerKey(event.getItemStack(), id);
        List<Component> tooltip = event.getToolTip();
        int header = -1;
        for (int i = 1; i < tooltip.size(); i++) {
            if (tooltip.get(i).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable
                    && translatable.getKey().equals(key)) {
                header = i;
                break;
            }
        }
        if (header < 0) {
            // el item no trae ese bloque: se crea uno (con linea en blanco arriba, como el de vanilla) al final
            tooltip.add(Component.empty());
            tooltip.add(glove ? Component.literal("When in Hand:").withStyle(ChatFormatting.GRAY)
                    : Component.translatable(key).withStyle(ChatFormatting.GRAY));
            tooltip.addAll(lines);
            return;
        }
        int end = header + 1;
        while (end < tooltip.size() && !tooltip.get(end).getString().isEmpty()
                && (tooltip.get(end).getString().startsWith(" ") || tooltip.get(end).getString().startsWith("+")
                || tooltip.get(end).getString().startsWith("-") || tooltip.get(end).getString().startsWith("\u00a7"))) {
            end++;
        }
        tooltip.addAll(end, lines);
    }
}
