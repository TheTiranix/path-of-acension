// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Contrato minimo que toda habilidad de clase/raza tiene que cumplir para engancharse al
 * keybind + cooldown + HUD genericos. Fase 2 (Berserker, Escudero, teletransporte del Ender
 * Warrior, invocacion del Guerrero Anima) solo necesita implementar esta interfaz y
 * registrarse en AbilityRegistry — el keybind, la validacion de cooldown server-side, el
 * paquete de red y el HUD ya estan resueltos por este esqueleto.
 */
public interface PlayerAbility {
    /** Id unico, ej. "tcorigenes:grito_de_guerra". */
    String id();

    /** Duracion del cooldown en ticks (20 ticks = 1 segundo). */
    int cooldownTicks();

    /** Textura a mostrar en el HUD (16x16 recomendado). */
    ResourceLocation icon();

    /** Efecto real de la habilidad. Ya paso la validacion de cooldown cuando se llama esto. */
    void activate(ServerPlayer player);
}
