// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido del usuario: la proteccion se muestra con solo dos numeros (proteccion con un escudito y dureza de armadura con su icono) en el
 * mismo sector que antes, y la vida es una barra que se llena segun vida actual / vida maxima, con el texto "actual/maxima" en negro
 * (con un contorno blanco para que se lea siempre) y un color que pasa de verde a rojo cuanto mas cerca de 0.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class HudOverrides {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/hud_icons.png");
    private static final int BAR_WIDTH = 81;   // el ancho de la fila de 10 corazones
    private static final int BAR_HEIGHT = 11;

    private HudOverrides() {
    }

    private static String number(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static boolean survival(Minecraft mc) {
        return mc.gameMode != null && mc.gameMode.canHurtPlayer() && mc.getCameraEntity() instanceof LocalPlayer;
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui instanceof ForgeGui gui) || !survival(mc) || mc.player == null || !HudConfig.CUSTOM_HUD.get()) {
            return;
        }
        boolean health = event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id());
        boolean armor = event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id());
        if (!health && !armor) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        int left = event.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = event.getWindow().getGuiScaledHeight() - gui.leftHeight;
        LocalPlayer player = mc.player;
        if (health) {
            drawHealthBar(g, mc, player, left, top - 1);
        } else {
            drawArmor(g, mc, player, left, top);
        }
        gui.leftHeight += 10;
    }

    private static int colorFor(float ratio) {
        int solid = HudConfig.solidColor(HudConfig.HEALTH_COLOR.get());
        if (solid != 0) {
            return solid;
        }
        // verde (lleno) -> amarillo -> rojo (cerca de 0), brillante para que el texto negro contraste
        return Mth.hsvToRgb(Mth.clamp(ratio, 0.0F, 1.0F) * 0.333F, 0.85F, 1.0F) | 0xFF000000;
    }

    private static void drawHealthBar(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        float max = Math.max(1.0F, player.getMaxHealth());
        float current = Mth.clamp(player.getHealth(), 0.0F, max);
        float ratio = current / max;
        g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF000000);           // borde
        g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF3A3A3A);                           // fondo
        int filled = Math.round(BAR_WIDTH * ratio);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + BAR_HEIGHT, colorFor(ratio));
            g.fill(x, y, x + filled, y + 2, 0x55FFFFFF);                                    // brillo
        }
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            int gold = Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * absorption / max));
            g.fill(x, y + BAR_HEIGHT - 2, x + gold, y + BAR_HEIGHT, 0xFFFFD700);
        }
        if (!HudConfig.HEALTH_TEXT.get()) {
            return;
        }
        String text = number(current) + "/" + number(max);
        int textWidth = mc.font.width(text);
        float scale = textWidth > BAR_WIDTH - 4 ? (BAR_WIDTH - 4) / (float) textWidth : 1.0F;
        float tx = x + (BAR_WIDTH - textWidth * scale) / 2.0F;
        float ty = y + (BAR_HEIGHT - 8 * scale) / 2.0F + 0.5F;
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        // contorno blanco + texto negro: se lee sobre verde, amarillo y rojo
        for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}}) {
            g.drawString(mc.font, text, d[0], d[1], 0xFFFFFFFF, false);
        }
        g.drawString(mc.font, text, 0, 0, 0xFF000000, false);
        g.pose().popPose();
    }

    private static void drawArmor(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        String protection = number(player.getArmorValue());
        String toughness = number(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        g.blit(ICONS, x, y, 0, 0, 9, 9, 32, 16);
        int tx = x + 11;
        g.drawString(mc.font, protection, tx, y + 1, 0xFFFFFFFF, true);
        int next = tx + mc.font.width(protection) + 8;
        g.blit(ICONS, next, y, 10, 0, 9, 9, 32, 16);
        g.drawString(mc.font, toughness, next + 11, y + 1, 0xFF9AE9F5, true);
    }
}
