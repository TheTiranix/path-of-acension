// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression.client;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.HashSet;
import java.util.Set;

/** Copia en el cliente del estado del arbol (la manda SkillSyncPacket). */
public final class ClientSkillData {
    private static int points;
    private static long xp;
    private static int bought;
    private static PlayerClass playerClass = PlayerClass.NINGUNA;
    private static Set<String> unlocked = new HashSet<>();

    private ClientSkillData() {
    }

    public static void set(int newPoints, long newXp, int newBought, String className, Set<String> newUnlocked) {
        points = newPoints;
        xp = newXp;
        bought = newBought;
        try {
            playerClass = PlayerClass.valueOf(className);
        } catch (IllegalArgumentException e) {
            playerClass = PlayerClass.NINGUNA;
        }
        unlocked = new HashSet<>(newUnlocked);
    }

    public static int points() {
        return points;
    }

    public static long xp() {
        return xp;
    }

    public static int bought() {
        return bought;
    }

    public static PlayerClass playerClass() {
        return playerClass;
    }

    public static Set<String> unlocked() {
        return unlocked;
    }
}
