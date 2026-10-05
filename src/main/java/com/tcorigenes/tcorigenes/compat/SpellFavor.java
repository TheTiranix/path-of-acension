// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import com.tcorigenes.tcorigenes.favor.Deity;
import com.tcorigenes.tcorigenes.favor.FavorManager;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

/**
 * Favor de los dioses segun la escuela de magia (diseño de la magia): cada hechizo lanzado de Iron's Spellbooks da favor al dios de su escuela
 * (+1, +2 con hechizos de nivel 3 o mas). Las escuelas son las de los grimorios (ver GodSpellbooks): rayo = Pater, fuego y sangre = Deiros,
 * naturaleza = Meidris, sagrada = Filis, eldritch = Luna, ender = Tempo. Pater y Deiros son rivales (el creador destierra al rebelde): cada 4
 * hechizos de uno se le resta 1 de favor al otro. El Hereje no acumula favor (ver FavorManager#addFavor) asi que no le afecta.
 */
public final class SpellFavor {
    private static final Map<String, Deity> SCHOOL_GOD = Map.of(
            "lightning", Deity.PATER,
            "fire", Deity.DEIROS,
            "blood", Deity.DEIROS,
            "nature", Deity.MEIDRIS,
            "holy", Deity.FILIS,
            "eldritch", Deity.LUNA,
            "ender", Deity.TEMPO);
    private static final String COUNTER_KEY = "pa_spell_favor_rival";
    private static final int RIVAL_EVERY = 4;

    private SpellFavor() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        if (!ModList.get().isLoaded("irons_spellbooks")) {
            return;
        }
        String className = "io.redspace.ironsspellbooks.api.events.SpellOnCastEvent";
        try {
            Class<? extends Event> type = (Class<? extends Event>) Class.forName(className);
            Consumer<Event> handler = SpellFavor::onCast;
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, (Class) type, (Consumer) handler);
        } catch (ReflectiveOperationException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger(SpellFavor.class).warn("No se pudo enganchar {}: {}", className, e.toString());
        }
    }

    private static void onCast(Event event) {
        if (!(event instanceof PlayerEvent playerEvent) || !(playerEvent.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        try {
            Object school = event.getClass().getMethod("getSchoolType").invoke(event);
            Object id = school.getClass().getMethod("getId").invoke(school);
            String path = ((net.minecraft.resources.ResourceLocation) id).getPath();
            int level = (int) event.getClass().getMethod("getSpellLevel").invoke(event);
            Deity god = SCHOOL_GOD.get(path);
            if (god == null) {
                return;
            }
            FavorManager.addFavor(player, god, level >= 3 ? 2 : 1);
            Deity rival = god == Deity.PATER ? Deity.DEIROS : god == Deity.DEIROS ? Deity.PATER : null;
            if (rival != null) {
                CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
                int count = persisted.getInt(COUNTER_KEY + "_" + god.name()) + 1;
                if (count >= RIVAL_EVERY) {
                    count = 0;
                    FavorManager.addFavor(player, rival, -1);
                }
                persisted.putInt(COUNTER_KEY + "_" + god.name(), count);
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            // sin favor por magia si cambia la API de Iron's Spellbooks
        }
    }
}
