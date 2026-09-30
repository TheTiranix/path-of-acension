// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.crafting;

/** Mesas de crafteo con nivel propio (tier 2 y tier 3): el nivel decide que recetas se pueden hacer (ver CraftingGate). */
public interface CraftingTier {
    int craftingTier();
}
