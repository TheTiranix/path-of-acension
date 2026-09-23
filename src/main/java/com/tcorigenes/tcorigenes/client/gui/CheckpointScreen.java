// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.networking.packet.ChooseCheckpointPacket;
import com.tcorigenes.tcorigenes.networking.packet.OpenCheckpointScreenPacket.CheckpointEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Se abre al dormirte de verdad (ver CheckpointManager), antes de crear ningun punto de guardado
 *  ni copia del mundo nueva: "Guardar acá" crea uno en esta cama; el resto son los puntos activos
 *  compartidos por todo el servidor, para elegir cual preferís para tu proximo respawn/revivida
 *  (reemplaza a /respawnpoint). Se puede cerrar con ESC sin elegir nada. */
public class CheckpointScreen extends OriginSelectionScreen<CheckpointScreen.Option> {
    private static final ResourceLocation ICON_NEW = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/checkpoint/new.png");
    private static final ResourceLocation ICON_BED = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/checkpoint/bed.png");

    /** entry == null significa "crear uno nuevo aca". */
    record Option(CheckpointEntry entry) {
    }

    private final boolean loadMode;

    public CheckpointScreen(List<CheckpointEntry> entries, boolean loadMode) {
        super(Component.literal(loadMode ? "Cargar Punto de Guardado" : "Punto de Guardado"), buildOptions(entries, loadMode));
        this.loadMode = loadMode;
    }

    private static List<Option> buildOptions(List<CheckpointEntry> entries, boolean loadMode) {
        List<Option> options = new ArrayList<>();
        if (!loadMode) {
            options.add(new Option(null)); // "Guardar acá"; al cargar solo se ofrecen los saves de cama
        }
        for (CheckpointEntry entry : entries) {
            options.add(new Option(entry));
        }
        return options;
    }

    @Override
    protected Component nameOf(Option option) {
        return option.entry() == null ? Component.literal(this.loadMode ? "Continuar" : "Guardar acá")
                : Component.literal((option.entry().preferred() ? "★ " : "") + option.entry().ownerName());
    }

    @Override
    protected ResourceLocation iconOf(Option option) {
        return option.entry() == null ? ICON_NEW : ICON_BED;
    }

    @Override
    protected String taglineOf(Option option) {
        return option.entry() == null ? (this.loadMode ? "Seguir donde saliste" : "Nuevo punto de guardado, acá")
                : (option.entry().preferred() ? "Tu punto preferido ahora mismo" : "Punto de guardado compartido");
    }

    @Override
    protected List<Component> linesOf(Option option) {
        if (option.entry() == null && this.loadMode) {
            return List.of(Component.literal("Seguís en el mundo tal como lo dejaste al salir (tu último Guardar y salir)."),
                    Component.literal("Elegí cualquier punto de cama de la lista para volver a ese momento: el mundo se restaura y volvés a entrar."));
        }
        if (option.entry() == null) {
            return List.of(
                    Component.literal("Crea un punto de guardado nuevo en esta cama."),
                    Component.literal("Se guarda una copia entera del mundo tal como está ahora: si más tarde cae el grupo entero, el mundo puede volver a este momento."),
                    Component.literal("Tus puntos de guardado anteriores siguen valiendo un día entero más.")
            );
        }
        CheckpointEntry entry = option.entry();
        return List.of(
                Component.literal("Colocado por " + entry.ownerName() + "."),
                Component.literal("Ubicación: " + entry.pos().toShortString() + " en " + entry.dimensionLabel() + "."),
                Component.literal(this.loadMode ? "Elegilo para cargar el mundo tal como estaba cuando lo guardaste. Lo hecho después se pierde."
                        : "Elegilo para SOBRESCRIBIRLO con esta cama y el mundo de ahora (reemplaza ese save).")
        );
    }

    @Override
    protected void choose(Option option) {
        if (this.loadMode && option.entry() == null) {
            return; // "Continuar": no hay nada que mandar
        }
        Networking.sendToServer(new ChooseCheckpointPacket(option.entry() == null ? null : option.entry().id(), this.loadMode));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.loadMode; // al entrar al mundo hay que elegir un save
    }
}
