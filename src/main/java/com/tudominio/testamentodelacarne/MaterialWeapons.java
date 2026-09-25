// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.tcorigenes.tcorigenes.weapon.WeaponDamageOverrides;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Armas nuevas de Zanite, Gravitite, Black Steel, Knightmetal, Steeleaf y Fiery con los tipos de Variant Tools y
 * Basic Weapons que quedaron sin eliminar (pedido de alejandr0). Su daño es proporcional al tipo de arma
 * (el del diamante de ese tipo, ya rebalanceado) segun lo que el material aporta respecto a una espada de
 * diamante; la velocidad es la del tipo. Los valores se resuelven al primer uso (cuando ya estan registrados
 * los items de los otros mods). Los datos de Better Combat, texturas, modelos y recetas van en resources.
 */
public final class MaterialWeapons {
    public record Material(String id, String displayEn, double multiplier, int uses, String ingredient) {
    }

    /** tipo -> item diamante de referencia. */
    public record Type(String id, String diamondRef, String displayEn) {
    }

    public static final List<Material> MATERIALS = List.of(
            // multiplicador = daño de la espada del material / 7 (espada de diamante)
            new Material("zanite", "Zanite", 1.0, 1100, "aether:zanite_gemstone"), // zanite = diamante del mismo tipo
            new Material("gravitite", "Gravitite", 18.0 / 7.0, 1300, "aether:enchanted_gravitite"),
            new Material("black_steel", "Black Steel", 13.0 / 7.0, 1800, "cataclysm:black_steel_ingot"),
            new Material("knightmetal", "Knightmetal", 1.0, 1500, "twilightforest:knightmetal_ingot"),
            new Material("steeleaf", "Steeleaf", 1.0, 1100, "twilightforest:steeleaf_ingot"),
            new Material("fiery", "Fiery", 26.0 / 7.0, 1024, "twilightforest:fiery_ingot"));

    public static final List<Type> TYPES = List.of(
            new Type("dagger", "vtaw_mw:diamond_dagger", "Dagger"),
            new Type("longsword", "vtaw_mw:diamond_longsword", "Saber"),
            new Type("katana", "vtaw_mw:diamond_katana", "Katana"),
            new Type("greatsword", "vtaw_mw:diamond_greatsword", "Greatsword"),
            new Type("battleaxe", "vtaw_mw:diamond_battleaxe", "Battleaxe"),
            new Type("halberd", "vtaw_mw:diamond_halberd", "Halberd"),
            new Type("glaive", "basicweapons:diamond_glaive", "Glaive"),
            new Type("hammer", "basicweapons:diamond_hammer", "Hammer"),
            new Type("quarterstaff", "basicweapons:diamond_quarterstaff", "Quarterstaff"),
            new Type("spear", "basicweapons:diamond_spear", "Spear"));

    public static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    private static final UUID DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    private MaterialWeapons() {
    }

    public static void registerAll(DeferredRegister<Item> items) {
        for (Material material : MATERIALS) {
            ForgeTier tier = new ForgeTier(4, material.uses(), 8.0F, 0.0F, 15, BlockTags.NEEDS_DIAMOND_TOOL,
                    () -> Ingredient.of(ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(material.ingredient()))));
            for (Type type : TYPES) {
                ALL.add(items.register(material.id() + "_" + type.id(),
                        () -> new WeaponItem(tier, material, type, new Item.Properties())));
            }
        }
    }

    public static class WeaponItem extends SwordItem {
        private final Material material;
        private final Type type;
        private Multimap<Attribute, AttributeModifier> attributes;

        public WeaponItem(ForgeTier tier, Material material, Type type, Properties properties) {
            super(tier, 3, -2.4F, properties);
            this.material = material;
            this.type = type;
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            if (slot != EquipmentSlot.MAINHAND) {
                return super.getDefaultAttributeModifiers(slot);
            }
            if (this.attributes == null) {
                ResourceLocation ref = ResourceLocation.tryParse(this.type.diamondRef());
                double damage = WeaponDamageOverrides.roundHalf(WeaponDamageOverrides.finalDamage(ref) * this.material.multiplier());
                double speed = WeaponDamageOverrides.finalSpeed(ref);
                ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
                builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_UUID, "Weapon modifier", damage - 1.0,
                        AttributeModifier.Operation.ADDITION));
                builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(SPEED_UUID, "Weapon modifier", speed - 4.0,
                        AttributeModifier.Operation.ADDITION));
                this.attributes = builder.build();
            }
            return this.attributes;
        }
    }
}
