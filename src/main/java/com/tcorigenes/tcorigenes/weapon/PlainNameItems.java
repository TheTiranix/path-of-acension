// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Pedido de alejandr0: items (armas, bastones, sets de armadura) cuyo nombre va sin color (rareza comun + nombre blanco, y sin los
 * codigos de color de su traduccion: ver los lang de kubejs/assets). Ademas de las armas/herramientas comunes, que ya se tratan
 * en ItemStackRarityMixin y PlainWeaponNames.
 */
public final class PlainNameItems {
    private static final Set<String> IDS = Set.of("alexscaves:totem_of_possession", "born_in_chaos_v1:birch_branches", "born_in_chaos_v1:staff_of_magic_arrows", "mutantmonsters:creeper_shard", "born_in_chaos_v1:nut_hammer", "mekanism:meka_tool", "mekanism:atomic_disassembler", "born_in_chaos_v1:pumpkinstaffa", "irons_spellbooks:ice_staff", "irons_spellbooks:lightning_rod", "alexscaves:ortholance", "born_in_chaos_v1:icy_sweetness", "born_in_chaos_v1:spiritual_sword", "mutantmonsters:endersoul_hand", "seadwellers:kelp_shearer", "born_in_chaos_v1:sweet_sword", "born_in_chaos_v1:carrot_sword", "born_in_chaos_v1:dark_ritual_dagger", "born_in_chaos_v1:intoxicating_dagger", "born_in_chaos_v1:soul_cutlass", "born_in_chaos_v1:sweet_axe", "born_in_chaos_v1:trident_hayfork", "born_in_chaos_v1:spider_bite_sword", "born_in_chaos_v1:wood_splitter_axe", "born_in_chaos_v1:skullbreaker_hammer", "born_in_chaos_v1:great_reaper_axe", "born_in_chaos_v1:nightmare_scythe", "panascraftrpgmod:void_sword", "mutantmonsters:hulk_hammer", "cataclysm:ancient_spear", "alexscaves:extinction_spear", "born_in_chaos_v1:soulbane", "cataclysm:gauntlet_of_maelstrom", "cataclysm:gauntlet_of_guard", "cataclysm:gauntlet_of_bulwark", "cataclysm:soul_render", "cataclysm:the_annihilator", "cataclysm:the_immolator", "cataclysm:tidal_claws", "cataclysm:ceraunus", "cataclysm:meat_shredder", "cataclysm:astrape", "cataclysm:the_incinerator", "born_in_chaos_v1:supreme_measure", "alexsmobs:tarantula_hawk_elytra", "alexscaves:cloak_of_darkness", "alexscaves:hood_of_darkness", "born_in_chaos_v1:spiny_shell_armor_helmet", "born_in_chaos_v1:spiny_shell_armor_chestplate", "twilightforest:naga_chestplate", "twilightforest:naga_leggings", "quark:forgotten_hat", "born_in_chaos_v1:damned_demomans_hat_helmet", "born_in_chaos_v1:killer_rabbit_ears_helmet", "born_in_chaos_v1:lord_pumpkinheads_hat_helmet", "born_in_chaos_v1:missionary_hat_helmet", "born_in_chaos_v1:spiritual_guide_sombrero_helmet", "mowziesmobs:geomancer_beads", "mowziesmobs:sol_visage", "mowziesmobs:wrought_helmet", "mutantmonsters:mutant_skeleton_skull", "alexscaves:diving_helmet", "twilightforest:phantom_helmet", "twilightforest:phantom_chestplate", "cataclysm:monstrous_helm", "aether:sentry_boots", "mekanism:free_runners_armored", "mekanism:mekasuit_bodyarmor", "mekanism:mekasuit_boots", "mekanism:mekasuit_helmet", "mekanism:mekasuit_pants");
    private static final String[][] ARMOR_SETS = {{"panascraftrpgmod", "void_armor_"}, {"eeeabsmobs", "ghost_warrior_"}, {"cataclysm", "cursium_"}, {"cataclysm", "ignitium_"}, {"twilightforest", "fiery_"}, {"twilightforest", "yeti_"}, {"aether", "valkyrie_"}, {"aether", "phoenix_"}, {"aether", "neptune_"}, {"aether", "obsidian_"}, {"born_in_chaos_v1", "dark_metal_armor_"}, {"born_in_chaos_v1", "nightmare_mantleofthe_night_"}};
    private static final Set<String> PIECES = Set.of("helmet", "chestplate", "leggings", "boots", "gloves", "elytra_chestplate");

    private PlainNameItems() {
    }

    public static boolean matches(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (IDS.contains(id.toString())) {
            return true;
        }
        for (String[] set : ARMOR_SETS) {
            if (id.getNamespace().equals(set[0]) && id.getPath().startsWith(set[1]) && PIECES.contains(id.getPath().substring(set[1].length()))) {
                return true;
            }
        }
        return false;
    }
}
