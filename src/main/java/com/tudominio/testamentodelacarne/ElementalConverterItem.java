// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prisma Convertidor: click derecho cicla el elemento (ninguno, fuego, agua, luz...). Mientras lo llevas en el
 * inventario, TODO el daño elemental que agrega tu equipo se convierte a ese elemento, asi se aprovechan armas de
 * cualquier elemento a pesar de la restriccion de un solo elemento por jugador (ver ElementalRestriction).
 */
public class ElementalConverterItem extends Item {
    public static final String[] ELEMENTS = {"", "fire_elemental", "water_elemental", "light", "lunar", "ender_elemental",
            "ice", "earth", "air", "natural"};
    public static final String[] NAMES = {"Ninguno", "Fuego", "Agua", "Luz", "Lunar", "Ender", "Hielo", "Tierra", "Aire", "Natural"};

    public ElementalConverterItem(Properties properties) {
        super(properties);
    }

    private static int indexOf(ItemStack stack) {
        String current = stack.hasTag() ? stack.getTag().getString("tc_convert_element") : "";
        for (int i = 0; i < ELEMENTS.length; i++) {
            if (ELEMENTS[i].equals(current)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            int next = (indexOf(stack) + 1) % ELEMENTS.length;
            CompoundTag tag = stack.getOrCreateTag();
            if (next == 0) {
                tag.remove("tc_convert_element");
            } else {
                tag.putString("tc_convert_element", ELEMENTS[next]);
            }
            player.displayClientMessage(Component.literal("Prisma Convertidor: " + (next == 0 ? "sin conversion"
                    : "todo tu daño elemental se convierte a " + NAMES[next])).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return indexOf(stack) != 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Convierte el daño elemental de tu equipo a un solo elemento.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Elemento actual: " + NAMES[indexOf(stack)]).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Click derecho para cambiar. Activo mientras lo llevás en el inventario.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
