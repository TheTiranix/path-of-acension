// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.faction;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Una faccion de NPCs propia por cada dios del panteon (ver favor.Deity). Cada una tiene un
 * grupo de biomas "natal" (vanilla + de mods del pack, elegidos por tematica) donde
 * FactionNpcEntity elige su color/identidad al spawnear naturalmente (ver
 * FactionNpcEntity#finalizeSpawn). Deiros es un caso especial: al ser el angel caido, solo
 * aparece en el Nether, nunca en el overworld. No hay aldeas/estructuras propias todavia, solo
 * el NPC con su textura distinta segun el bioma.
 */
public enum Faction {
    // Guerra/sangre/muerte: badlands vanilla + zonas hostiles/de combate del Twilight Forest.
    PATER("Pater",
            Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS,
            key("twilightforest", "highlands"),
            key("twilightforest", "thornlands"),
            key("twilightforest", "fire_swamp"),
            key("twilightforest", "swamp")),

    // Fertilidad/animales/reproduccion: praderas vanilla + jardines flotantes del Aether +
    // sabana/luciernagas del Twilight Forest (vida abundante y pacifica).
    FILIS("Filis",
            Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW,
            key("aether", "skyroot_grove"),
            key("aether", "skyroot_meadow"),
            key("twilightforest", "oak_savannah"),
            key("twilightforest", "firefly_forest")),

    // Naturaleza/crecimiento: bosques/selvas vanilla + bosques del Aether + bosques densos y
    // fungicos del Twilight Forest.
    MEIDRIS("Meidris",
            Biomes.FOREST, Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.FLOWER_FOREST,
            key("aether", "skyroot_forest"),
            key("aether", "skyroot_woodland"),
            key("twilightforest", "forest"),
            key("twilightforest", "dense_forest"),
            key("twilightforest", "mushroom_forest"),
            key("twilightforest", "dense_mushroom_forest"),
            key("twilightforest", "clearing")),

    // Noche/misterio/magia: bosques oscuros y taiga vanilla + bosques arcanos (Ars Nouveau,
    // Quark) + zonas oscuras/frias/encantadas del Twilight Forest + simas profundas de Alex's
    // Caves.
    LUNA("Luna",
            Biomes.DARK_FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA,
            key("ars_nouveau", "archwood_forest"),
            key("quark", "glimmering_weald"),
            key("twilightforest", "dark_forest"),
            key("twilightforest", "dark_forest_center"),
            key("twilightforest", "spooky_forest"),
            key("twilightforest", "enchanted_forest"),
            key("twilightforest", "glacier"),
            key("twilightforest", "snowy_forest"),
            key("twilightforest", "underground"),
            key("alexscaves", "abyssal_chasm")),

    // Angel caido: unicamente el Nether, nunca aparece en el overworld.
    DEIROS("Deiros",
            Biomes.NETHER_WASTES, Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST,
            Biomes.SOUL_SAND_VALLEY, Biomes.BASALT_DELTAS),

    // Ciclos/tiempo: cherry grove vanilla (floracion efimera) + lagos/arroyos y la meseta final
    // del Twilight Forest (fin de un ciclo) + cuevas primordiales de Alex's Caves (tiempo
    // ancestral).
    TEMPO("Tempo",
            Biomes.CHERRY_GROVE,
            key("twilightforest", "lake"),
            key("twilightforest", "stream"),
            key("twilightforest", "final_plateau"),
            key("alexscaves", "primordial_caves"));

    private final String displayName;
    private final ResourceKey<Biome>[] biomes;

    @SafeVarargs
    Faction(String displayName, ResourceKey<Biome>... biomes) {
        this.displayName = displayName;
        this.biomes = biomes;
    }

    private static ResourceKey<Biome> key(String namespace, String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean matchesBiome(ResourceKey<Biome> biomeKey) {
        for (ResourceKey<Biome> candidate : biomes) {
            if (candidate.equals(biomeKey)) {
                return true;
            }
        }
        return false;
    }

    public static Faction fromBiome(ResourceKey<Biome> biomeKey) {
        for (Faction faction : values()) {
            if (faction.matchesBiome(biomeKey)) {
                return faction;
            }
        }
        return null;
    }

    public static Faction byOrdinalSafe(int ordinal) {
        Faction[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return PATER;
        }
        return all[ordinal];
    }
}
