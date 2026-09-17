package com.tcorigenes.tcorigenes.attributes;

import com.tudominio.elementaldamage.ModAttributes;
import com.tcorigenes.tcorigenes.core.Race;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Bonos base por raza, con los valores exactos que definiste. HUMANO no tiene case: no
 * tiene el favor de nadie, es la raza neutra (sin bonus ni malus).
 *
 * Nota sobre lo que NO esta aca (queda para la fase de Origenes de clase, ver
 * LORE_AND_PROGRESSION.md seccion 7): el 45%+5% real de "atacar puntos debiles" del Hereje,
 * el reflejo de daño del Devoto, la cola/planeo visual de Demonio/Angel, la bateria lunar
 * del Siervo de la Luna, el teletransporte del Ender Warrior, y el anillo de purificacion
 * del Malnacido. Todo eso requiere sistemas nuevos (raycasting, items con NBT propio,
 * habilidades con cooldown) que no son atributos planos.
 */
public class RaceAttributeManager {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("a8a2d7a8-8a2c-4b8a-9a2d-7a8a2d7a8a2c");
    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("b8a2d7a8-8a2c-4b8a-9a2d-7a8a2d7a8a2c");
    private static final UUID ATTACK_DAMAGE_MODIFIER_ID = UUID.fromString("c8a2d7a8-8a2c-4b8a-9a2d-7a8a2d7a8a2c");
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("d8a2d7a8-8a2c-4b8a-9a2d-7a8a2d7a8a2c");
    public static final UUID LUNAR_DAMAGE_MODIFIER_ID = UUID.fromString("e8a2d7a8-8a2c-4b8a-9a2d-7a8a2d7a8a2c");
    public static final UUID LUNAR_PROTECTION_MODIFIER_ID = UUID.fromString("f8a2d7a8-8a2c-4b8a-9a2d-7a8a2d7a8a2c");

    private static final UUID DODGE_MODIFIER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ARROW_DODGE_MODIFIER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID RESIST_LIGHT_MODIFIER_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID RESIST_LIGHT_WEAKNESS_MODIFIER_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID RESIST_FIRE_MODIFIER_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final UUID RESIST_LUNAR_MODIFIER_ID = UUID.fromString("66666666-6666-4666-8666-666666666666");
    private static final UUID RESIST_WATER_WEAKNESS_MODIFIER_ID = UUID.fromString("77777777-7777-4777-8777-777777777777");
    private static final UUID RESIST_ENDER_MODIFIER_ID = UUID.fromString("88888888-8888-4888-8888-888888888888");
    private static final UUID CRIT_CHANCE_MODIFIER_ID = UUID.fromString("99999999-9999-4999-8999-999999999999");

    public static void updateAttributes(Player player, Race race) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        AttributeInstance dodge = player.getAttribute(ModAttributes.DODGE_CHANCE.get());
        AttributeInstance arrowDodge = player.getAttribute(ModAttributes.ARROW_DODGE_CHANCE.get());
        AttributeInstance resistLight = player.getAttribute(ModAttributes.RESIST_LIGHT.get());
        AttributeInstance resistFire = player.getAttribute(ModAttributes.RESIST_FIRE.get());
        AttributeInstance resistWater = player.getAttribute(ModAttributes.RESIST_WATER.get());
        AttributeInstance resistLunar = player.getAttribute(ModAttributes.RESIST_LUNAR.get());
        AttributeInstance resistEnder = player.getAttribute(ModAttributes.RESIST_ENDER.get());
        AttributeInstance critChance = player.getAttribute(ModAttributes.CRIT_CHANCE.get());

        // Limpieza: sacamos todos los modifiers de raza antes de aplicar los de la nueva.
        removeIfPresent(health, HEALTH_MODIFIER_ID);
        removeIfPresent(speed, SPEED_MODIFIER_ID);
        removeIfPresent(attackDamage, ATTACK_DAMAGE_MODIFIER_ID);
        removeIfPresent(attackSpeed, ATTACK_SPEED_MODIFIER_ID);
        removeIfPresent(dodge, DODGE_MODIFIER_ID);
        removeIfPresent(arrowDodge, ARROW_DODGE_MODIFIER_ID);
        removeIfPresent(resistLight, RESIST_LIGHT_MODIFIER_ID);
        removeIfPresent(resistLight, RESIST_LIGHT_WEAKNESS_MODIFIER_ID);
        removeIfPresent(resistFire, RESIST_FIRE_MODIFIER_ID);
        removeIfPresent(resistLunar, RESIST_LUNAR_MODIFIER_ID);
        removeIfPresent(resistWater, RESIST_WATER_WEAKNESS_MODIFIER_ID);
        removeIfPresent(resistEnder, RESIST_ENDER_MODIFIER_ID);
        removeIfPresent(critChance, CRIT_CHANCE_MODIFIER_ID);

        List<Runnable> toApply = new ArrayList<>();

        switch (race) {
            case HEREJE -> {
                // +3% esquive, +5% velocidad, +5% velocidad de ataque. El 45%+5% real por golpear
                // puntos debiles y el tope de favor en 0 se manejan en ModEvents.
                add(toApply, dodge, DODGE_MODIFIER_ID, "Hereje Dodge", 0.03, AttributeModifier.Operation.ADDITION);
                add(toApply, speed, SPEED_MODIFIER_ID, "Hereje Speed", 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
                add(toApply, attackSpeed, ATTACK_SPEED_MODIFIER_ID, "Hereje Attack Speed", 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            case DEVOTO -> {
                // 10% mas regen/curacion (aplicado en ModEvents#onLivingHeal sobre TODA curacion,
                // incluye robo de vida/pociones porque ese evento intercepta cualquier heal) y
                // 3% de reflejo de daño (fase 2: necesita un LivingHurtEvent que devuelva daño al atacante).
            }
            case DEMONIO -> {
                // Inmune a fuego/lava/magma (ModEvents), +10% daño, +15% resistencia a fuego elemental.
                // -40% resistencia a luz: no esta en el documento, es un agregado de balance a proposito
                // (confirmado explicitamente, mantener). El +10% extra al PEGAR con fuego elemental
                // esta en ModEvents (necesita leer el tipo de daño del golpe, no es un atributo plano).
                add(toApply, attackDamage, ATTACK_DAMAGE_MODIFIER_ID, "Demonio Damage", 0.10, AttributeModifier.Operation.MULTIPLY_TOTAL);
                add(toApply, resistLight, RESIST_LIGHT_WEAKNESS_MODIFIER_ID, "Demonio Light Weakness", -0.40, AttributeModifier.Operation.ADDITION);
                add(toApply, resistFire, RESIST_FIRE_MODIFIER_ID, "Demonio Fire Resistance", 0.15, AttributeModifier.Operation.ADDITION);
            }
            case ANGEL -> {
                // Inmune a caida + planeo (ModEvents/fase 2 para el planeo), +10% daño de luz, +10% resistencia a luz.
                add(toApply, resistLight, RESIST_LIGHT_MODIFIER_ID, "Angel Light Resistance", 0.10, AttributeModifier.Operation.ADDITION);
                // El "+10% daño elemental de luz infligido" no es un atributo de defensa: se aplica
                // multiplicando el daño cuando EL ANGEL es quien ataca con luz (fase 2, en el item/hechizo de luz).
            }
            case SIERVO_DE_LA_LUNA -> {
                // +10% proteccion y +10% daño de noche (dinamico, ModEvents), 15% resistencia lunar.
                // -20% resistencia a luz: agregado de balance a proposito (confirmado, mantener),
                // no esta en el documento. La bateria de energia lunar es un item nuevo: fase 2.
                add(toApply, resistLight, RESIST_LIGHT_WEAKNESS_MODIFIER_ID, "Siervo Luna Light Weakness", -0.20, AttributeModifier.Operation.ADDITION);
                add(toApply, resistLunar, RESIST_LUNAR_MODIFIER_ID, "Siervo Luna Lunar Resistance", 0.15, AttributeModifier.Operation.ADDITION);
            }
            case ENDER_WARRIOR -> {
                // +15% vida, 10% esquive de flechas, +15% resistencia ender. -20% resistencia a agua
                // elemental: agregado de balance a proposito (confirmado, mantener), no esta en el
                // documento. El daño por tocar agua (como Enderman) esta en ModEvents#onPlayerTick.
                // 0.5 bloques mas alto: no implementado, requeriria overridear el hitbox del jugador
                // (mixin de riesgo/beneficio dudoso dado lo visto con relics_in_chaos).
                add(toApply, health, HEALTH_MODIFIER_ID, "Ender Warrior Health", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
                add(toApply, arrowDodge, ARROW_DODGE_MODIFIER_ID, "Ender Warrior Arrow Dodge", 0.10, AttributeModifier.Operation.ADDITION);
                add(toApply, resistWater, RESIST_WATER_WEAKNESS_MODIFIER_ID, "Ender Warrior Water Weakness", -0.20, AttributeModifier.Operation.ADDITION);
                add(toApply, resistEnder, RESIST_ENDER_MODIFIER_ID, "Ender Warrior Ender Resistance", 0.15, AttributeModifier.Operation.ADDITION);
            }
            case MALNACIDO -> {
                if (player.getPersistentData().getBoolean("malnacido_purificado")) {
                    // Anillo de Purificacion usado: se van los debuffs, +15% daño, +50% chance de
                    // critico, Regen II permanente (la Regen se re-aplica como MobEffectInstance en
                    // login/respawn, ver ModEvents).
                    add(toApply, attackDamage, ATTACK_DAMAGE_MODIFIER_ID, "Malnacido Purificado Damage", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
                    add(toApply, critChance, CRIT_CHANCE_MODIFIER_ID, "Malnacido Purificado Crit Chance", 0.50, AttributeModifier.Operation.ADDITION);
                } else {
                    // -20% velocidad de ataque (manos deformes), -15% velocidad, -35% chance de acertar
                    // (ver ModEvents#onAttackEntity), +10% vida, inmune a veneno/daño instantaneo/wither
                    // (ModEvents). Odio de facciones y tope de favor en 0: requieren sistemas que no existen
                    // todavia (reputacion, favor).
                    add(toApply, attackSpeed, ATTACK_SPEED_MODIFIER_ID, "Malnacido Attack Speed", -0.20, AttributeModifier.Operation.MULTIPLY_TOTAL);
                    add(toApply, speed, SPEED_MODIFIER_ID, "Malnacido Slowness", -0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
                    add(toApply, health, HEALTH_MODIFIER_ID, "Malnacido Health", 0.10, AttributeModifier.Operation.MULTIPLY_TOTAL);
                }
            }
            default -> {
                // HUMANO: sin bonus ni malus.
            }
        }

        toApply.forEach(Runnable::run);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Regeneracion II permanente para el Malnacido purificado. Se re-llama en login/respawn
     *  porque los MobEffectInstance (a diferencia de los atributos) no persisten solos. */
    public static void applyPurifiedEffects(Player player) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, Integer.MAX_VALUE, 1, true, false, false));
    }

    private static void removeIfPresent(AttributeInstance instance, UUID id) {
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static void add(List<Runnable> queue, AttributeInstance instance, UUID id, String name, double amount, AttributeModifier.Operation operation) {
        if (instance != null) {
            queue.add(() -> instance.addTransientModifier(new AttributeModifier(id, name, amount, operation)));
        }
    }
}
