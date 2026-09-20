// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.favor.client;

import com.tcorigenes.tcorigenes.favor.Deity;
import java.util.EnumMap;
import java.util.Map;

/** Cache cliente del favor del jugador, para que la HUD no dependa de leer la capability
 *  directamente (que vive en el jugador del servidor). Se actualiza via FavorSyncPacket. */
public final class ClientFavorData {
    private static final Map<Deity, Integer> FAVOR = new EnumMap<>(Deity.class);

    private ClientFavorData() {
    }

    public static void onSync(Map<Deity, Integer> favor) {
        FAVOR.clear();
        FAVOR.putAll(favor);
    }

    public static int getFavor(Deity deity) {
        return FAVOR.getOrDefault(deity, 0);
    }

    public static boolean hasAny() {
        return FAVOR.values().stream().anyMatch(value -> value != 0);
    }
}
