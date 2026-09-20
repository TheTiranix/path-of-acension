// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.ChooseRacePacket;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/** Elegir raza: fila de iconos + ficha con la descripcion completa; consume 1 Orbe de Orígenes al confirmar. */
public class RaceSelectionScreen extends OriginSelectionScreen<Race> {
    private final InteractionHand hand;

    public RaceSelectionScreen(InteractionHand hand) {
        super(Component.literal("Elige tu Origen"), List.of(Race.values()));
        this.hand = hand;
    }

    @Override
    protected Component nameOf(Race race) {
        return Component.literal(race.getDisplayName());
    }

    @Override
    protected ResourceLocation iconOf(Race race) {
        return OriginInfo.icon(race);
    }

    @Override
    protected String taglineOf(Race race) {
        return OriginInfo.tagline(race);
    }

    @Override
    protected List<Component> linesOf(Race race) {
        return OriginInfo.lines(race);
    }

    @Override
    protected void choose(Race race) {
        Networking.sendToServer(new ChooseRacePacket(race));
    }
}
