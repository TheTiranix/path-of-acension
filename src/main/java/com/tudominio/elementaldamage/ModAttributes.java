// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Atributos nuevos: esquive generico, esquive de flechas, y resistencia/debilidad por
 * elemento (positivo = % menos daño de ese tipo, negativo = % mas daño de ese tipo).
 * Todos default 0 = sin efecto para cualquier entidad que no sea jugador/raza.
 */
public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ElementalDamage.MODID);

    public static final RegistryObject<Attribute> DODGE_CHANCE = ATTRIBUTES.register(
            "dodge_chance", () -> new RangedAttribute("attribute.name.elementaldamage.dodge_chance", 0.0, -1.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> ARROW_DODGE_CHANCE = ATTRIBUTES.register(
            "arrow_dodge_chance", () -> new RangedAttribute("attribute.name.elementaldamage.arrow_dodge_chance", 0.0, 0.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> RESIST_LIGHT = ATTRIBUTES.register(
            "resist_light", () -> new RangedAttribute("attribute.name.elementaldamage.resist_light", 0.0, -5.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> RESIST_FIRE = ATTRIBUTES.register(
            "resist_fire", () -> new RangedAttribute("attribute.name.elementaldamage.resist_fire", 0.0, -5.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> RESIST_WATER = ATTRIBUTES.register(
            "resist_water", () -> new RangedAttribute("attribute.name.elementaldamage.resist_water", 0.0, -5.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> RESIST_LUNAR = ATTRIBUTES.register(
            "resist_lunar", () -> new RangedAttribute("attribute.name.elementaldamage.resist_lunar", 0.0, -5.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> RESIST_ENDER = ATTRIBUTES.register(
            "resist_ender", () -> new RangedAttribute("attribute.name.elementaldamage.resist_ender", 0.0, -5.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> RESIST_EARTH = ATTRIBUTES.register(
            "resist_earth", () -> new RangedAttribute("attribute.name.elementaldamage.resist_earth", 0.0, -5.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> RESIST_AIR = ATTRIBUTES.register(
            "resist_air", () -> new RangedAttribute("attribute.name.elementaldamage.resist_air", 0.0, -5.0, 1.0).setSyncable(true));

    /** Poder de hechizos lunares (base 1.0, +2.5% = 0.025 en modificadores multiply_base). Todavia no lo usa ningun hechizo:
     *  despues se le asignan los hechizos con tags (pedido de alejandr0). */
    public static final RegistryObject<Attribute> LUNAR_SPELL_POWER = ATTRIBUTES.register(
            "lunar_spell_power", () -> new RangedAttribute("attribute.name.elementaldamage.lunar_spell_power", 1.0, 0.0, 10.0).setSyncable(true));

    /** 1 = el jugador desbloqueo la "absorcion elemental" del arbol de habilidades (funcion del Prisma Convertidor sin el item). */
    public static final RegistryObject<Attribute> ELEMENT_ABSORB = ATTRIBUTES.register(
            "element_absorb", () -> new RangedAttribute("attribute.name.elementaldamage.element_absorb", 0.0, 0.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> CRIT_CHANCE = ATTRIBUTES.register(
            "crit_chance", () -> new RangedAttribute("attribute.name.elementaldamage.crit_chance", 0.0, 0.0, 1.0).setSyncable(true));

    public static final RegistryObject<Attribute> CRIT_DAMAGE = ATTRIBUTES.register(
            "crit_damage", () -> new RangedAttribute("attribute.name.elementaldamage.crit_damage", 1.5, 1.0, 10.0).setSyncable(true));

    public static final RegistryObject<Attribute> BOW_DAMAGE_MULT = ATTRIBUTES.register(
            "bow_damage_mult", () -> new RangedAttribute("attribute.name.elementaldamage.bow_damage_mult", 1.0, 0.0, 10.0).setSyncable(true));

    /** Velocidad de carga de arcos y ballestas: aditiva, 0.10 = carga un 10% mas rapido, -0.20 = un 20% mas lento. */
    public static final RegistryObject<Attribute> DRAW_SPEED = ATTRIBUTES.register(
            "draw_speed", () -> new RangedAttribute("attribute.name.elementaldamage.draw_speed", 0.0, -0.9, 5.0).setSyncable(true));

    /** Hay que asignarle estos atributos nuevos a Player, si no Forge los ignora en esa entidad. */
    public static void addToPlayer(EntityAttributeModificationEvent event) {
        if (!event.getTypes().contains(net.minecraft.world.entity.EntityType.PLAYER)) {
            return;
        }
        event.add(net.minecraft.world.entity.EntityType.PLAYER, DODGE_CHANCE.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ARROW_DODGE_CHANCE.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, RESIST_LIGHT.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, RESIST_FIRE.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, RESIST_WATER.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, RESIST_LUNAR.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, RESIST_ENDER.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, RESIST_EARTH.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, RESIST_AIR.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, LUNAR_SPELL_POWER.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ELEMENT_ABSORB.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, CRIT_CHANCE.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, CRIT_DAMAGE.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, BOW_DAMAGE_MULT.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, DRAW_SPEED.get());
    }
}
