// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fondo animado del menu principal: un bosque oscuro en capas que se mueven con el mouse (profundidad), lluvia, un farol que parpadea y rayos
 * que iluminan todo de rojo y dejan ver cosas turbias. Reemplaza el panorama de vanilla (ver PanoramaRendererMixin). Las texturas las genera
 * tools/gen_menu_forest.py; LANTERN_X/LANTERN_Y tienen que coincidir con las de ese script. Si algo falla al dibujar, se apaga solo y vuelve el
 * panorama normal (el menu nunca se rompe).
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class MenuScene {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuScene.class);
    private static final int TEX_W = 1920;
    private static final int TEX_H = 1080;
    private static final float LANTERN_X = 1230F;
    private static final float LANTERN_Y = 470F;

    private static final ResourceLocation SKY = tex("sky"), SKY_LIT = tex("sky_lit");
    private static final ResourceLocation FAR = tex("far"), FAR_LIT = tex("far_lit");
    private static final ResourceLocation MID = tex("mid"), MID_LIT = tex("mid_lit");
    private static final ResourceLocation NEAR = tex("near"), NEAR_LIT = tex("near_lit");
    private static final ResourceLocation RAIN = tex("rain"), FOG = tex("fog"), GLOW = tex("glow"), VIGNETTE = tex("vignette");

    private static boolean failed;
    private static double lastDraw = -10;
    private static double nextFlash;
    private static double flashStart = -100;
    private static double thunderAt = -1;
    private static float camX;
    private static float camY;

    private MenuScene() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("tcorigenes", "textures/gui/menu/" + name + ".png");
    }

    /** El mixin del panorama pregunta si tiene que dejar de dibujar el de vanilla. */
    public static boolean active() {
        return !failed && Minecraft.getInstance().screen instanceof TitleScreen;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (failed || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        try {
            draw(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("[tcorigenes] fondo animado del menu desactivado por un error", t);
        }
    }

    // ------------------------------------------------------------------ animacion
    private static double flashIntensity(double now) {
        double t = now - flashStart;
        if (t < 0 || t > 2.0) {
            return 0;
        }
        double a = Math.exp(-t * 9.0);
        double b = t >= 0.22 ? 0.85 * Math.exp(-(t - 0.22) * 6.0) : 0;
        return Math.min(1.0, Math.max(a, b));
    }

    private static double lampIntensity(double now) {
        double v = 0.72 + 0.12 * Math.sin(now * 9.1) + 0.08 * Math.sin(now * 23.7 + 1.3);
        long slot = (long) Math.floor(now * 5.0);
        double h = Math.abs(Math.sin(slot * 12.9898) * 43758.5453) % 1.0; // ruido deterministico: a veces el farol casi se apaga
        return h < 0.11 ? v * 0.12 : v;
    }

    private static void draw(GuiGraphics g, int mouseX, int mouseY) {
        double now = Util.getMillis() / 1000.0;
        if (now - lastDraw > 0.6) { // se acaba de abrir el menu: el primer rayo cae enseguida
            nextFlash = now + 1.6;
            camX = 0;
            camY = 0;
        }
        lastDraw = now;
        if (now >= nextFlash) {
            flashStart = now;
            thunderAt = now + 0.4 + Math.random() * 0.8;
            nextFlash = now + 6.0 + Math.random() * 7.0;
        }
        if (thunderAt > 0 && now >= thunderAt) {
            thunderAt = -1;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8F, 0.35F));
        }
        float flash = (float) flashIntensity(now);
        float lamp = (float) lampIntensity(now);

        int sw = g.guiWidth();
        int sh = g.guiHeight();
        // la camara sigue al mouse con suavidad y se mece un poco sola
        float tx = (mouseX / (float) sw - 0.5F) * 2.0F + (float) Math.sin(now * 0.35) * 0.12F;
        float ty = (mouseY / (float) sh - 0.5F) * 2.0F + (float) Math.sin(now * 0.27 + 1.0) * 0.08F;
        camX += (tx - camX) * 0.08F;
        camY += (ty - camY) * 0.08F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.fill(0, 0, sw, sh, 0xFF000000);
        layer(g, SKY, SKY_LIT, flash, sw, sh, 0.004F, 1.04F);
        layer(g, FAR, FAR_LIT, flash, sw, sh, 0.012F, 1.05F);
        fog(g, sw, sh, now, 0.5F, 0.016F, 0.3F, flash);
        rain(g, sw, sh, now, 0.6F, 0.9F, 0.30F, 0.015F);
        float[] mid = layer(g, MID, MID_LIT, flash, sw, sh, 0.030F, 1.07F);
        glow(g, mid[0] + LANTERN_X * mid[2], mid[1] + LANTERN_Y * mid[3], sw, lamp);
        fog(g, sw, sh, now, 0.9F, 0.035F, 0.45F, flash);
        rain(g, sw, sh, now, 1.0F, 1.5F, 0.42F, 0.035F);
        layer(g, NEAR, NEAR_LIT, flash, sw, sh, 0.070F, 1.12F);
        rain(g, sw, sh, now, 1.5F, 2.4F, 0.50F, 0.075F);
        if (flash > 0.01F) { // destello blanco azulado
            g.fill(0, 0, sw, sh, ((int) (flash * 70) << 24) | 0xC8D8FF);
        }
        g.blit(VIGNETTE, 0, 0, sw, sh, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        g.setColor(1, 1, 1, 1);
    }

    /** Dibuja una capa (y encima su version iluminada, con la intensidad del rayo). Devuelve {x, y, escalaX, escalaY} para ubicar cosas sobre ella. */
    private static float[] layer(GuiGraphics g, ResourceLocation dark, ResourceLocation lit, float flash, int sw, int sh, float depth, float zoom) {
        float w = sw * zoom;
        float h = sh * zoom;
        float x = (sw - w) / 2F - camX * depth * sw;
        float y = (sh - h) / 2F - camY * depth * sh * 0.6F;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.blit(dark, 0, 0, (int) w, (int) h, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        if (flash > 0.01F) {
            g.setColor(1, 1, 1, flash);
            g.blit(lit, 0, 0, (int) w, (int) h, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
            g.setColor(1, 1, 1, 1);
        }
        g.pose().popPose();
        return new float[] {x, y, w / TEX_W, h / TEX_H};
    }

    private static void rain(GuiGraphics g, int sw, int sh, double now, float scale, float speed, float alpha, float depth) {
        int ts = Math.max(32, Math.round(128 * scale));
        float yOff = (float) ((now * speed * ts) % ts);
        float xOff = (float) ((now * speed * ts * 0.22) % ts) - camX * depth * sw;
        xOff = ((xOff % ts) + ts) % ts;
        g.setColor(1, 1, 1, alpha);
        for (int ix = -1; ix * ts < sw + ts; ix++) {
            for (int iy = -1; iy * ts < sh + ts; iy++) {
                g.blit(RAIN, Math.round(ix * ts - xOff), Math.round(iy * ts + yOff), ts, ts, 0, 0, 256, 256, 256, 256);
            }
        }
        g.setColor(1, 1, 1, 1);
    }

    private static void fog(GuiGraphics g, int sw, int sh, double now, float scale, float depth, float alpha, float flash) {
        int tw = Math.round(sw * 1.4F * scale);
        int th = Math.round(sh * 0.9F);
        float xOff = (float) ((now * 6.0 * scale) % tw) - camX * depth * sw;
        xOff = ((xOff % tw) + tw) % tw;
        g.setColor(1, 1, 1, alpha * (1.0F + flash));
        for (int ix = -1; ix * tw < sw + tw; ix++) {
            g.blit(FOG, Math.round(ix * tw - xOff), sh - th, tw, th, 0, 0, 512, 256, 512, 256);
        }
        g.setColor(1, 1, 1, 1);
    }

    private static void glow(GuiGraphics g, float cx, float cy, int sw, float lamp) {
        float r = sw * 0.16F * (0.85F + 0.3F * lamp);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        g.setColor(1, 1, 1, Math.min(1F, lamp * 0.9F));
        g.blit(GLOW, Math.round(cx - r), Math.round(cy - r), Math.round(r * 2), Math.round(r * 2), 0, 0, 256, 256, 256, 256);
        g.setColor(1, 1, 1, 1);
        RenderSystem.defaultBlendFunc();
    }
}
