package com.tudominio.elementaldamage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Que elemento(s) "posee" cada tipo de mob, UNA vez por entidad (guardado en persistentData,
 * sobrevive recarga de chunk). Solo un 20% de los mobs con perfil definido llegan a tener algo
 * (no todos): de esos, siempre es su elemento PRIMARIO (el "base", el mas logico para esa
 * especie - ej. zombie=lunar por ser un no-muerto maldito), y con 15% de chance ADEMAS un
 * secundario de su propia lista (nunca uno random de cualquier lado: un oso polar jamas va a
 * salir con fuego, no tiene fuego en su lista). Un mob sin perfil definido nunca tiene ninguno.
 */
public final class MobElementalAffinity {
    private static final String ROLLED_KEY = "elemental_affinity_rolled";
    private static final String LIST_KEY = "elemental_affinity";
    private static final double OVERALL_CHANCE = 0.20;
    private static final double SECOND_ELEMENT_CHANCE = 0.15;

    private record Profile(ResourceKey<DamageType> primary, List<ResourceKey<DamageType>> secondary) {
        private static Profile of(ResourceKey<DamageType> primary, ResourceKey<DamageType>... secondary) {
            return new Profile(primary, List.of(secondary));
        }
    }

    private static final Map<EntityType<?>, Profile> PROFILES = new HashMap<>();

    static {
        // Fuego
        PROFILES.put(EntityType.BLAZE, Profile.of(ModDamageTypes.FIRE_ELEMENTAL));
        PROFILES.put(EntityType.MAGMA_CUBE, Profile.of(ModDamageTypes.FIRE_ELEMENTAL));
        PROFILES.put(EntityType.WITHER_SKELETON, Profile.of(ModDamageTypes.FIRE_ELEMENTAL, ModDamageTypes.LUNAR));
        PROFILES.put(EntityType.HOGLIN, Profile.of(ModDamageTypes.FIRE_ELEMENTAL));
        PROFILES.put(EntityType.ZOGLIN, Profile.of(ModDamageTypes.FIRE_ELEMENTAL));
        PROFILES.put(EntityType.ZOMBIFIED_PIGLIN, Profile.of(ModDamageTypes.FIRE_ELEMENTAL));
        PROFILES.put(EntityType.CREEPER, Profile.of(ModDamageTypes.FIRE_ELEMENTAL));
        PROFILES.put(EntityType.PIGLIN_BRUTE, Profile.of(ModDamageTypes.EARTH, ModDamageTypes.FIRE_ELEMENTAL));
        PROFILES.put(EntityType.GHAST, Profile.of(ModDamageTypes.AIR, ModDamageTypes.FIRE_ELEMENTAL));

        // Hielo (SIN fuego en su lista, nunca puede salir con eso)
        PROFILES.put(EntityType.STRAY, Profile.of(ModDamageTypes.ICE));
        PROFILES.put(EntityType.POLAR_BEAR, Profile.of(ModDamageTypes.ICE));
        PROFILES.put(EntityType.SNOW_GOLEM, Profile.of(ModDamageTypes.ICE));

        // Agua
        PROFILES.put(EntityType.DROWNED, Profile.of(ModDamageTypes.WATER_ELEMENTAL));
        PROFILES.put(EntityType.GUARDIAN, Profile.of(ModDamageTypes.WATER_ELEMENTAL));
        PROFILES.put(EntityType.ELDER_GUARDIAN, Profile.of(ModDamageTypes.WATER_ELEMENTAL));

        // Luz
        PROFILES.put(EntityType.IRON_GOLEM, Profile.of(ModDamageTypes.LIGHT));

        // Ender
        PROFILES.put(EntityType.ENDERMAN, Profile.of(ModDamageTypes.ENDER_ELEMENTAL));
        PROFILES.put(EntityType.ENDERMITE, Profile.of(ModDamageTypes.ENDER_ELEMENTAL));
        PROFILES.put(EntityType.SHULKER, Profile.of(ModDamageTypes.ENDER_ELEMENTAL));
        PROFILES.put(EntityType.ENDER_DRAGON, Profile.of(ModDamageTypes.ENDER_ELEMENTAL));

        // Lunar: el zombie base es lunar (no-muerto maldito), con tierra como secundario ocasional
        PROFILES.put(EntityType.ZOMBIE, Profile.of(ModDamageTypes.LUNAR, ModDamageTypes.EARTH));
        PROFILES.put(EntityType.PHANTOM, Profile.of(ModDamageTypes.LUNAR, ModDamageTypes.AIR));
        PROFILES.put(EntityType.VEX, Profile.of(ModDamageTypes.LUNAR, ModDamageTypes.AIR));
        PROFILES.put(EntityType.WITCH, Profile.of(ModDamageTypes.LUNAR, ModDamageTypes.NATURAL));
        PROFILES.put(EntityType.EVOKER, Profile.of(ModDamageTypes.LUNAR));

        // Tierra
        PROFILES.put(EntityType.HUSK, Profile.of(ModDamageTypes.EARTH, ModDamageTypes.FIRE_ELEMENTAL));
        PROFILES.put(EntityType.SKELETON, Profile.of(ModDamageTypes.EARTH));
        PROFILES.put(EntityType.SILVERFISH, Profile.of(ModDamageTypes.EARTH));
        PROFILES.put(EntityType.RAVAGER, Profile.of(ModDamageTypes.EARTH));

        // Aire
        PROFILES.put(EntityType.BAT, Profile.of(ModDamageTypes.AIR));
        PROFILES.put(EntityType.BEE, Profile.of(ModDamageTypes.AIR, ModDamageTypes.NATURAL));

        // Natural
        PROFILES.put(EntityType.WOLF, Profile.of(ModDamageTypes.NATURAL));
        PROFILES.put(EntityType.FOX, Profile.of(ModDamageTypes.NATURAL));
        PROFILES.put(EntityType.SPIDER, Profile.of(ModDamageTypes.NATURAL, ModDamageTypes.EARTH));
        PROFILES.put(EntityType.CAVE_SPIDER, Profile.of(ModDamageTypes.NATURAL, ModDamageTypes.EARTH));
        PROFILES.put(EntityType.SLIME, Profile.of(ModDamageTypes.NATURAL));
        PROFILES.put(EntityType.COW, Profile.of(ModDamageTypes.NATURAL));
        PROFILES.put(EntityType.PIG, Profile.of(ModDamageTypes.NATURAL));
        PROFILES.put(EntityType.SHEEP, Profile.of(ModDamageTypes.NATURAL));
        PROFILES.put(EntityType.PANDA, Profile.of(ModDamageTypes.NATURAL));
    }

    private MobElementalAffinity() {
    }

    public static void rollIfNeeded(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(ROLLED_KEY)) {
            return;
        }
        data.putBoolean(ROLLED_KEY, true);

        Profile profile = PROFILES.get(entity.getType());
        if (profile == null) {
            return; // sin perfil definido: este mob nunca tiene daño elemental.
        }

        RandomSource random = entity.getRandom();
        if (random.nextDouble() >= OVERALL_CHANCE) {
            return; // no le tocó (80% de los mobs con perfil se quedan sin elemento igual).
        }

        List<ResourceKey<DamageType>> chosen = new ArrayList<>();
        chosen.add(profile.primary());
        if (!profile.secondary().isEmpty() && random.nextDouble() < SECOND_ELEMENT_CHANCE) {
            chosen.add(profile.secondary().get(random.nextInt(profile.secondary().size())));
        }

        ListTag listTag = new ListTag();
        chosen.forEach(key -> listTag.add(StringTag.valueOf(key.location().toString())));
        data.put(LIST_KEY, listTag);
    }

    public static List<ResourceKey<DamageType>> get(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(LIST_KEY)) {
            return List.of();
        }
        ListTag listTag = data.getList(LIST_KEY, net.minecraft.nbt.Tag.TAG_STRING);
        List<ResourceKey<DamageType>> result = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            ResourceLocation loc = ResourceLocation.tryParse(listTag.getString(i));
            if (loc != null) {
                result.add(ResourceKey.create(Registries.DAMAGE_TYPE, loc));
            }
        }
        return result;
    }
}
