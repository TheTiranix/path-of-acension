package com.tcorigenes.tcorigenes.core;

import net.minecraft.server.level.ServerPlayer;

/**
 * Otorga el nodo de PassiveSkillTree exclusivo de cada raza (ver data/tcorigenes/advancements
 * y data/tcorigenes/functions/dar_raza_*.mcfunction, y los 7 nodos "raza_*" en
 * data/skilltree/skills, referenciados desde main_tree.json). Humano no tiene nodo: es la
 * raza neutra.
 */
public final class RaceSkillGrant {
    private RaceSkillGrant() {
    }

    public static void grant(ServerPlayer player, Race race) {
        String advancementId = switch (race) {
            case HEREJE -> "tcorigenes:otorgar_raza_hereje";
            case DEVOTO -> "tcorigenes:otorgar_raza_devoto";
            case DEMONIO -> "tcorigenes:otorgar_raza_demonio";
            case ANGEL -> "tcorigenes:otorgar_raza_angel";
            case SIERVO_DE_LA_LUNA -> "tcorigenes:otorgar_raza_siervo_de_la_luna";
            case ENDER_WARRIOR -> "tcorigenes:otorgar_raza_ender_warrior";
            case MALNACIDO -> "tcorigenes:otorgar_raza_malnacido";
            case HUMANO -> null;
        };

        if (advancementId == null) {
            return;
        }

        String command = "advancement grant " + player.getGameProfile().getName() + " only " + advancementId;
        player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack().withPermission(4), command);
    }
}
