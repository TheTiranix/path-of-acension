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
        /** Linea extra del tooltip (ej. "Prende fuego"). */
        public String note;
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

        public Spec note(String text) {
            this.note = text;
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

    /**
     * Armaduras con proteccion, tenacidad y durabilidad propias (ref = pieza de diamante para reutilizar sus UUID).
     * toughness NaN = no se toca la tenacidad original; durability 0 = irrompible; durability < 0 = no se toca.
     */
    public record ArmorFixed(double defense, double toughness, int durability, ResourceLocation uuidRef) {
    }

    public static final Map<ResourceLocation, ArmorFixed> ARMOR_FIXED = new HashMap<>();
    /** Armaduras que copian la durabilidad de otra pieza (knightmetal = diamante). */
    public static final Map<ResourceLocation, ResourceLocation> ARMOR_DURABILITY_LIKE = new HashMap<>();
    /** Armaduras irrompibles e imposibles de sacar por mobs (ver ArmorGuard). */
    public static final java.util.Set<ResourceLocation> GUARDED_ARMOR = new java.util.HashSet<>();

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

    /** Durabilidad de cada pieza de gravitite / netherite / valkyrie / phoenix: el ancla del resto. */
    public static final int GRAVITITE_DURABILITY = 1500;
    /**
     * Durabilidad MINIMA por item (pedido de alejandr0: revisar las que quedaban por debajo de su nivel). Solo sube: si el item
     * ya tenia mas usos se queda como esta, y los items que no se rompen (0 usos) no se tocan. Ver ArmorDurability.
     */
    public static final Map<ResourceLocation, Integer> DURABILITY_MIN = new HashMap<>();

    private static void minDur(int uses, String... ids) {
        for (String id : ids) {
            DURABILITY_MIN.merge(rl(id), uses, Math::max);
        }
    }

    private static final String[] TOOLS5 = {"sword", "axe", "pickaxe", "shovel", "hoe"};
    private static final String[] ARMOR4 = {"helmet", "chestplate", "leggings", "boots"};

    /** Espada, hacha, pico, pala y azada de un set: prefix + tipo (ej. "iceandfire:dragonsteel_fire_" + "sword"). */
    private static void minDurTools(int uses, String prefix) {
        for (String tool : TOOLS5) {
            minDur(uses, prefix + tool);
        }
    }

    private static void minDurArmor(int uses, String prefix) {
        for (String piece : ARMOR4) {
            minDur(uses, prefix + piece);
        }
    }

    /** Armaduras de ARMOR_FIXED cuya durabilidad se fija EXACTA (puede bajar la original); el resto solo sube. */
    public static final java.util.Set<ResourceLocation> EXACT_DURABILITY = new java.util.HashSet<>();

    /** Durabilidad de herramientas (no armaduras) que se fija a mano: id -> usos. Ver ArmorDurability. */
    public static final Map<ResourceLocation, Integer> TOOL_DURABILITY = new HashMap<>();

    /**
     * Durabilidad proporcional a la proteccion: la armadura de gravitite (defensa de esa pieza + 4 de tenacidad) vale
     * GRAVITITE_DURABILITY y las demas escalan segun (defensa + tenacidad) respecto de ella. ArmorDurability nunca la
     * deja por debajo de la que la armadura ya tenia.
     */
    /** Proteccion de la pieza de diamante (3 / 8 / 6 / 3). */
    static double diamondDefenseOf(String piece) {
        return switch (piece) {
            case "helmet" -> 3;
            case "chestplate" -> 8;
            case "leggings" -> 6;
            default -> 3;
        };
    }

    static int scaledDurability(double defense, double toughness, double gravititeDefense) {
        return (int) Math.round(GRAVITITE_DURABILITY * (defense + toughness) / (gravititeDefense + 4.0));
    }

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
        for (String tool : new String[] {"sword", "axe", "pickaxe", "shovel", "hoe"}) {
            w("aether:zanite_" + tool).like("minecraft:diamond_" + tool); // zanite = diamante del mismo tipo
        }
        w("aether:holy_sword").dmg(6).el(LIGHT, 8);
        w("aether:lightning_sword").dmg(8).el(AIR, 2).el(LIGHT, 4);
        w("aether:valkyrie_lance").dmg(9).speed(1.5).reach(3.5).two().el(LIGHT, 3);
        w("aether:hammer_of_kingbdogz").dmg(9).el(LIGHT, 3);
        w("aether:pig_slayer").dmg(10).el(LIGHT, 2).el(EARTH, 2);
        w("aether:flaming_sword").dmg(9).el(LIGHT, 3);
        w("aether:vampire_blade").dmg(10).el(LIGHT, 4);

        // ------------------------------------------------------------- Celestisynth
        // Wrath of the Desert: 3 torbellinos por uso, cada uno con su elemento fijo.
        w("panascraftrpgmod:the_king_of_the_abyss_sword").dmg(50000);
        group("panascraftrpgmod:the_king_of_the_abyss_sword", 50000, "panascraftrpgmod:the_king_of_the_abyss_axe",
                "panascraftrpgmod:the_king_of_the_abyss_pickaxe", "panascraftrpgmod:the_king_of_the_abyss_shovel",
                "panascraftrpgmod:the_king_of_the_abyss_hoe");
        w("cataclysm:cursed_bow").dmg(15000).shot();
        // Arcos (pedido de alejandr0): el daño es por impacto; el arco vanilla hace 9 por flecha.
        w("iceandfire:dragonbone_bow").dmg(12).shot();
        w("burnt:flint_bow").dmg(11).shot().note("Prende fuego");
        w("twilightforest:ice_bow").dmg(15).shot().el(ICE, 3);
        w("twilightforest:ender_bow").dmg(15).shot().el(ENDER, 3);
        w("twilightforest:seeker_bow").dmg(15).shot().el(NATURAL, 3);
        w("aether:phoenix_bow").dmg(12).shot().el(FIRE, 3);
        w("ars_nouveau:spell_bow").dmg(12).shot();
        w("ars_nouveau:spell_crossbow").dmg(12).shot();
        w("alexscaves:dreadbow").dmg(100).shot().el(LUNAR, 50).note("Genera una lluvia de flechas");
        w("cataclysm:gauntlet_of_maelstrom").dmg(300).el(ENDER, 200);
        w("mowziesmobs:axe_of_a_thousand_metals").two();
        w("cataclysm:wrath_of_the_desert").dmg(25000).perProjectile().cycle(LUNAR, N, AIR);
        w("celestisynth:rainfall_serenity").dmg(120000).shot().cycle(AIR, LIGHT, N, N);
        w("celestisynth:aquaflora").dmg(180000).speed(3).cycle(NATURAL, WATER, N, N);
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
        w("mowziesmobs:earthrend_gauntlet").dmg(300).el(ENDER, 200);
        w("cataclysm:gauntlet_of_bulwark").dmg(300).el(ENDER, 200);
        w("cataclysm:gauntlet_of_guard").dmg(300).el(ENDER, 200);
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
        // Mutants Buff: Charged Hammer y Flame Burst Hammer (upgraded_hulk_hammer): 45 de daño y 20 de aire.
        w("mutantsbuff:charged_hammer").dmg(45).el(AIR, 20);
        w("mutantsbuff:upgraded_hulk_hammer").dmg(45).el(AIR, 20);
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

        // Osmium (Mekanism Tools): espada de 6.5 y el resto proporcional.
        w("mekanismtools:osmium_sword").dmg(6.5);
        group("mekanismtools:osmium_sword", 6.5, "mekanismtools:osmium_axe", "mekanismtools:osmium_pickaxe",
                "mekanismtools:osmium_shovel", "mekanismtools:osmium_hoe", "mekanismtools:osmium_paxel");

        // Netherite vanilla: espada 18 y el resto proporcional.
        w("minecraft:netherite_sword").dmg(18);
        group("minecraft:netherite_sword", 18, "minecraft:netherite_axe", "minecraft:netherite_pickaxe",
                "minecraft:netherite_shovel", "minecraft:netherite_hoe");

        // Fiery (Twilight Forest): sus herramientas tienen 1000 usos mas que las de netherite (2031 + 1000).
        TOOL_DURABILITY.put(rl("twilightforest:fiery_sword"), 3031);
        TOOL_DURABILITY.put(rl("twilightforest:fiery_pickaxe"), 3031);

        // ------------------------------------------------------ durabilidades minimas por nivel (ver DURABILITY_MIN)
        // Ice and Fire: la plata queda sobre el hierro, el myrmex al nivel del diamante, el hueso de dragon y el dragonsteel muy por
        // encima (dragonsteel = diamante + 10000, igual que su armadura).
        minDurTools(1200, "iceandfire:silver_");
        minDurTools(1561, "iceandfire:myrmex_desert_");
        minDurTools(1561, "iceandfire:myrmex_jungle_");
        minDur(1561, "iceandfire:myrmex_desert_sword_venom", "iceandfire:myrmex_jungle_sword_venom");
        minDurTools(3000, "iceandfire:dragonbone_");
        minDur(3000, "iceandfire:dragonbone_sword_fire", "iceandfire:dragonbone_sword_ice", "iceandfire:dragonbone_sword_lightning",
                "iceandfire:dragonbone_bow");
        for (String element : new String[] {"fire", "ice", "lightning"}) {
            minDurTools(1561 + 10000, "iceandfire:dragonsteel_" + element + "_");
        }
        for (String element : new String[] {"fire", "ice", "lightning"}) {
            String swordId = "iceandfire:dragonsteel_" + element + "_sword";
            w(swordId).dmg(50);
            group(swordId, 50, "iceandfire:dragonsteel_" + element + "_axe", "iceandfire:dragonsteel_" + element + "_pickaxe",
                    "iceandfire:dragonsteel_" + element + "_shovel", "iceandfire:dragonsteel_" + element + "_hoe");
        }
        minDur(2500, "iceandfire:dread_sword", "iceandfire:dread_knight_sword", "iceandfire:dread_queen_sword",
                "iceandfire:tide_trident", "iceandfire:troll_weapon_axe", "iceandfire:troll_weapon_hammer");
        minDur(2000, "iceandfire:ghost_sword", "iceandfire:amphithere_macuahuitl");
        minDur(1800, "iceandfire:hippogryph_sword", "iceandfire:stymphalian_bird_dagger");
        for (String color : new String[] {"blue", "bronze", "deepblue", "green", "purple", "red", "teal"}) {
            minDurArmor(1000, "iceandfire:tide_" + color + "_"); // armadura de serpiente marina: al nivel del diamante
        }

        // Panascraft: todo con durabilidad altisima.
        minDurTools(20000, "panascraftrpgmod:the_king_of_the_abyss_");
        minDurArmor(20000, "panascraftrpgmod:the_king_of_the_abyss_armor_");
        minDurArmor(20000, "panascraftrpgmod:upgraded_armor_");
        minDurArmor(20000, "panascraftrpgmod:void_armor_");
        minDur(20000, "panascraftrpgmod:void_sword");

        // Born in Chaos: espadas especiales sobre el dark metal (1800) y los jefes todavia mas arriba.
        minDur(2500, "born_in_chaos_v1:spider_bite_sword", "born_in_chaos_v1:soul_cutlass", "born_in_chaos_v1:spiritual_sword",
                "born_in_chaos_v1:darkwarblade", "born_in_chaos_v1:dark_ritual_dagger", "born_in_chaos_v1:intoxicating_dagger",
                "born_in_chaos_v1:nightmare_claw", "born_in_chaos_v1:sweet_sword", "born_in_chaos_v1:shell_mace");
        minDur(1800, "born_in_chaos_v1:sharpened_dark_metal_sword");
        minDur(3000, "born_in_chaos_v1:nightmare_scythe", "born_in_chaos_v1:frostbitten_blade", "born_in_chaos_v1:great_reaper_axe",
                "born_in_chaos_v1:skullbreaker_hammer", "born_in_chaos_v1:stop_hammer");
        minDur(4000, "born_in_chaos_v1:soulbane");

        // Aether: zanite = diamante, gravitite y valkyrie = netherite, las espadas especiales por encima.
        minDurTools(1561, "aether:zanite_");
        minDurTools(2031, "aether:gravitite_");
        minDur(2031, "aether:valkyrie_axe", "aether:valkyrie_pickaxe", "aether:valkyrie_shovel", "aether:valkyrie_hoe");
        minDur(2500, "aether:lightning_sword", "aether:holy_sword", "aether:vampire_blade", "aether:flaming_sword",
                "aether:pig_slayer", "aether:hammer_of_kingbdogz", "aether:hammer_of_jeb", "aether:valkyrie_lance");
        minDur(2000, "aether:phoenix_bow");

        // Natures Aura: infused iron (sobre el hierro) < sky < depth (arma de 14 de daño).
        minDurTools(1000, "naturesaura:infused_iron_");
        minDurTools(1500, "naturesaura:sky_");
        minDurTools(2500, "naturesaura:depth_");
        minDur(800, "naturesaura:infused_iron_helmet", "naturesaura:infused_iron_shoes");
        minDur(1000, "naturesaura:sky_helmet", "naturesaura:sky_shoes");
        minDur(1500, "naturesaura:depth_helmet", "naturesaura:depth_shoes");

        // Sculk Horde: armas de jefe; diascite al nivel del dark metal (1800); ferriscite un poco menos.
        minDur(2500, "sculkhorde:sculk_sweeper_sword");
        minDur(3000, "sculkhorde:sculk_enderman_cleaver");
        minDur(4000, "sculkhorde:blade_of_purity");
        minDur(1800, "sculkhorde:diascite_axe", "sculkhorde:diascite_hoe", "sculkhorde:diascite_pickaxe", "sculkhorde:diascite_shovel");
        minDur(1000, "sculkhorde:ferriscite_axe", "sculkhorde:ferriscite_hoe", "sculkhorde:ferriscite_pickaxe",
                "sculkhorde:ferriscite_shovel");

        // Cataclysm (armas de dos manos y gauntlets), Alexs Caves, Mowzies, Scary Mobs y EEEABs Mobs: armas de jefe.
        minDur(8000, "cataclysm:meat_shredder", "cataclysm:soul_render", "cataclysm:zweiender");
        minDur(5000, "cataclysm:gauntlet_of_bulwark", "cataclysm:gauntlet_of_guard", "cataclysm:gauntlet_of_maelstrom",
                "mowziesmobs:earthrend_gauntlet", "alexscaves:galena_gauntlet");
        minDur(6000, "alexscaves:extinction_spear", "mowziesmobs:axe_of_a_thousand_metals");
        minDur(5000, "eeeabsmobs:guardian_axe");
        minDur(4000, "scary_mobs:mallet");
        minDur(3000, "eeeabsmobs:immortal_sword", "eeeabsmobs:immortal_axe", "eeeabsmobs:netherworld_katana");
        // Irons Spellbooks: espadas
        minDur(3000, "irons_spellbooks:amethyst_rapier", "irons_spellbooks:claymore", "irons_spellbooks:spellbreaker",
                "irons_spellbooks:dreadsword", "irons_spellbooks:template_large_sword");

        // Espada de diascite (nuestra): como la de dark metal (10 de daño).
        w("testamentodelacarne:diascite_sword").dmg(10);

        minDur(5000, "mutantsbuff:charged_hammer", "mutantsbuff:upgraded_hulk_hammer");
        minDur(4000, "mutantmonsters:hulk_hammer");

        // Herramientas de zafiro (nuestras): al nivel del diamante.
        for (String tool : new String[] {"sword", "axe", "pickaxe", "shovel", "hoe"}) {
            w("testamentodelacarne:sapphire_" + tool).like("minecraft:diamond_" + tool);
        }

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
            for (String set : new String[] {"twilightforest:knightmetal_", "twilightforest:steeleaf_"}) {
                ARMOR_LIKE.put(rl(set + piece), rl("minecraft:diamond_" + piece));
            }
            double defense = switch (piece) {
                case "helmet" -> 6;
                case "chestplate" -> 11;
                case "leggings" -> 9;
                default -> 6;
            };
            for (String set : new String[] {"aether:gravitite_", "minecraft:netherite_"}) {
                ARMOR_FIXED.put(rl(set + piece), new ArmorFixed(defense, 4, GRAVITITE_DURABILITY, rl("minecraft:diamond_" + piece)));
            }
            // Valkyrie y Phoenix: +3 de proteccion por pieza sobre lo que tenian (como el diamante) y 4 de tenacidad, igual que
            // gravitite; el bonus de set completo (+20% al daño de luz / fuego) esta en ArmorSetBonus.
            for (String set : new String[] {"aether:valkyrie_", "aether:phoenix_"}) {
                ARMOR_FIXED.put(rl(set + piece), new ArmorFixed(defense, 4, scaledDurability(defense, 4, defense), rl("minecraft:diamond_" + piece)));
            }
            // Neptune (Aether): 5 casco y botas, 10 pechera, 8 pantalones, 3 de tenacidad por pieza.
            double neptuneDefense = switch (piece) {
                case "helmet" -> 5;
                case "chestplate" -> 10;
                case "leggings" -> 8;
                default -> 5;
            };
            ARMOR_FIXED.put(rl("aether:neptune_" + piece), new ArmorFixed(neptuneDefense, 3, scaledDurability(neptuneDefense, 3, defense), rl("minecraft:diamond_" + piece)));
            // Yeti (Twilight Forest): como gravitite (6/11/9/6) con 5 de tenacidad; 10% de resistencia al retroceso por
            // pieza e inmunidad al frio con el set completo (ver YetiArmor y ClimateImmunity).
            ARMOR_FIXED.put(rl("twilightforest:yeti_" + piece), new ArmorFixed(defense, 5, scaledDurability(defense, 5, defense), rl("minecraft:diamond_" + piece)));
            // Fiery (Twilight Forest): como la de gravitite pero 2 mas de proteccion por pieza y 1000 usos mas (2500)
            ARMOR_FIXED.put(rl("twilightforest:fiery_" + piece), new ArmorFixed(diamondDefenseOf(piece) * 2.5, 5, GRAVITITE_DURABILITY + 1000, rl("minecraft:diamond_" + piece)));
            EXACT_DURABILITY.add(rl("twilightforest:fiery_" + piece));
            // Dragon scale armor (Ice and Fire, todos los colores): iguales a las de fiery (2.5 veces el diamante, 5 de tenacidad,
            // 2500 usos).
            double dragonDefense = diamondDefenseOf(piece) * 2.5;
            for (String color : new String[] {"amythest", "black", "blue", "bronze", "copper", "electric", "gray", "green",
                    "red", "sapphire", "silver", "white"}) {
                ARMOR_FIXED.put(rl("iceandfire:armor_" + color + "_" + piece),
                        new ArmorFixed(dragonDefense, 5, GRAVITITE_DURABILITY + 1000, rl("minecraft:diamond_" + piece)));
                EXACT_DURABILITY.add(rl("iceandfire:armor_" + color + "_" + piece));
            }
            // Armaduras "como" otra (diamante / netherite): tambien igualan su durabilidad (nunca la bajan, ver ArmorDurability)
            for (String set : new String[] {"twilightforest:steeleaf_", "seadwellers:depth_"}) {
                ARMOR_DURABILITY_LIKE.put(rl(set + piece), rl("minecraft:diamond_" + piece));
            }
            ARMOR_DURABILITY_LIKE.put(rl("alexscaves:diving_" + piece), rl("minecraft:netherite_" + piece));
            // Knightmetal: igual que el diamante en proteccion (ARMOR_LIKE) y en durabilidad
            ARMOR_DURABILITY_LIKE.put(rl("twilightforest:knightmetal_" + piece), rl("minecraft:diamond_" + piece));
            // Solar Crystal y Lunar Stone (Celestisynth): 2500 veces la proteccion del diamante, irrompibles y sin robo
            double diamondDefense = switch (piece) {
                case "helmet" -> 3;
                case "chestplate" -> 8;
                case "leggings" -> 6;
                default -> 3;
            };
            for (String set : new String[] {"celestisynth:solar_crystal_", "celestisynth:lunar_stone_"}) {
                ARMOR_FIXED.put(rl(set + piece), new ArmorFixed(diamondDefense * 2500.0, Double.NaN, 0, rl("minecraft:diamond_" + piece)));
                GUARDED_ARMOR.add(rl(set + piece));
            }
            // Depth (Sea Dwellers): al nivel del diamante.
            ARMOR_LIKE.put(rl("seadwellers:depth_" + piece), rl("minecraft:diamond_" + piece));
            // Diving Armor (Alex's Caves): al nivel del netherite (proteccion y dureza).
            ARMOR_LIKE.put(rl("alexscaves:diving_" + (piece.equals("chestplate") ? "chestplate" : piece)), rl("minecraft:netherite_" + piece));
            // Dragonsteel (Ice and Fire, fuego/hielo/rayo): 6 veces la proteccion y la durabilidad del diamante (tenacidad sin tocar).
            double dragonsteelDefense = diamondDefenseOf(piece) * 6.0;
            int diamondPieceDurability = switch (piece) {
                case "helmet" -> 363;
                case "chestplate" -> 528;
                case "leggings" -> 495;
                default -> 429;
            };
            for (String element : new String[] {"fire", "ice", "lightning"}) {
                ARMOR_FIXED.put(rl("iceandfire:dragonsteel_" + element + "_" + piece),
                        new ArmorFixed(dragonsteelDefense, Double.NaN, diamondPieceDurability * 6, rl("minecraft:diamond_" + piece)));
                EXACT_DURABILITY.add(rl("iceandfire:dragonsteel_" + element + "_" + piece));
            }
            // Cursium e Ignitium (Cataclysm): 30 veces la proteccion del diamante y 20 de tenacidad por pieza (ignitium tambien en
            // su version con elytra).
            double cataclysmDefense = diamondDefenseOf(piece) * 30.0;
            ARMOR_FIXED.put(rl("cataclysm:cursium_" + piece),
                    new ArmorFixed(cataclysmDefense, 20, scaledDurability(cataclysmDefense, 20, defense), rl("minecraft:diamond_" + piece)));
            ARMOR_FIXED.put(rl("cataclysm:ignitium_" + piece),
                    new ArmorFixed(cataclysmDefense, 20, scaledDurability(cataclysmDefense, 20, defense), rl("minecraft:diamond_" + piece)));
            if (piece.equals("chestplate")) {
                ARMOR_FIXED.put(rl("cataclysm:ignitium_elytra_chestplate"),
                        new ArmorFixed(cataclysmDefense, 20, scaledDurability(cataclysmDefense, 20, defense), rl("minecraft:diamond_chestplate")));
            }
            // Ghost Warrior (EEEAB's Mobs): iguales a las de Skymetal e Ignitium (ademas son inmunes al wither, ver ClimateImmunity).
            ARMOR_FIXED.put(rl("eeeabsmobs:ghost_warrior_" + piece),
                    new ArmorFixed(cataclysmDefense, 20, scaledDurability(cataclysmDefense, 20, defense), rl("minecraft:diamond_" + piece)));
            // Osmium (Mekanism Tools): 2.5 casco y botas, 6 pechera, 5 pantalones, 1 de tenacidad; durabilidad proporcional (exacta).
            double osmiumDefense = switch (piece) {
                case "helmet" -> 2.5;
                case "chestplate" -> 6;
                case "leggings" -> 5;
                default -> 2.5;
            };
            ARMOR_FIXED.put(rl("mekanismtools:osmium_" + piece),
                    new ArmorFixed(osmiumDefense, 1, scaledDurability(osmiumDefense, 1, defense), rl("minecraft:diamond_" + piece)));
            EXACT_DURABILITY.add(rl("mekanismtools:osmium_" + piece));
            // Dark Metal (Born in Chaos): valores fijos propios, sin tocar tenacidad ni durabilidad.
            double darkMetalDefense = switch (piece) {
                case "helmet" -> 4;
                case "chestplate" -> 9;
                case "leggings" -> 7;
                default -> 4;
            };
            ARMOR_FIXED.put(rl("born_in_chaos_v1:dark_metal_armor_" + piece), new ArmorFixed(darkMetalDefense, 3, scaledDurability(darkMetalDefense, 3, defense), rl("minecraft:diamond_" + piece)));
            // Diascite (nuestra, Sculk Horde): igual que el dark metal.
            ARMOR_FIXED.put(rl("testamentodelacarne:diascite_" + piece), new ArmorFixed(darkMetalDefense, 3, scaledDurability(darkMetalDefense, 3, defense), rl("minecraft:diamond_" + piece)));
            // Armadura de zafiro (nuestra, ver SapphireArmorMaterial): 3.5 casco y botas, 8.5 pechera, 6.5 pantalones, 3 de tenacidad.
            double sapphireDefense = switch (piece) {
                case "helmet" -> 3.5;
                case "chestplate" -> 8.5;
                case "leggings" -> 6.5;
                default -> 3.5;
            };
            ARMOR_FIXED.put(rl("testamentodelacarne:sapphire_" + piece), new ArmorFixed(sapphireDefense, 3, -1, rl("minecraft:diamond_" + piece)));
            // Netherite Battlemage (Iron's Spellbooks, netherite_mage_*): valores fijos propios + 4 de tenacidad de armadura.
            // La Battlemage normal de Ars Nouveau NO se toca: usa los valores por defecto del mod.
            double battlemageDefense = switch (piece) {
                case "helmet" -> 5.5;
                case "chestplate" -> 10.5;
                case "leggings" -> 8.5;
                default -> 5.5;
            };
            ARMOR_FIXED.put(rl("irons_spellbooks:netherite_mage_" + piece), new ArmorFixed(battlemageDefense, 4, scaledDurability(battlemageDefense, 4, defense), rl("minecraft:diamond_" + piece)));
            // Myrmex (Ice and Fire): valores fijos propios, ambas variantes (desierto y jungla).
            double myrmexDefense = switch (piece) {
                case "helmet" -> 2.5;
                case "chestplate" -> 7.5;
                case "leggings" -> 5.5;
                default -> 2.5;
            };
            for (String variant : new String[] {"desert", "jungle"}) {
                ARMOR_FIXED.put(rl("iceandfire:myrmex_" + variant + "_" + piece), new ArmorFixed(myrmexDefense, Double.NaN, scaledDurability(myrmexDefense, 0, defense), rl("minecraft:diamond_" + piece)));
            }
        }
        // Chitin (Death Worm) y Troll Leather (Ice and Fire): pantalones a 5, botas a 2 (ambas variantes de cada uno).
        for (String color : new String[] {"yellow", "white", "red"}) {
            ARMOR_FIXED.put(rl("iceandfire:deathworm_" + color + "_leggings"), new ArmorFixed(5, Double.NaN, scaledDurability(5, 0, 9), rl("minecraft:diamond_leggings")));
            ARMOR_FIXED.put(rl("iceandfire:deathworm_" + color + "_boots"), new ArmorFixed(2, Double.NaN, scaledDurability(2, 0, 6), rl("minecraft:diamond_boots")));
        }
        for (String variant : new String[] {"forest_troll", "frost_troll", "mountain_troll"}) {
            ARMOR_FIXED.put(rl("iceandfire:" + variant + "_leather_leggings"), new ArmorFixed(5, Double.NaN, scaledDurability(5, 0, 9), rl("minecraft:diamond_leggings")));
            ARMOR_FIXED.put(rl("iceandfire:" + variant + "_leather_boots"), new ArmorFixed(2, Double.NaN, scaledDurability(2, 0, 6), rl("minecraft:diamond_boots")));
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
