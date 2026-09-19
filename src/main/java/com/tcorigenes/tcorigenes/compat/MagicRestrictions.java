package com.tcorigenes.tcorigenes.compat;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import java.lang.reflect.Method;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

/**
 * La magia es cosa del Ritualista Arcano. Los demas solo tienen acceso limitado:
 * - Iron's Spellbooks: cualquiera puede lanzar hechizos de nivel 1; de nivel 2 para arriba, solo el Ritualista.
 * - Ars Nouveau: solo el Ritualista.
 * Se engancha por reflexion (sin compilar contra esos mods) y solo si estan instalados.
 */
public final class MagicRestrictions {
    private static final String IRONS_EVENT = "io.redspace.ironsspellbooks.api.events.SpellPreCastEvent";
    private static final String ARS_EVENT = "com.hollingsworth.arsnouveau.api.event.SpellCastEvent";

    private MagicRestrictions() {
    }

    public static void register() {
        if (ModList.get().isLoaded("irons_spellbooks")) {
            hook(IRONS_EVENT, event -> {
                Player player = playerOf(event);
                if (player != null && !isMage(player) && spellLevel(event) > 1) {
                    deny(player, "Solo el Ritualista Arcano puede lanzar hechizos de nivel 2 o mas.");
                    event.setCanceled(true);
                }
            });
        }
        if (ModList.get().isLoaded("ars_nouveau")) {
            hook(ARS_EVENT, event -> {
                Player player = playerOf(event);
                if (player != null && !isMage(player)) {
                    deny(player, "Solo el Ritualista Arcano puede usar magia de Ars Nouveau.");
                    event.setCanceled(true);
                }
            });
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hook(String className, java.util.function.Consumer<Event> handler) {
        try {
            Class<? extends Event> type = (Class<? extends Event>) Class.forName(className);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, (Class) type, (java.util.function.Consumer) handler);
        } catch (ReflectiveOperationException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger(MagicRestrictions.class).warn("No se pudo enganchar {}: {}", className, e.toString());
        }
    }

    private static Player playerOf(Event event) {
        try {
            Method getEntity = event.getClass().getMethod("getEntity");
            return getEntity.invoke(event) instanceof Player player ? player : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static int spellLevel(Event event) {
        try {
            return (int) event.getClass().getMethod("getSpellLevel").invoke(event);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    private static boolean isMage(Player player) {
        return player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass() == PlayerClass.RITUALISTA_ARCANO).orElse(false)
                || player.hasPermissions(2) && player.isCreative();
    }

    private static void deny(Player player, String message) {
        if (!player.level().isClientSide()) {
            player.displayClientMessage(Component.literal(message), true);
        }
    }
}
