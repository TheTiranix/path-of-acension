package com.tcorigenes.tcorigenes.compat;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/**
 * El mod PlayerRevive cancela la muerte normal del jugador y lo deja "desangrandose" en vez de
 * matarlo (solo muere de verdad si se desangra del todo o se rinde). Eso significa que
 * PlayerRespawnEvent (donde reaplicamos los bonos de raza/clase, ver ModEvents.onPlayerRespawn)
 * nunca se dispara cuando te reviven, y por eso los bonos de raza/clase parecian "perderse" al
 * morir: el jugador seguia vivo con la MISMA entidad pero sin sus AttributeModifier reaplicados.
 * PlayerRevive expone su propio evento (PlayerRevivedEvent) para este caso exacto.
 * Esta clase se registra solo si PlayerRevive esta instalado, para no romper si algun dia se
 * saca del modpack (la clase interna Handler, que sí referencia sus clases, no se carga si el
 * check de abajo da false).
 */
public final class PlayerReviveCompat {
    private PlayerReviveCompat() {
    }

    public static void register() {
        if (ModList.get().isLoaded("playerrevive")) {
            MinecraftForge.EVENT_BUS.register(Handler.class);
        }
    }

    private static final class Handler {
        private Handler() {
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onPlayerRevived(team.creative.playerrevive.api.event.PlayerRevivedEvent event) {
            net.minecraft.world.entity.player.Player player = event.getEntity();
            player.getCapability(com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                    .ifPresent(raceInfo -> com.tcorigenes.tcorigenes.attributes.RaceAttributeManager.updateAttributes(player, raceInfo.getRace()));
            player.getCapability(com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                    .ifPresent(classInfo -> com.tcorigenes.tcorigenes.playerclass.ClassAttributeManager.updateAttributes(player, classInfo.getPlayerClass()));
            if (player.getPersistentData().getBoolean("malnacido_purificado")) {
                com.tcorigenes.tcorigenes.attributes.RaceAttributeManager.applyPurifiedEffects(player);
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                com.tcorigenes.tcorigenes.favor.FavorManager.syncToClient(serverPlayer);
            }
        }
    }
}
