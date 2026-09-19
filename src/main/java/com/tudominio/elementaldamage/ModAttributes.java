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
        event.add(net.minecraft.world.entity.EntityType.PLAYER, CRIT_CHANCE.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, CRIT_DAMAGE.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, BOW_DAMAGE_MULT.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, DRAW_SPEED.get());
    }
}
