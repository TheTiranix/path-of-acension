// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.datafixers.util.Either;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: la Divine Solar/Lunar Armor usan la misma tipografia que las armas de Celestisynth: nombre en
 * negrita con el color animado "celestial" (se le pide el color a la propia ExtraUtil de Celestisynth) y la linea
 * CELESTIAL GRADE en la descripcion.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class DivineArmorTooltip {
    private static Method colorMethod;
    private static Method argbMethod;
    private static boolean reflectionFailed;

    private DivineArmorTooltip() {
    }

    private static boolean isDivine(ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("celestisynth")
                && (id.getPath().startsWith("solar_crystal_") || id.getPath().startsWith("lunar_stone_"))
                && !id.getPath().endsWith("_block");
    }

    private static int celestialColor(int offset) {
        int scroll = (int) (System.currentTimeMillis() / 50L) + offset;
        if (!reflectionFailed) {
            try {
                if (colorMethod == null) {
                    colorMethod = Class.forName("com.aqutheseal.celestisynth.util.ExtraUtil").getMethod("getCelestialColor", int.class);
                }
                Object color = colorMethod.invoke(null, scroll);
                if (argbMethod == null) {
                    argbMethod = color.getClass().getMethod("argbInt");
                }
                return ((Integer) argbMethod.invoke(color)) & 0xFFFFFF;
            } catch (ReflectiveOperationException | RuntimeException e) {
                reflectionFailed = true;
            }
        }
        float hue = (scroll % 240) / 240.0F;
        return java.awt.Color.HSBtoRGB(0.5F + hue * 0.2F, 0.45F, 1.0F) & 0xFFFFFF;
    }

    /** Texto con un color por letra, de la paleta celestial en movimiento (cada letra va un poco adelantada a la anterior). */
    private static MutableComponent wave(String text, Style base) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            result.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(base.withColor(celestialColor(i * 6)).withBold(true)));
        }
        return result;
    }

    @SubscribeEvent
    public static void onGather(RenderTooltipEvent.GatherComponents event) {
        if (!isDivine(event.getItemStack())) {
            return;
        }
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        if (elements.isEmpty()) {
            return;
        }
        // igual que las armas de Celestisynth: el nombre y la linea CELESTIAL GRADE cambian de color letra por letra y se mueven con el tiempo
        FormattedText first = elements.get(0).left().orElse(null);
        if (first != null) {
            elements.set(0, Either.left(wave(first.getString(), first instanceof Component name ? name.getStyle() : Style.EMPTY)));
        }
        String gradeText = Component.translatable("item.celestisynth.celestial_tier").getString();
        if (elements.stream().noneMatch(e -> e.left().map(t -> t.getString().equals(gradeText)).orElse(false))) {
            elements.add(1, Either.left(wave(gradeText, Style.EMPTY)));
        } else {
            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i).left().map(t -> t.getString().equals(gradeText)).orElse(false)) {
                    elements.set(i, Either.left(wave(gradeText, Style.EMPTY)));
                }
            }
        }
    }
}
