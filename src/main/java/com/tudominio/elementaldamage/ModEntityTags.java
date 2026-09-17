package com.tudominio.elementaldamage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Tags de "tipo elemental" de enemigo usados por los bonos de matchup (fuego vs hielo,
 * luz vs fuego/lunar, agua vs ender, lunar vs natural). Datapack en
 * data/elementaldamage/tags/entity_type/*.json, editable sin recompilar.
 */
public final class ModEntityTags {
    public static final TagKey<EntityType<?>> FIRE_TYPE = tag("fire_type");
    public static final TagKey<EntityType<?>> ICE_TYPE = tag("ice_type");
    public static final TagKey<EntityType<?>> LUNAR_TYPE = tag("lunar_type");
    public static final TagKey<EntityType<?>> NATURAL_TYPE = tag("natural_type");
    public static final TagKey<EntityType<?>> ENDER_TYPE = tag("ender_type");

    /** "Puede llegar a pegar con este elemento" (ver MobElementalAffinity). Distinto de los tags
     *  de arriba (esos son "es debil contra"): un oso polar puede tener afinidad de hielo, nunca
     *  de fuego, aunque nada le diga que es "debil" al hielo. Datapack, editable sin recompilar. */
    public static final TagKey<EntityType<?>> FIRE_AFFINITY = tag("fire_affinity");
    public static final TagKey<EntityType<?>> ICE_AFFINITY = tag("ice_affinity");
    public static final TagKey<EntityType<?>> WATER_AFFINITY = tag("water_affinity");
    public static final TagKey<EntityType<?>> LIGHT_AFFINITY = tag("light_affinity");
    public static final TagKey<EntityType<?>> ENDER_AFFINITY = tag("ender_affinity");
    public static final TagKey<EntityType<?>> LUNAR_AFFINITY = tag("lunar_affinity");
    public static final TagKey<EntityType<?>> EARTH_AFFINITY = tag("earth_affinity");
    public static final TagKey<EntityType<?>> AIR_AFFINITY = tag("air_affinity");
    public static final TagKey<EntityType<?>> NATURAL_AFFINITY = tag("natural_affinity");

    private ModEntityTags() {
    }

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(ElementalDamage.MODID, name));
    }
}
