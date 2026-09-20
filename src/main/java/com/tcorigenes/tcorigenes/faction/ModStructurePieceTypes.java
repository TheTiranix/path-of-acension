// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.faction;

import com.tcorigenes.tcorigenes.TCOrigenes;
import com.tcorigenes.tcorigenes.faction.structure.FactionCampPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;

/** Ver ModStructureTypes: hace falta RegisterEvent, un Registry.register directo tira "frozen". */
public final class ModStructurePieceTypes {
    public static final StructurePieceType.ContextlessType FACTION_CAMP = FactionCampPiece::new;

    private ModStructurePieceTypes() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((RegisterEvent event) ->
                event.register(Registries.STRUCTURE_PIECE, helper ->
                        helper.register(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "faction_camp_piece"), FACTION_CAMP)));
    }
}
