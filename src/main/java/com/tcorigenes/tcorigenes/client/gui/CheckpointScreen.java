// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.core.Tr;

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
        super(Component.translatable(loadMode ? "pa.m.ck_title_load" : "pa.m.ck_title"), buildOptions(entries, loadMode));
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
        return option.entry() == null ? Component.translatable(this.loadMode ? "pa.m.ck_continue" : "pa.m.ck_save_here")
                : Component.translatable("pa.msg.f127d0024c", (option.entry().preferred() ? "★ " : ""), option.entry().number());
    }

    @Override
    protected ResourceLocation iconOf(Option option) {
        return option.entry() == null ? ICON_NEW : ICON_BED;
    }

    @Override
    protected String taglineOf(Option option) {
        return option.entry() == null ? (this.loadMode ? Tr.s("Seguir donde saliste") : Tr.s("Nuevo punto de guardado, acá"))
                : Tr.f("Día %s", option.entry().day()) + (option.entry().preferred() ? Tr.s(" · tu punto preferido ahora mismo") : Tr.s(" · punto de guardado compartido"));
    }

    @Override
    protected List<Component> linesOf(Option option) {
        if (option.entry() == null && this.loadMode) {
            return List.of(Component.translatable("pa.msg.d93c85f320"),
                    Component.translatable("pa.msg.ca12c4e1c8"));
        }
        if (option.entry() == null) {
            return List.of(
                    Component.translatable("pa.msg.7ac1d44cb9"),
                    Component.translatable("pa.msg.f14cdba75c"),
                    Component.translatable("pa.msg.4cfefd81cb")
            );
        }
        CheckpointEntry entry = option.entry();
        return List.of(
                Component.translatable("pa.msg.b063f12557", entry.number(), entry.day()),
                Component.translatable("pa.msg.2815cbc6f8", entry.ownerName()),
                Component.translatable("pa.msg.efa0dc763f", entry.pos().toShortString(), entry.dimensionLabel()),
                Component.literal(this.loadMode ? Tr.s("Elegilo para cargar el mundo tal como estaba cuando lo guardaste. Lo hecho después se pierde.")
                        : Tr.s("Elegilo para SOBRESCRIBIRLO con esta cama y el mundo de ahora (reemplaza ese save)."))
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
