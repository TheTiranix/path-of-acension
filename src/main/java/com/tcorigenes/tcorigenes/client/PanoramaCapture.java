// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * /panorama [tamaño]: saca las 6 caras de un panorama (mismo metodo que la captura de panorama de vanilla) desde donde estas parado, mirando
 * hacia donde miras. Quedan en screenshots/panorama/panorama_0.png .. panorama_5.png, listas para ponerlas en config/fancymenu/panoramas/<nombre>/
 * y usarlas como fondo 3D (que gira) del menu principal.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class PanoramaCapture {
    private PanoramaCapture() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("panorama")
                .executes(c -> capture(1024))
                .then(Commands.argument("tamaño", IntegerArgumentType.integer(256, 4096)).executes(c -> capture(IntegerArgumentType.getInteger(c, "tamaño")))));
    }

    private static int capture(int size) {
        Minecraft mc = Minecraft.getInstance();
        // en el proximo tick: ya se cerro el chat y la captura sale sin la interfaz
        mc.tell(() -> mc.gui.getChat().addMessage(mc.grabPanoramixScreenshot(mc.gameDirectory, size, size)));
        return 1;
    }
}
