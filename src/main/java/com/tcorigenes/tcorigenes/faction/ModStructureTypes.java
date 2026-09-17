package com.tcorigenes.tcorigenes.faction;

import com.tcorigenes.tcorigenes.TCOrigenes;
import com.tcorigenes.tcorigenes.faction.structure.FactionCampStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;

/**
 * BuiltInRegistries.STRUCTURE_TYPE ya esta congelado para cuando el constructor del mod corre,
 * asi que Registry.register directo tira "Registry is already frozen". Forge des-congela estos
 * registros vanilla durante RegisterEvent (en el mod bus), que es lo que hay que usar aca.
 */
public final class ModStructureTypes {
    public static final StructureType<FactionCampStructure> FACTION_CAMP = () -> FactionCampStructure.CODEC;

    private ModStructureTypes() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((RegisterEvent event) ->
                event.register(Registries.STRUCTURE_TYPE, helper ->
                        helper.register(ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "faction_camp"), FACTION_CAMP)));
    }
}
