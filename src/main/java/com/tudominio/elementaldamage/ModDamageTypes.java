package com.tudominio.elementaldamage;

import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * Claves de los 5 tipos de daño elemental (datapack en data/elementaldamage/damage_type/).
 * Uso: ElementalDamageSource.create(level, ModDamageTypes.LIGHT, attacker) para infligirlos
 * desde items/hechizos/habilidades (fase 2, cuando se escriban los Origenes de clase).
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> LIGHT = key("light");
    public static final ResourceKey<DamageType> FIRE_ELEMENTAL = key("fire_elemental");
    public static final ResourceKey<DamageType> WATER_ELEMENTAL = key("water_elemental");
    public static final ResourceKey<DamageType> LUNAR = key("lunar");
    public static final ResourceKey<DamageType> ENDER_ELEMENTAL = key("ender_elemental");
    public static final ResourceKey<DamageType> ICE = key("ice");
    public static final ResourceKey<DamageType> EARTH = key("earth");
    public static final ResourceKey<DamageType> AIR = key("air");
    public static final ResourceKey<DamageType> NATURAL = key("natural");

    /** Los 9 tipos elementales, para distinguir "esto es daño de un elemento nuestro" de
     *  cualquier otro tipo de daño vanilla/de otro mod (mob_attack, player_attack, etc). */
    public static final Set<ResourceKey<DamageType>> ALL = Set.of(
            LIGHT, FIRE_ELEMENTAL, WATER_ELEMENTAL, LUNAR, ENDER_ELEMENTAL, ICE, EARTH, AIR, NATURAL);

    private ModDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(ElementalDamage.MODID, name));
    }
}
