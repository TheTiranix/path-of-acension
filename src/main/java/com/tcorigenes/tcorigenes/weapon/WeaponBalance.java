// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import com.tudominio.elementaldamage.ModDamageTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * Tabla unica del balance de armas de mods de terceros (pedidos de alejandr0 en Discord). Cada arma
 * tiene una "spec": daño normal TOTAL (el numero que se ve en el tooltip), velocidad de ataque total,
 * alcance total, si es de dos manos, daño elemental fijo que se suma como golpe aparte, un ciclo de
 * golpes (ej. fuego, lunar, normal, normal) y destreza requerida. Los items NO listados no se tocan.
 * La aplican WeaponDamageOverrides (atributos), WeaponElemental (elementales y ciclos), WeaponWeights
 * (destreza) y TwoHandedWeapons.
 */
public final class WeaponBalance {
    public record Extra(ResourceKey<DamageType> element, float amount) {
    }

    /** Un slot de ciclo: null = golpe normal. */
    public static final class Spec {
        public Double damage;
        /** Copiar el daño total de otro item (ej. knightmetal = diamante). */
        public ResourceLocation damageLike;
        public Double speed;
        public Double reach;
        public boolean removeBlockReach;
        public Boolean twoHanded;
        /** Arma de proyectiles (arco): el daño es por IMPACTO del proyectil, no un atributo de melee. */
        public boolean ranged;
        /** El elemento del ciclo lo lleva cada proyectil (se asigna al aparecer), no cada impacto (ej. los 3 torbellinos). */
        public boolean perProjectile;
        public Integer dex;
        public final List<Extra> extras = new ArrayList<>();
        public List<ResourceKey<DamageType>> cycle;

        public Spec dmg(double v) {
            this.damage = v;
            return this;
        }

        public Spec like(String id) {
            this.damageLike = rl(id);
            return this;
        }

        public Spec speed(double v) {
            this.speed = v;
            return this;
        }

        public Spec reach(double v) {
            this.reach = v;
            return this;
        }

        public Spec noBlockReach() {
            this.removeBlockReach = true;
            return this;
        }

        public Spec shot() {
            this.ranged = true;
            return this;
        }

        public Spec perProjectile() {
            this.ranged = true;
            this.perProjectile = true;
            return this;
        }

        public Spec two() {
            this.twoHanded = true;
            return this;
        }

        public Spec one() {
            this.twoHanded = false;
            return this;
        }

        public Spec dex(int v) {
            this.dex = v;
            return this;
        }

        public Spec el(ResourceKey<DamageType> element, double amount) {
            this.extras.add(new Extra(element, (float) amount));
            return this;
        }

        @SafeVarargs
        public final Spec cycle(ResourceKey<DamageType>... slots) {
            this.cycle = java.util.Arrays.asList(slots);
            return this;
        }

        public float extrasTotal() {
            float total = 0.0F;
            for (Extra extra : this.extras) {
                total += extra.amount();
            }
            return total;
        }
    }

    /** Escala en bloque el daño de un grupo de items segun la relacion target/original de uno de referencia. */
    public record Family(ResourceLocation ref, double refTarget, List<ResourceLocation> members) {
    }

    public static final Map<ResourceLocation, Spec> SPECS = new HashMap<>();
    public static final List<Family> FAMILIES = new ArrayList<>();
    /** Armaduras cuya proteccion se iguala a la de otra pieza (ej. neptune = diamante). */
    public static final Map<ResourceLocation, ResourceLocation> ARMOR_LIKE = new HashMap<>();
    /** Armaduras cuya proteccion se escala por un factor (dark metal: ver static block). */
    public static final Map<ResourceLocation, ResourceLocation> ARMOR_SCALE_REF = new HashMap<>();

    /** Armaduras con proteccion, tenacidad y durabilidad propias (ref = pieza de diamante para reutilizar sus UUID). */
    public record ArmorFixed(double defense, double toughness, int durability, ResourceLocation uuidRef) {
    }

    public static final Map<ResourceLocation, ArmorFixed> ARMOR_FIXED = new HashMap<>();

    static final ResourceKey<DamageType> N = null;
    static final ResourceKey<DamageType> LIGHT = ModDamageTypes.LIGHT;
    static final ResourceKey<DamageType> FIRE = ModDamageTypes.FIRE_ELEMENTAL;
    static final ResourceKey<DamageType> WATER = ModDamageTypes.WATER_ELEMENTAL;
    static final ResourceKey<DamageType> LUNAR = ModDamageTypes.LUNAR;
    static final ResourceKey<DamageType> ENDER = ModDamageTypes.ENDER_ELEMENTAL;
    static final ResourceKey<DamageType> ICE = ModDamageTypes.ICE;
    static final ResourceKey<DamageType> EARTH = ModDamageTypes.EARTH;
    static final ResourceKey<DamageType> AIR = ModDamageTypes.AIR;
    static final ResourceKey<DamageType> NATURAL = ModDamageTypes.NATURAL;

    private WeaponBalance() {
    }

    static ResourceLocation rl(String id) {
        return ResourceLocation.tryParse(id);
    }

    static Spec w(String id) {
        Spec spec = new Spec();
        SPECS.put(rl(id), spec);
        return spec;
    }

    private static final String[] MATERIALS = {"wooden", "stone", "iron", "golden", "diamond", "netherite"};

    /** Familia por material (wooden_X ... netherite_X): el diamante manda, el resto escala proporcional. */
    private static void family(String ns, String type, double diamondDamage, Double allSpeed, Double diamondSpeed) {
        List<ResourceLocation> members = new ArrayList<>();
        for (String material : MATERIALS) {
            members.add(rl(ns + ":" + material + "_" + type));
        }
        FAMILIES.add(new Family(rl(ns + ":diamond_" + type), diamondDamage, members));
        if (allSpeed != null) {
            for (ResourceLocation member : members) {
                SPECS.computeIfAbsent(member, k -> new Spec()).speed = allSpeed;
            }
        }
        if (diamondSpeed != null) {
            SPECS.computeIfAbsent(rl(ns + ":diamond_" + type), k -> new Spec()).speed = diamondSpeed;
        }
    }

    /** Velocidad proporcional: los demas materiales escalan su velocidad segun lo que cambio la del diamante. */
    public static final Map<ResourceLocation, ResourceLocation> SPEED_FAMILY_REF = new HashMap<>();

    private static void speedFamily(String ns, String type) {
        for (String material : MATERIALS) {
            if (!material.equals("diamond")) {
                SPEED_FAMILY_REF.put(rl(ns + ":" + material + "_" + type), rl(ns + ":diamond_" + type));
            }
        }
    }

    private static void group(String refId, double refTarget, String... members) {
        List<ResourceLocation> list = new ArrayList<>();
        for (String member : members) {
            list.add(rl(member));
        }
        FAMILIES.add(new Family(rl(refId), refTarget, list));
    }

    static {
        // ---------------------------------------------------------------- Aether
        w("aether:gravitite_sword").dmg(18);
        group("aether:gravitite_sword", 18, "aether:gravitite_axe", "aether:gravitite_pickaxe",
                "aether:gravitite_shovel", "aether:gravitite_hoe");
        w("aether:zanite_sword").dmg(13);
        group("aether:zanite_sword", 13, "aether:zanite_axe", "aether:zanite_pickaxe",
                "aether:zanite_shovel", "aether:zanite_hoe");
        w("aether:holy_sword").dmg(6).el(LIGHT, 8);
        w("aether:lightning_sword").dmg(8).el(AIR, 2).el(LIGHT, 4);
        w("aether:valkyrie_lance").dmg(9).speed(1.5).reach(3.5).two().el(LIGHT, 3);
        w("aether:hammer_of_kingbdogz").dmg(9).el(LIGHT, 3);
        w("aether:pig_slayer").dmg(10).el(LIGHT, 2).el(EARTH, 2);
        w("aether:flaming_sword").dmg(9).el(LIGHT, 3);
        w("aether:vampire_blade").dmg(10).el(LIGHT, 4);

        // ------------------------------------------------------------- Celestisynth
        // Wrath of the Desert: 3 torbellinos por uso, cada uno con su elemento fijo.
        w("cataclysm:cursed_bow").dmg(15000).shot();
        w("cataclysm:wrath_of_the_desert").dmg(25000).perProjectile().cycle(LUNAR, N, AIR);
        w("celestisynth:rainfall_serenity").dmg(120000).shot().cycle(AIR, LIGHT, N, N);
        w("celestisynth:aquaflora").dmg(180000).cycle(WATER, N, N, N);
        w("celestisynth:breezebreaker").dmg(180000).speed(3).cycle(AIR, NATURAL, N, N);
        w("celestisynth:solaris").dmg(200000).speed(2.7).cycle(FIRE, LUNAR, N, N);
        w("celestisynth:crescentia").dmg(450000).speed(1.5).two().cycle(ENDER, EARTH, N, N);
        w("celestisynth:frostbound").dmg(450000).speed(1.5).two().cycle(ICE, AIR, N, N);
        w("celestisynth:keres").dmg(400000).cycle(LUNAR, AIR, N, N);
        w("celestisynth:poltergeist").dmg(220000).speed(2.4).cycle(LIGHT, NATURAL, N, N);

        // ------------------------------------------------------------ L_Ender's Cataclysm
        w("cataclysm:khopesh").dmg(6.5);
        w("cataclysm:coral_spear").dmg(4).el(WATER, 2.5);
        w("cataclysm:the_annihilator").dmg(350).el(LIGHT, 100).el(AIR, 50);
        w("cataclysm:the_immolator").dmg(350).el(FIRE, 100).el(LUNAR, 50);
        w("cataclysm:tidal_claws").dmg(350).el(WATER, 150);
        w("cataclysm:meat_shredder").dmg(600).reach(4).two().el(LUNAR, 350);
        w("cataclysm:ancient_spear").dmg(70);
        w("cataclysm:astrape").dmg(600).el(AIR, 350);
        w("cataclysm:final_fractal").dmg(300000).cycle(LIGHT, LUNAR, N, N);
        w("cataclysm:infernal_forge").dmg(350).el(FIRE, 150);
        w("cataclysm:void_forge").dmg(350).el(ENDER, 150);
        w("cataclysm:the_incinerator").dmg(600).el(FIRE, 300);
        w("cataclysm:soul_render").dmg(600).two().el(LUNAR, 150).el(AIR, 150);
        w("cataclysm:ceraunus").dmg(700).el(WATER, 350);
        w("cataclysm:zweiender").dmg(600);
        w("cataclysm:black_steel_sword").dmg(13);
        group("cataclysm:black_steel_sword", 13, "cataclysm:black_steel_axe", "cataclysm:black_steel_pickaxe",
                "cataclysm:black_steel_shovel", "cataclysm:black_steel_hoe");

        // ------------------------------------------------------------ Born in Chaos
        w("born_in_chaos_v1:spiritual_sword").dmg(6);
        w("born_in_chaos_v1:frostbitten_blade").dmg(18).two();
        w("born_in_chaos_v1:spider_bite_sword").dmg(7).el(NATURAL, 3);
        w("born_in_chaos_v1:soul_cutlass").dmg(9);
        w("born_in_chaos_v1:intoxicating_dagger").dmg(7).dex(25);
        w("born_in_chaos_v1:dark_ritual_dagger").dmg(7).dex(25);
        w("born_in_chaos_v1:nightmare_scythe").dmg(24);
        w("born_in_chaos_v1:shell_mace").dmg(10);
        w("born_in_chaos_v1:great_reaper_axe").dmg(14);
        w("born_in_chaos_v1:soulbane").dmg(350).el(FIRE, 100).el(LUNAR, 50);
        w("born_in_chaos_v1:sharpened_dark_metal_sword").dmg(10);

        // ----------------------------------------------------------------- otros mods
        w("iceandfire:silver_sword").dmg(6);
        w("iceandfire:myrmex_desert_sword").dmg(6);
        w("iceandfire:myrmex_jungle_sword").dmg(6);
        w("iceandfire:myrmex_desert_sword_venom").dmg(6);
        w("iceandfire:myrmex_jungle_sword_venom").dmg(6);
        w("iceandfire:dread_sword").dmg(6.5);
        w("iceandfire:amphithere_macuahuitl").dmg(7);
        w("iceandfire:hippogryph_sword").dmg(6).el(AIR, 3);
        w("iceandfire:tide_trident").dmg(18);
        w("iceandfire:ghost_sword").dmg(8).el(LUNAR, 3);
        w("mekanismtools:lapis_lazuli_sword").dmg(5.5);
        w("eeeabsmobs:immortal_sword").dmg(8);
        w("eeeabsmobs:guardian_axe").dmg(200);
        w("eeeabsmobs:netherworld_katana").speed(3);
        w("scary_mobs:lunar_axe").dmg(13).two().el(LUNAR, 15);
        w("scary_mobs:mallet").dmg(90).one().el(EARTH, 20);
        w("seadwellers:depth_sword").dmg(6).el(WATER, 3);
        w("sculkhorde:sculk_sweeper_sword").dmg(13).el(LUNAR, 10);
        w("sculkhorde:blade_of_purity").dmg(40).el(LIGHT, 20);
        w("alexscaves:extinction_spear").dmg(350).el(NATURAL, 150);
        w("mutantmonsters:hulk_hammer").dmg(40).el(LUNAR, 20);
        w("naturesaura:depth_sword").dmg(14).el(LUNAR, 4);
        w("mowziesmobs:wrought_axe").dmg(600).el(EARTH, 300);
        w("irons_spellbooks:amethyst_rapier").dmg(22).reach(2.5);
        w("irons_spellbooks:claymore").dmg(15).el(EARTH, 3);
        w("irons_spellbooks:spellbreaker").dmg(39);
        w("twilightforest:ice_sword").dmg(18).el(ICE, 5);
        w("twilightforest:fiery_sword").dmg(26);
        group("twilightforest:fiery_sword", 26, "twilightforest:fiery_pickaxe");
        w("twilightforest:diamond_minotaur_axe").dmg(26);
        w("twilightforest:giant_sword").dmg(21).reach(5).noBlockReach();
        w("immersive_weathering:ice_sickle").speed(2);
        w("mekanismtools:lapis_lazuli_paxel").speed(0.9);

        // Netherite vanilla: espada 18 y el resto proporcional.
        w("minecraft:netherite_sword").dmg(18);
        group("minecraft:netherite_sword", 18, "minecraft:netherite_axe", "minecraft:netherite_pickaxe",
                "minecraft:netherite_shovel", "minecraft:netherite_hoe");

        // Knightmetal y Steeleaf: al nivel del diamante.
        w("twilightforest:knightmetal_sword").like("minecraft:diamond_sword");
        w("twilightforest:knightmetal_axe").like("minecraft:diamond_axe");
        w("twilightforest:knightmetal_pickaxe").like("minecraft:diamond_pickaxe");
        w("twilightforest:steeleaf_sword").like("minecraft:diamond_sword");
        w("twilightforest:steeleaf_axe").like("minecraft:diamond_axe");
        w("twilightforest:steeleaf_pickaxe").like("minecraft:diamond_pickaxe");
        w("twilightforest:steeleaf_shovel").like("minecraft:diamond_shovel");
        w("twilightforest:steeleaf_hoe").like("minecraft:diamond_hoe");

        // ------------------------------------------- Variant Tools and Weaponry / Basic Weapons
        family("vtaw_mw", "dagger", 5, null, null);
        family("vtaw_mw", "longsword", 5.5, null, 1.8);
        speedFamily("vtaw_mw", "longsword");
        family("vtaw_mw", "katana", 12, 2.0, null);
        family("vtaw_mw", "greatsword", 16, 0.8, null);
        family("vtaw_mw", "battleaxe", 18, null, null);
        family("vtaw_mw", "halberd", 17, null, null);
        family("basicweapons", "glaive", 17, null, null);
        family("basicweapons", "hammer", 10.5, null, 0.8);
        speedFamily("basicweapons", "hammer");
        family("basicweapons", "spear", 9, 1.5, null);
        for (String material : MATERIALS) {
            SPECS.computeIfAbsent(rl("basicweapons:" + material + "_quarterstaff"), k -> new Spec()).speed(1.8).one();
            SPECS.get(rl("basicweapons:" + material + "_quarterstaff")).dex = 25;
            SPECS.computeIfAbsent(rl("vtaw_mw:" + material + "_longsword"), k -> new Spec()).dex = 25;
            SPECS.computeIfAbsent(rl("vtaw_mw:" + material + "_dagger"), k -> new Spec()).dex = 25;
        }

        // ------------------------------------------------------------------- armaduras
        String[] diamond = {"helmet", "chestplate", "leggings", "boots"};
        for (String piece : diamond) {
            for (String set : new String[] {"aether:neptune_", "aether:valkyrie_", "aether:phoenix_",
                    "twilightforest:knightmetal_", "twilightforest:steeleaf_"}) {
                ARMOR_LIKE.put(rl(set + piece), rl("minecraft:diamond_" + piece));
            }
            double defense = switch (piece) {
                case "helmet" -> 6;
                case "chestplate" -> 12;
                case "leggings" -> 9;
                default -> 5;
            };
            for (String set : new String[] {"aether:gravitite_", "minecraft:netherite_"}) {
                ARMOR_FIXED.put(rl(set + piece), new ArmorFixed(defense, 5, 1500, rl("minecraft:diamond_" + piece)));
            }
            ARMOR_SCALE_REF.put(rl("born_in_chaos_v1:dark_metal_armor_" + piece), rl("born_in_chaos_v1:sharpened_dark_metal_sword"));
        }
    }

    public static Spec spec(ResourceLocation id) {
        return SPECS.get(id);
    }

    public static boolean isTwoHanded(ResourceLocation id) {
        Spec spec = SPECS.get(id);
        return spec != null && Boolean.TRUE.equals(spec.twoHanded);
    }
}
