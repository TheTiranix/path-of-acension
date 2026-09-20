// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.faction.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tcorigenes.tcorigenes.faction.Faction;
import com.tcorigenes.tcorigenes.faction.ModStructureTypes;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Structure real (buscable con /locate structure) para el campamento de cada faccion. Una sola
 * clase sirve para las 6; el nombre de la faccion viaja en el propio json de worldgen/structure
 * (campo "faction") y decide paleta/comportamiento (ver Faction, FactionCampBuilder).
 */
public class FactionCampStructure extends Structure {
    public static final Codec<FactionCampStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            Codec.STRING.fieldOf("faction").forGetter(s -> s.faction)
    ).apply(instance, FactionCampStructure::new));

    private final String faction;

    public FactionCampStructure(StructureSettings settings, String faction) {
        super(settings);
        this.faction = faction;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Faction fac = Faction.valueOf(faction.toUpperCase(Locale.ROOT));
        int x = context.chunkPos().getMinBlockX();
        int z = context.chunkPos().getMinBlockZ();
        int y = fac == Faction.DEIROS
                ? 64
                : context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
        BlockPos origin = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(origin, builder ->
                builder.addPiece(new FactionCampPiece(origin, fac))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.FACTION_CAMP;
    }
}
