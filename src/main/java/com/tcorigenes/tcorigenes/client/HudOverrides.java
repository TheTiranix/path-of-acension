// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Interfaz del jugador del pack: limpia, oscura y con acentos carmesi, con la letra Cinzel de los menus. Reemplaza (todo con barras planas, sin
 * texturas) la vida, la proteccion, el hambre, la sed de Tough As Nails, la experiencia y la barra rapida de vanilla. La vida es una barra
 * que se llena segun vida actual / vida maxima con el texto "actual/maxima" (el color se elige en la pestaña "Interfaz": por defecto el
 * carmesi del pack, que late cuando queda poca). La proteccion son dos numeros: proteccion con un escudito y dureza con su icono. Se apaga
 * entero con la opcion customHud (HudConfig): vuelven los corazones y la barra de vanilla.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class HudOverrides {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/hud_icons.png");
    private static final Style FONT = Style.EMPTY.withFont(new ResourceLocation("tcorigenes", "menu"));
    private static final int BAR_WIDTH = 81;   // el ancho de la fila de 10 corazones
    private static final int BAR_HEIGHT = 11;

    // paleta del pack
    private static final int PANEL = 0xC00E080B;
    private static final int EDGE = 0xFF6E1018;
    private static final int EDGE_HOT = 0xFFE2242C;
    private static final int INSIDE = 0xFF0F090C;
    private static final int TEXT = 0xFFF0E0E0;
    private static final int CRIMSON_TOP = 0xFFE02A34;
    private static final int CRIMSON_BOTTOM = 0xFF8E1220;
    private static final int AMBER_TOP = 0xFFE6A23A;
    private static final int AMBER_BOTTOM = 0xFF9A5E1A;
    private static final int WATER_TOP = 0xFF55AEEA;
    private static final int WATER_BOTTOM = 0xFF1F5FA0;
    private static final int XP_TOP = 0xFFEFCB62;
    private static final int XP_BOTTOM = 0xFFA9801F;

    private HudOverrides() {
    }

    // se dibujaba ocho veces el texto en cada cuadro creando el arreglo de desplazamientos de nuevo y formateando los numeros con String.format:
    // ahora el texto (y su ancho) se recalculan solo cuando cambian la vida o la armadura
    private static float cachedCurrent = Float.NaN;
    private static float cachedMax = Float.NaN;
    private static Component cachedHealthText = Component.empty();
    private static int cachedHealthWidth;
    private static double cachedArmor = Double.NaN;
    private static double cachedToughness = Double.NaN;
    private static Component cachedArmorText = Component.empty();
    private static Component cachedToughnessText = Component.empty();

    private static String number(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static Component styled(String text) {
        return Component.literal(text).withStyle(FONT);
    }

    private static boolean survival(Minecraft mc) {
        return mc.gameMode != null && mc.gameMode.canHurtPlayer() && mc.getCameraEntity() instanceof LocalPlayer;
    }

    // ------------------------------------------------------------------ eventos
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui instanceof ForgeGui gui) || mc.player == null || mc.gameMode == null || !HudConfig.CUSTOM_HUD.get()) {
            return;
        }
        ResourceLocation id = event.getOverlay().id();
        GuiGraphics g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        LocalPlayer player = mc.player;

        if (id.equals(VanillaGuiOverlay.HOTBAR.id())) {
            if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR && mc.getCameraEntity() instanceof Player) {
                event.setCanceled(true);
                drawHotbar(g, mc, player, width, height);
            }
            return;
        }
        if (id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            if (mc.gameMode.hasExperience() && player.jumpableVehicle() == null) {
                event.setCanceled(true);
                drawExperience(g, mc, player, width, height);
            }
            return;
        }
        if (!survival(mc)) {
            return;
        }
        int left = width / 2 - 91;
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
            int top = height - gui.leftHeight;
            // dos lotes: primero las barras y despues el texto, asi el numero queda siempre por encima aunque otro mod reordene los lotes
            g.drawManaged(() -> drawHealthBarShapes(g, player, left, top - 1));
            g.drawManaged(() -> drawHealthText(g, mc, player, left, top - 1));
            gui.leftHeight += 10;
        } else if (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) {
            event.setCanceled(true);
            int top = height - gui.leftHeight;
            g.drawManaged(() -> drawArmor(g, mc, player, left, top));
            gui.leftHeight += 10;
        } else if (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) && !(player.getVehicle() instanceof LivingEntity)) {
            event.setCanceled(true);
            int top = height - gui.rightHeight;
            g.drawManaged(() -> drawFood(g, mc, player, width / 2 + 10, top - 1));
            gui.rightHeight += 10;
        } else if (id.getNamespace().equals("toughasnails") && id.getPath().equals("thirst_level") && !(player.getVehicle() instanceof LivingEntity)) {
            int thirst = thirstOf(player);
            if (thirst >= 0) {
                event.setCanceled(true);
                int top = height - gui.rightHeight;
                g.drawManaged(() -> drawThirst(g, mc, thirst, width / 2 + 10, top - 1));
                gui.rightHeight += 10;
            }
        }
    }

    // ------------------------------------------------------------------ piezas
    private static void panel(GuiGraphics g, int x, int y, int w, int h, int edge) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, edge);
        g.fill(x, y, x + w, y + h, INSIDE);
    }

    /** Barra plana: borde de 1 px, fondo oscuro, relleno con degradado, brillo arriba y cuatro marcas suaves. */
    private static void bar(GuiGraphics g, int x, int y, int w, int h, float ratio, int top, int bottom, int edge) {
        panel(g, x, y, w, h, edge);
        int filled = Math.round(w * Mth.clamp(ratio, 0.0F, 1.0F));
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + h, top, bottom);
            g.fill(x, y, x + filled, y + 1, 0x40FFFFFF);
        }
        for (int i = w / 4; i < w; i += w / 4) {
            g.fill(x + i, y, x + i + 1, y + h, 0x38000000);
        }
    }

    private static void centered(GuiGraphics g, Minecraft mc, Component text, int cx, int y, int color) {
        g.drawString(mc.font, text, cx - mc.font.width(text) / 2, y, color, true);
    }

    private static int pulse(int quiet, int hot, double speed) {
        float t = (float) ((Math.sin(System.currentTimeMillis() / 1000.0 * speed) + 1.0) / 2.0);
        return lerp(quiet, hot, t);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gr = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    // ------------------------------------------------------------------ vida
    private static void drawHealthBarShapes(GuiGraphics g, LocalPlayer player, int x, int y) {
        float max = Math.max(1.0F, player.getMaxHealth());
        float current = Mth.clamp(player.getHealth(), 0.0F, max);
        float ratio = current / max;
        int solid = HudConfig.solidColor(HudConfig.HEALTH_COLOR.get());
        int top = solid != 0 ? solid : CRIMSON_TOP;
        int bottom = solid != 0 ? (solid & 0xFEFEFE) >> 1 | 0xFF000000 : CRIMSON_BOTTOM;
        int edge = ratio <= 0.3F ? pulse(EDGE, EDGE_HOT, 7.0) : EDGE; // poca vida: el borde late
        bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, ratio, top, bottom, edge);
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            int gold = Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * absorption / max));
            g.fill(x, y + BAR_HEIGHT - 2, x + gold, y + BAR_HEIGHT, 0xFFE6C040);
        }
    }

    private static void drawHealthText(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        if (!HudConfig.HEALTH_TEXT.get()) {
            return;
        }
        float max = Math.max(1.0F, player.getMaxHealth());
        float current = Mth.clamp(player.getHealth(), 0.0F, max);
        if (current != cachedCurrent || max != cachedMax) {
            cachedCurrent = current;
            cachedMax = max;
            cachedHealthText = styled(number(current) + " / " + number(max));
            cachedHealthWidth = mc.font.width(cachedHealthText);
        }
        float scale = cachedHealthWidth > BAR_WIDTH - 4 ? (BAR_WIDTH - 4) / (float) cachedHealthWidth : 1.0F;
        float tx = x + (BAR_WIDTH - cachedHealthWidth * scale) / 2.0F;
        float ty = y + (BAR_HEIGHT - 8 * scale) / 2.0F + 0.5F;
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(mc.font, cachedHealthText, 0, 0, TEXT, true);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ proteccion
    private static void drawArmor(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        double armorValue = player.getArmorValue();
        double toughnessValue = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (armorValue != cachedArmor || toughnessValue != cachedToughness) {
            cachedArmor = armorValue;
            cachedToughness = toughnessValue;
            cachedArmorText = styled(number(armorValue));
            cachedToughnessText = styled(number(toughnessValue));
        }
        g.blit(ICONS, x, y, 0, 0, 9, 9, 32, 16);
        int tx = x + 11;
        g.drawString(mc.font, cachedArmorText, tx, y + 1, TEXT, true);
        int next = tx + mc.font.width(cachedArmorText) + 8;
        g.blit(ICONS, next, y, 10, 0, 9, 9, 32, 16);
        g.drawString(mc.font, cachedToughnessText, next + 11, y + 1, 0xFF9AE9F5, true);
    }

    // ------------------------------------------------------------------ hambre y sed
    private static void drawFood(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        int food = player.getFoodData().getFoodLevel();
        boolean starving = player.hasEffect(MobEffects.HUNGER);
        int edge = food <= 6 ? pulse(EDGE, EDGE_HOT, 5.0) : EDGE;
        bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, food / 20.0F, starving ? 0xFF8FA040 : AMBER_TOP, starving ? 0xFF4A5A1C : AMBER_BOTTOM, edge);
        float saturation = Math.min(player.getFoodData().getSaturationLevel(), food);
        if (saturation > 0.0F) {
            g.fill(x, y + BAR_HEIGHT - 2, x + Math.round(BAR_WIDTH * saturation / 20.0F), y + BAR_HEIGHT, 0xFFF5E2A8);
        }
        centered(g, mc, styled(Integer.toString(food)), x + BAR_WIDTH / 2, y + 2, TEXT);
    }

    private static void drawThirst(GuiGraphics g, Minecraft mc, int thirst, int x, int y) {
        int edge = thirst <= 6 ? pulse(EDGE, EDGE_HOT, 5.0) : EDGE;
        bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, thirst / 20.0F, WATER_TOP, WATER_BOTTOM, edge);
        centered(g, mc, styled(Integer.toString(thirst)), x + BAR_WIDTH / 2, y + 2, TEXT);
    }

    private static Method thirstHelper;
    private static Method thirstGetter;
    private static boolean thirstMissing;

    /** Sed de Tough As Nails (0 a 20), o -1 si el mod no esta o cambio su API (en ese caso se deja su barra como viene). Se busca por reflexion. */
    private static int thirstOf(Player player) {
        if (thirstMissing) {
            return -1;
        }
        try {
            if (thirstHelper == null) {
                thirstHelper = Class.forName("toughasnails.api.thirst.ThirstHelper").getMethod("getThirst", Player.class);
                thirstGetter = Class.forName("toughasnails.api.thirst.IThirst").getMethod("getThirst");
            }
            return (Integer) thirstGetter.invoke(thirstHelper.invoke(null, player));
        } catch (Throwable t) {
            thirstMissing = true;
            return -1;
        }
    }

    // ------------------------------------------------------------------ experiencia
    private static void drawExperience(GuiGraphics g, Minecraft mc, LocalPlayer player, int width, int height) {
        int x = width / 2 - 91;
        int y = height - 28;
        g.drawManaged(() -> bar(g, x, y, 182, 3, player.experienceProgress, XP_TOP, XP_BOTTOM, EDGE));
        if (player.experienceLevel > 0) {
            g.drawManaged(() -> centered(g, mc, styled(Integer.toString(player.experienceLevel)), width / 2, y - 9, 0xFFEFCB62));
        }
    }

    // ------------------------------------------------------------------ barra rapida
    private static void drawHotbar(GuiGraphics g, Minecraft mc, LocalPlayer player, int width, int height) {
        int x0 = width / 2 - 91;
        int y0 = height - 22;
        int selected = player.getInventory().selected;
        g.drawManaged(() -> {
            g.fill(x0 - 1, y0 - 1, x0 + 183, y0 + 23, EDGE);
            g.fill(x0, y0, x0 + 182, y0 + 22, PANEL);
            for (int i = 1; i < 9; i++) {
                g.fill(x0 + i * 20, y0 + 3, x0 + i * 20 + 1, y0 + 19, 0x30A01820);
            }
            ItemStack off = player.getOffhandItem();
            if (!off.isEmpty()) {
                int ox = player.getMainArm().getOpposite() == HumanoidArm.LEFT ? x0 - 29 : x0 + 188;
                g.fill(ox - 1, y0 - 1, ox + 23, y0 + 23, EDGE);
                g.fill(ox, y0, ox + 22, y0 + 22, PANEL);
            }
            int sx = x0 - 1 + selected * 20;
            g.fill(sx + 1, y0, sx + 23, y0 + 22, 0x40E2242C);                 // brillo detras del slot elegido
            g.renderOutline(sx, y0 - 1, 24, 24, EDGE_HOT);
        });
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            g.renderItem(stack, x0 + 3 + i * 20, y0 + 3);
            g.renderItemDecorations(mc.font, stack, x0 + 3 + i * 20, y0 + 3);
        }
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty()) {
            int ox = player.getMainArm().getOpposite() == HumanoidArm.LEFT ? x0 - 29 : x0 + 188;
            g.renderItem(off, ox + 3, y0 + 3);
            g.renderItemDecorations(mc.font, off, ox + 3, y0 + 3);
        }
    }
}
