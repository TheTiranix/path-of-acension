package com.tcorigenes.tcorigenes.compat.jei;

import java.util.List;

/** La lista completa ordenada (daño o armadura) es UNA sola "receta" con una grilla de todos los items. */
public record StatSortRecipe(List<StatEntry> entries, String statLabel) {
}
