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

    private static int celestialColor() {
        int scroll = (int) (System.currentTimeMillis() / 50L);
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

    @SubscribeEvent
    public static void onGather(RenderTooltipEvent.GatherComponents event) {
        if (!isDivine(event.getItemStack())) {
            return;
        }
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        if (elements.isEmpty()) {
            return;
        }
        int color = celestialColor();
        Style style = Style.EMPTY.withColor(color).withBold(true);
        FormattedText first = elements.get(0).left().orElse(null);
        if (first instanceof Component name) {
            elements.set(0, Either.left(name.copy().withStyle(style)));
        }
        if (elements.stream().noneMatch(e -> e.left().map(t -> t.getString().equals(Component.translatable("item.celestisynth.celestial_tier").getString())).orElse(false))) {
            MutableComponent grade = Component.translatable("item.celestisynth.celestial_tier").withStyle(style);
            elements.add(1, Either.left(grade));
        }
    }
}
