package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.progression.SkillTreeManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * El bono pasivo de cada origen ya no depende de PassiveSkillTree: lo aplica el arbol propio
 * (ver SkillTreeManager). Al elegir/cambiar raza alcanza con refrescar los bonos.
 */
public final class RaceSkillGrant {
    private RaceSkillGrant() {
    }

    public static void grant(ServerPlayer player, Race race) {
        SkillTreeManager.refresh(player);
    }
}
