package com.tcorigenes.tcorigenes.playerclass;

public enum PlayerClass {
    NINGUNA("Ninguna"),
    RITUALISTA_ARCANO("Ritualista Arcano"),
    BERSERKER("Berserker"),
    GUERRERO_ANIMA("Guerrero Ánima"),
    ESCUDERO("Escudero"),
    ARQUERO("Arquero");

    private final String displayName;

    PlayerClass(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
