package com.tcorigenes.tcorigenes.favor;

/**
 * El panteon completo segun el lore: Pater (orden/luz/creador), Meidris (naturaleza/vida/magia
 * blanca), Luna (oculto/magia oscura, paralelo al panteon principal, no antagonista), Filis
 * (redencion/curacion), Deiros (rebeldia/fuego), Tempo (el 6to dios, secreto para el jugador
 * hasta que el lore lo revele - eso es un tema de contenido/narrativa, no de este enum).
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
