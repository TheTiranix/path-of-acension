// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Ruido turbio de fondo de los menus (fuera de un mundo): un bucle ambiental propio
 * (assets/tcorigenes/sounds/menu_ambient.ogg) que sube en fade al entrar a un menu y baja al cargar un mundo.
 * Lo reproduce el mod con el motor de sonido de Minecraft (no FancyMenu) para que funcione en todos los menus
 * y respete solo el volumen general.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class MenuAmbience {
    private static final SoundEvent EVENT = SoundEvent.createVariableRangeEvent(new ResourceLocation("tcorigenes", "menu.ambient"));
    private static final float TARGET_VOLUME = 0.75F;
    private static final int FADE_IN_TICKS = 80;
    private static final float FADE_OUT_PER_TICK = 0.04F;

    private static Loop loop;

    private MenuAmbience() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean inMenu = mc.level == null && mc.screen != null;
        if (inMenu) {
            if (loop == null || loop.fadingOut || !mc.getSoundManager().isActive(loop)) {
                loop = new Loop();
                mc.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.fadingOut = true;
            loop = null;
        }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private int age = 0;
        private boolean fadingOut = false;

        private Loop() {
            super(EVENT, SoundSource.MASTER, RandomSource.create());
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
                this.volume -= FADE_OUT_PER_TICK;
                if (this.volume <= 0.0F) {
                    this.stop();
                }
            } else {
                this.volume = Math.min(1.0F, this.age / (float) FADE_IN_TICKS) * TARGET_VOLUME;
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
