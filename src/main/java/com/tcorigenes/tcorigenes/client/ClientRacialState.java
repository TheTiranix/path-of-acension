// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.networking.packet.RacialStateSyncPacket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Cache cliente del estado visual de las habilidades raciales de los jugadores cercanos (ver RacialStateSyncPacket). */
public final class ClientRacialState {
    private static final Set<Integer> PRAYING = new HashSet<>();
    private static final Map<Integer, BlockState> HELD = new HashMap<>();

    private ClientRacialState() {
    }

    public static void apply(int entityId, int kind, int value) {
        if (kind == RacialStateSyncPacket.KIND_PRAYING) {
            if (value != 0) {
                PRAYING.add(entityId);
            } else {
                PRAYING.remove(entityId);
            }
        } else if (kind == RacialStateSyncPacket.KIND_HELD_BLOCK) {
            if (value == 0) {
                HELD.remove(entityId);
            } else {
                HELD.put(entityId, Block.stateById(value));
            }
        }
    }

    public static boolean isPraying(int entityId) {
        return PRAYING.contains(entityId);
    }

    public static BlockState heldBlock(int entityId) {
        return HELD.get(entityId);
    }
}
