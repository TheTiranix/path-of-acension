// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression;

import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadout;
import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider;
import net.minecraft.world.entity.player.Player;

/**
 * Nivel del jugador (mide su progreso): empieza en 1 y sube 1 por cada punto de habilidad que compra (ver SkillTreeManager#buyPoint), no con la
 * experiencia vanilla. Lo usan las habilidades raciales para escalar su poder y los limites.
 */
public final class PlayerLevel {
    private PlayerLevel() {
    }

    public static int of(Player player) {
        return 1 + player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                .map(PlayerAbilityLoadout.IPlayerAbilityLoadout::getPointsBought).orElse(0);
    }
}
