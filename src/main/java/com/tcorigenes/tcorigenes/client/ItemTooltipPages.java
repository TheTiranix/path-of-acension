// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

/**
 * Tooltips en paginas (pedido de alejandr0): pagina 1 = daño, alcance, velocidad, proteccion, dureza, spell power y
 * demas stats; pagina 2 = encantamientos y requerimientos de destreza; pagina 3 = gemas, lore, habilidad especial
 * y hechizos. Sin Shift se ve la pagina 1; con Shift apretado se cambia de pagina con el 4 y el 6 del numpad.
 * Si un mod oculta los bonus de stats de su item, se muestran igual (ver appendHiddenAttributes).
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class ItemTooltipPages {
    /** Renglones de info (sin el nombre) hasta los que se muestra todo junto, sin paginar. */
    private static final int MAX_LINES = 10;
    private static int page = 0;
    private static boolean noticeShown = false;
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");

    private ItemTooltipPages() {
    }

    /** Cada vez que se entra al mundo, avisa por chat como cambiar de pagina en los tooltips. */
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            noticeShown = false;
        } else if (!noticeShown && player.tickCount > 40) {
            noticeShown = true;
            player.sendSystemMessage(Component.literal(
                    "Tip: en la descripción de los ítems mantené Shift y usá el 4 y el 6 del numpad para cambiar de página.")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!Screen.hasShiftDown()) {
            return;
        }
        if (event.getKeyCode() == GLFW.GLFW_KEY_KP_4) {
            page = Math.max(0, page - 1);
            event.setCanceled(true);
        } else if (event.getKeyCode() == GLFW.GLFW_KEY_KP_6) {
            page = Math.min(2, page + 1);
            event.setCanceled(true);
        }
    }

    /** Saca la aclaracion "[Entity: 1 | Item: 179999]" que Apothic Attributes agrega al daño y la velocidad de ataque. */
    private static void stripAdvancedBase(Component component) {
        component.getSiblings().removeIf(sibling -> sibling.getContents() instanceof TranslatableContents translatable
                && translatable.getKey().equals("attributeslib.adv.base"));
        for (Component sibling : new ArrayList<>(component.getSiblings())) {
            stripAdvancedBase(sibling);
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) {
                    stripAdvancedBase(inner);
                }
            }
        }
    }

    private static boolean has(Component component, String prefix) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (translatable.getKey().startsWith(prefix)) {
                return true;
            }
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner && has(inner, prefix)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (has(sibling, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOurStatLine(String text) {
        return text.startsWith("Ciclo de golpes") || text.startsWith("Set completo")
                || text.startsWith("Ciclo de impactos") || text.startsWith("Torbellinos") || text.startsWith("Daño por impacto")
                || text.startsWith("Daño total") || text.startsWith("Se puede equipar") || text.startsWith("Como guante")
                || (text.startsWith("+") && text.contains("Daño de "));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        List<Component> original = event.getToolTip();
        if (original.size() < 2 || event.getItemStack().isEmpty()) {
            return;
        }
        boolean twoHandedShown = false;
        for (int i = 1; i < original.size(); i++) {
            if (has(original.get(i), "item.held.two_handed")) {
                if (twoHandedShown) {
                    original.remove(i--); // Better Combat ya lo muestra: no repetirlo
                }
                twoHandedShown = true;
            }
        }
        for (Component line : original) {
            stripAdvancedBase(line);
        }
        Component name = original.get(0);
        List<Component> stats = new ArrayList<>();
        List<Component> requirements = new ArrayList<>();
        List<Component> lore = new ArrayList<>();
        boolean hasAttributeLines = false;
        boolean inAttributeBlock = false;
        for (int i = 1; i < original.size(); i++) {
            Component line = original.get(i);
            String text = line.getString();
            if (text.isEmpty()) {
                inAttributeBlock = false;
                continue;
            }
            if (has(line, "item.held.")) {
                stats.add(line);
            } else if (has(line, "item.modifiers.")) {
                inAttributeBlock = true;
                hasAttributeLines = true;
                stats.add(line);
            } else if (inAttributeBlock || has(line, "attribute.modifier.") || has(line, "item.durability")
                    || isOurStatLine(text)) {
                stats.add(line);
                hasAttributeLines |= has(line, "attribute.modifier.");
            } else if (has(line, "enchantment.") || text.startsWith("Destreza requerida")) {
                requirements.add(line);
            } else {
                lore.add(line);
            }
        }
        List<Component> hidden = new ArrayList<>();
        if (!hasAttributeLines) {
            appendHiddenAttributes(event.getItemStack(), hidden);
            stats.addAll(hidden);
        }
        // Se pagina solo si la info pasa de MAX_LINES renglones; si entra en una pagina se muestra toda junta.
        int infoLines = stats.size() + requirements.size() + lore.size();
        if (infoLines <= MAX_LINES) {
            original.addAll(1, hidden);
            return;
        }
        List<List<Component>> pages = new ArrayList<>();
        pages.add(stats);
        pages.add(requirements);
        pages.add(lore);
        pages.removeIf(List::isEmpty);
        if (pages.size() <= 1 && lore.isEmpty() && requirements.isEmpty()) {
            // Item simple: no se toca.
            return;
        }
        int total = pages.size();
        int shown = Screen.hasShiftDown() ? Math.min(page, total - 1) : 0;
        original.clear();
        original.add(name);
        original.addAll(pages.get(shown));
        if (total > 1) {
            original.add(Component.literal(Screen.hasShiftDown()
                    ? "Página " + (shown + 1) + "/" + total + "  (Shift + Numpad 4 / 6)"
                    : "Mantené Shift para ver más páginas (" + total + ")").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Muestra los modifiers de atributos aunque el mod original oculte esas lineas (ej. flag HideFlags). */
    private static void appendHiddenAttributes(ItemStack stack, List<Component> stats) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            var modifiers = stack.getAttributeModifiers(slot);
            if (modifiers.isEmpty()) {
                continue;
            }
            stats.add(Component.translatable("item.modifiers." + slot.getName()).withStyle(ChatFormatting.GRAY));
            for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
                AttributeModifier modifier = entry.getValue();
                double amount = modifier.getAmount();
                boolean percent = modifier.getOperation() != AttributeModifier.Operation.ADDITION;
                double shown = percent ? amount * 100.0 : amount;
                String sign = shown >= 0 ? "+" : "";
                stats.add(Component.literal(" " + sign + FORMAT.format(shown) + (percent ? "%" : "") + " ")
                        .append(Component.translatable(entry.getKey().getDescriptionId()))
                        .withStyle(shown >= 0 ? ChatFormatting.BLUE : ChatFormatting.RED));
            }
        }
    }
}
