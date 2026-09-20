// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.favor;

/**
 * El panteon segun el lore final:
 * - Pater: creador tirano y perfeccionista; destierra al inframundo a Deiros, el angel rebelde
 *   de los 7 pecados capitales.
 * - Meidris: compasiva (naturaleza, vida, magia blanca); de ella nace Filis.
 * - Filis: redencion y curacion, hija de Meidris.
 * - Tempo: apatico; en secreto empodera a los Herejes y controla el End (secreto para el jugador).
 * - Luna: hija agenero de Deiros; sirve a Pater controlando la magia oscura y las criaturas de la noche.
 * - Deiros: rebeldia y fuego, desterrado.
 * De donde sale el favor de cada uno: ver FavorEvents y AltarBlock.
 */
public enum Deity {
    PATER("Pater"),
    MEIDRIS("Meidris"),
    LUNA("Luna"),
    FILIS("Filis"),
    DEIROS("Deiros"),
    TEMPO("Tempo");

    private final String displayName;

    Deity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
