// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

public enum Race {
    HUMANO("Humano"),
    HEREJE("Hereje"),
    DEVOTO("Devoto"),
    DEMONIO("Demonio"),
    ANGEL("Ángel"),
    SIERVO_DE_LA_LUNA("Siervo de la Luna"),
    ENDER_WARRIOR("Ender Warrior"),
    MALNACIDO("Malnacido"),
    STONE_GIANT("Gigante Rocoso");

    private final String displayName;

    Race(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
