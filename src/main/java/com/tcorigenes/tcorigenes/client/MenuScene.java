// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fondo del menu principal: el video del bosque con tormenta (assets/tcorigenes/menu/storm.bin, sin audio; lo genera tools/gen_menu_storm.py),
 * reproducido cuadro por cuadro con una textura dinamica. Encima suena la lluvia (bucle propio) y los truenos, que se disparan en el mismo
 * cuadro en que el video tiene cada rayo. Reemplaza el panorama de vanilla (ver PanoramaRendererMixin). Si algo falla (archivo faltante,
 * decodificacion, dibujo) se apaga solo y vuelve el panorama normal: el menu nunca se rompe.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class MenuScene {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuScene.class);
    private static final ResourceLocation PACK = new ResourceLocation("tcorigenes", "menu/storm.bin");
    private static final ResourceLocation TEXTURE = new ResourceLocation("tcorigenes", "dynamic/menu_storm");
    private static final SoundEvent RAIN = SoundEvent.createVariableRangeEvent(new ResourceLocation("tcorigenes", "menu.rain"));
    private static final int LOOKAHEAD = 8;
    private static final double GIVE_UP_SECONDS = 6.0;

    private static boolean failed;
    private static Video video;
    private static double lastDraw = -10;
    private static double startTime;
    private static long lastAbsFrame = -1;
    private static float camX;
    private static float camY;
    private static RainLoop rain;

    private MenuScene() {
    }

    /** El mixin del panorama pregunta si tiene que dejar de dibujar el de vanilla. */
    public static boolean active() {
        return !failed && Minecraft.getInstance().screen instanceof TitleScreen;
    }

    private static void fail(String why, Throwable t) {
        failed = true;
        LOGGER.error("[tcorigenes] video del menu desactivado: {}", why, t);
    }

    // ------------------------------------------------------------------ dibujo
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (failed || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        try {
            draw(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        } catch (Throwable t) {
            fail("error al dibujar", t);
        }
    }

    private static void draw(GuiGraphics g, int mouseX, int mouseY) {
        double now = Util.getMillis() / 1000.0;
        if (video == null) {
            video = new Video();
        }
        if (now - lastDraw > 0.6) { // se abrio el menu: el video arranca de cero
            startTime = now;
            lastAbsFrame = -1;
            camX = 0;
            camY = 0;
        }
        lastDraw = now;
        if (!video.ready()) {
            if (video.error != null) {
                fail("no se pudo cargar el video", video.error);
            }
            return; // mientras carga se ve el fondo negro
        }
        long abs = (long) Math.floor((now - startTime) * video.fps);
        int idx = (int) (abs % video.frames);
        video.show(idx);
        if (now - startTime > GIVE_UP_SECONDS && !video.shown) {
            fail("el video no mostro ningun cuadro", null);
            return;
        }
        for (long a = Math.max(lastAbsFrame + 1, abs); a <= abs; a++) { // truenos: en el mismo cuadro que cada rayo del video
            if (video.isFlash((int) (a % video.frames))) {
                thunder();
            }
        }
        lastAbsFrame = abs;

        int sw = g.guiWidth();
        int sh = g.guiHeight();
        float tx = (mouseX / (float) sw - 0.5F) * 2.0F;
        float ty = (mouseY / (float) sh - 0.5F) * 2.0F;
        camX += (tx - camX) * 0.06F;
        camY += (ty - camY) * 0.06F;
        // "cover": el video llena la pantalla recortando lo que sobre; con un poco de zoom para que se mueva con el mouse
        float screenAspect = sw / (float) sh;
        float videoAspect = video.w / (float) video.h;
        int srcW = video.w;
        int srcH = video.h;
        if (screenAspect > videoAspect) {
            srcH = Math.round(video.w / screenAspect);
        } else {
            srcW = Math.round(video.h * screenAspect);
        }
        int u = (video.w - srcW) / 2;
        int v = (video.h - srcH) / 2;
        float zoom = 1.05F;
        float dw = sw * zoom;
        float dh = sh * zoom;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1, 1, 1, 1);
        g.pose().pushPose();
        g.pose().translate((sw - dw) / 2F - camX * 0.02F * sw, (sh - dh) / 2F - camY * 0.02F * sh, 0);
        g.blit(TEXTURE, 0, 0, Math.round(dw), Math.round(dh), u, v, srcW, srcH, video.w, video.h);
        g.pose().popPose();
    }

    private static void thunder() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.85F + (float) Math.random() * 0.25F, 0.7F));
    }

    // ------------------------------------------------------------------ lluvia
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean want = active() && video != null && video.shown;
        if (want) {
            if (rain == null || rain.fadingOut || !mc.getSoundManager().isActive(rain)) {
                rain = new RainLoop();
                mc.getSoundManager().play(rain);
            }
        } else if (rain != null) {
            rain.fadingOut = true;
            rain = null;
        }
    }

    private static final class RainLoop extends AbstractTickableSoundInstance {
        private int age;
        private boolean fadingOut;

        private RainLoop() {
            super(RAIN, SoundSource.MASTER, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.001F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            this.age++;
            if (this.fadingOut) {
                this.volume -= 0.05F;
                if (this.volume <= 0.0F) {
                    this.stop();
                }
            } else {
                this.volume = Math.min(1.0F, this.age / 40.0F) * 0.6F;
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    // ------------------------------------------------------------------ video
    /** El archivo se lee en un hilo aparte y los cuadros JPEG se decodifican unos cuantos por delante de la reproduccion. */
    private static final class Video {
        volatile boolean loaded;
        volatile Throwable error;
        int w;
        int h;
        double fps;
        int frames;
        Set<Integer> flashes = new HashSet<>();
        int[] offsets;
        byte[] data;
        DynamicTexture texture;
        boolean shown;
        int shownIdx = -1;
        private final ExecutorService decoder = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tcorigenes-menu-video");
            t.setDaemon(true);
            return t;
        });
        private final Map<Integer, NativeImage> ready = new ConcurrentHashMap<>();
        private final Set<Integer> requested = ConcurrentHashMap.newKeySet();

        Video() {
            decoder.execute(() -> {
                try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(PACK).open()) {
                    byte[] all = in.readAllBytes();
                    DataInputStream d = new DataInputStream(new ByteArrayInputStream(all));
                    w = d.readInt();
                    h = d.readInt();
                    fps = d.readInt() / 1000.0;
                    frames = d.readInt();
                    int n = d.readInt();
                    for (int i = 0; i < n; i++) {
                        flashes.add(d.readInt());
                    }
                    offsets = new int[frames + 1];
                    for (int i = 0; i <= frames; i++) {
                        offsets[i] = d.readInt();
                    }
                    int headerSize = 4 * (5 + n + frames + 1);
                    data = java.util.Arrays.copyOfRange(all, headerSize, all.length);
                    loaded = true;
                    LOGGER.info("[tcorigenes] video del menu cargado: {}x{}, {} cuadros a {} fps, rayos en los cuadros {}", w, h, frames, fps, flashes);
                } catch (Throwable t) {
                    error = t;
                }
            });
        }

        boolean ready() {
            if (!loaded) {
                return false;
            }
            if (texture == null) {
                texture = new DynamicTexture(w, h, false);
                Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
            }
            return true;
        }

        boolean isFlash(int frame) {
            return flashes.contains(frame);
        }

        /** Pide los proximos cuadros al hilo decodificador y sube a la textura el que toca mostrar (si ya esta listo). */
        void show(int idx) {
            for (int k = 0; k < LOOKAHEAD; k++) {
                int f = (idx + k) % frames;
                if (!ready.containsKey(f) && requested.add(f)) {
                    decoder.execute(() -> decode(f));
                }
            }
            NativeImage img = ready.remove(idx);
            if (img != null) {
                texture.getPixels().copyFrom(img);
                texture.upload();
                img.close();
                if (!shown) {
                    LOGGER.info("[tcorigenes] video del menu: primer cuadro mostrado");
                }
                shown = true;
                shownIdx = idx;
            }
            for (Integer f : ready.keySet()) { // cuadros que quedaron atras (el equipo fue mas lento que el video): se descartan
                int ahead = Math.floorMod(f - idx, frames);
                if (ahead > LOOKAHEAD + 2) {
                    NativeImage old = ready.remove(f);
                    if (old != null) {
                        old.close();
                    }
                }
            }
        }

        private void decode(int f) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(data, offsets[f], offsets[f + 1] - offsets[f])) {
                ready.put(f, NativeImage.read(NativeImage.Format.RGBA, in));
            } catch (Throwable t) {
                error = t;
                LOGGER.error("[tcorigenes] no se pudo decodificar el cuadro {} del video del menu", f, t);
            } finally {
                requested.remove(f);
            }
        }
    }
}
