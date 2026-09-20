// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.ChooseClassPacket;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Elegir clase; aparece justo despues de elegir raza. No se cierra con ESC: hay que elegir. */
public class ClassSelectionScreen extends OriginSelectionScreen<PlayerClass> {
    public ClassSelectionScreen() {
        super(Component.literal("Elige tu Clase"),
                Arrays.stream(PlayerClass.values()).filter(c -> c != PlayerClass.NINGUNA).toList());
    }

    @Override
    protected Component nameOf(PlayerClass playerClass) {
        return Component.literal(playerClass.getDisplayName());
    }

    @Override
    protected ResourceLocation iconOf(PlayerClass playerClass) {
        return OriginInfo.icon(playerClass);
    }

    @Override
    protected String taglineOf(PlayerClass playerClass) {
        return OriginInfo.tagline(playerClass);
    }

    @Override
    protected List<Component> linesOf(PlayerClass playerClass) {
        return OriginInfo.lines(playerClass);
    }

    @Override
    protected void choose(PlayerClass playerClass) {
        Networking.sendToServer(new ChooseClassPacket(playerClass));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
