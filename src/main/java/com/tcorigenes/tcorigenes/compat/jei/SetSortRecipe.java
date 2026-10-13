// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat.jei;

import com.tcorigenes.tcorigenes.compat.LightArmorSets;
import java.util.List;

/** Una pagina de sets de armadura (una fila por set, ver SetSortCategory). */
public record SetSortRecipe(List<LightArmorSets.ArmorSet> sets) {
}
