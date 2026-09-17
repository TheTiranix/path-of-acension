package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.core.Race;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cache cliente de "que raza (y si esta purificado) tiene cada jugador" (ver RaceSyncPacket). */
public final class ClientRaceData {
    private record Entry(Race race, boolean purified) {
    }

    private static final Map<UUID, Entry> DATA = new HashMap<>();

    private ClientRaceData() {
    }

    public static void set(UUID playerId, Race race, boolean purified) {
        DATA.put(playerId, new Entry(race, purified));
    }

    public static Race get(UUID playerId) {
        Entry entry = DATA.get(playerId);
        return entry != null ? entry.race() : Race.HUMANO;
    }

    public static boolean isPurified(UUID playerId) {
        Entry entry = DATA.get(playerId);
        return entry != null && entry.purified();
    }
}
