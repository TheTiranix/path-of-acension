// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * El proc elemental de Aire (ver ElementalDamageEvents#applyOnHitEffect) crea un LightningBolt con
 * setVisualOnly(true) solo para el destello, pero ese flag NO evita el sonido de trueno de vanilla
 * (LightningBolt lo reproduce sin importar si es visual-only). Resultado: se escuchaba un trueno con el
 * cielo totalmente despejado.
 *
 * Como el rayo REAL del clima solo puede aparecer mientras esta tronando, silenciar el sonido de trueno
 * cuando NO esta tronando afecta unicamente a estos rayos falsos: nunca a una tormenta real.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class SilentFakeThunder {
    private SilentFakeThunder() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() == null || event.getSound().getLocation() == null) {
            return;
        }
        var location = event.getSound().getLocation();
        boolean isThunderSound = location.equals(SoundEvents.LIGHTNING_BOLT_THUNDER.getLocation())
                || location.equals(SoundEvents.LIGHTNING_BOLT_IMPACT.getLocation());
        if (!isThunderSound) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level != null && !level.isThundering()) {
            event.setSound(null);
        }
    }
}
