package com.tcorigenes.tcorigenes.playerclass;

import com.tudominio.elementaldamage.ModAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Bonos pasivos por clase. Simplificaciones conscientes (documentadas para retomar despues):
 * - Ritualista: NO restringe todavia el uso de armas pesadas (necesitaria interceptar equip/attack).
 * - Guerrero Anima: NO restringe todavia a solo la espada anima ni le da la espada al elegir clase.
 * - Ninguna clase tiene todavia el "Matrimonio de Carne" (ritual) ni el anillo de purificacion.
 * Todo lo que SI es un atributo plano (daño, vida, velocidad, critico, resistencia) esta aplicado.
 */
public class ClassAttributeManager {
    private static final UUID HEALTH_ID = UUID.fromString("c1a55000-0001-4001-8001-000000000001");
    private static final UUID ARMOR_ID = UUID.fromString("c1a55000-0002-4002-8002-000000000002");
    private static final UUID ATTACK_DAMAGE_ID = UUID.fromString("c1a55000-0003-4003-8003-000000000003");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("c1a55000-0004-4004-8004-000000000004");
    private static final UUID CRIT_CHANCE_ID = UUID.fromString("c1a55000-0005-4005-8005-000000000005");
    private static final UUID CRIT_DAMAGE_ID = UUID.fromString("c1a55000-0006-4006-8006-000000000006");
    private static final UUID BOW_DAMAGE_ID = UUID.fromString("c1a55000-0007-4007-8007-000000000007");
    private static final UUID SPEED_ID = UUID.fromString("c1a55000-0008-4008-8008-000000000008");

    public static void updateAttributes(Player player, PlayerClass playerClass) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance critChance = player.getAttribute(ModAttributes.CRIT_CHANCE.get());
        AttributeInstance critDamage = player.getAttribute(ModAttributes.CRIT_DAMAGE.get());
        AttributeInstance bowDamage = player.getAttribute(ModAttributes.BOW_DAMAGE_MULT.get());

        removeIfPresent(health, HEALTH_ID);
        removeIfPresent(armor, ARMOR_ID);
        removeIfPresent(attackDamage, ATTACK_DAMAGE_ID);
        removeIfPresent(attackSpeed, ATTACK_SPEED_ID);
        removeIfPresent(speed, SPEED_ID);
        removeIfPresent(critChance, CRIT_CHANCE_ID);
        removeIfPresent(critDamage, CRIT_DAMAGE_ID);
        removeIfPresent(bowDamage, BOW_DAMAGE_ID);

        List<Runnable> toApply = new ArrayList<>();

        switch (playerClass) {
            case RITUALISTA_ARCANO -> {
                // -15% daño, -15% vida, hasta que se consagre el Matrimonio de Carne (VinculoDeCarneItem).
                // El limite de fuerza de 50 (y la penalidad post-matrimonio si lo supera) necesita un
                // sistema de "fuerza de arma" que todavia no existe: pendiente, igual que los hechizos.
                if (!player.getPersistentData().getBoolean("matrimonio_consagrado")) {
                    add(toApply, attackDamage, ATTACK_DAMAGE_ID, "Ritualista Damage", -0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
                    add(toApply, health, HEALTH_ID, "Ritualista Health", -0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
                }
            }
            case BERSERKER -> {
                // +25% daño cuerpo a cuerpo, +15% critico (el modo "Furia Berserker" activo es la habilidad).
                add(toApply, attackDamage, ATTACK_DAMAGE_ID, "Berserker Damage", 0.25, AttributeModifier.Operation.MULTIPLY_TOTAL);
                add(toApply, critChance, CRIT_CHANCE_ID, "Berserker Crit Chance", 0.15, AttributeModifier.Operation.ADDITION);
            }
            case GUERRERO_ANIMA -> {
                // +5% velocidad de ataque, +15% critico, +5% daño, +10% vida. (Espada unica/evolucion: pendiente.)
                add(toApply, attackSpeed, ATTACK_SPEED_ID, "Guerrero Anima Attack Speed", 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
                add(toApply, critChance, CRIT_CHANCE_ID, "Guerrero Anima Crit Chance", 0.15, AttributeModifier.Operation.ADDITION);
                add(toApply, attackDamage, ATTACK_DAMAGE_ID, "Guerrero Anima Damage", 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL);
                add(toApply, health, HEALTH_ID, "Guerrero Anima Health", 0.10, AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            case ESCUDERO -> {
                // +20% resistencia (armadura plana), +20% vida. (Invulnerabilidad activa = Guardia Total.)
                add(toApply, armor, ARMOR_ID, "Escudero Armor", 0.20, AttributeModifier.Operation.MULTIPLY_TOTAL);
                add(toApply, health, HEALTH_ID, "Escudero Health", 0.20, AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            case ARQUERO -> {
                // Arco/ballesta: +100% daño (el +50% critico y +50% daño critico, al ser SOLO con
                // arco/ballesta, se calculan en ElementalDamageEvents leyendo si el golpe es flecha,
                // no como atributo plano que afectaria tambien al cuerpo a cuerpo). +10% velocidad.
                // La fuerza ilimitada (y penalidad si excede 50): pendiente, mismo sistema que Ritualista.
                // BOW_DAMAGE_MULT es un multiplicador propio (no un % sobre un stat vanilla), se deja ADDITION.
                add(toApply, bowDamage, BOW_DAMAGE_ID, "Arquero Bow Damage", 1.0, AttributeModifier.Operation.ADDITION);
                add(toApply, speed, SPEED_ID, "Arquero Speed", 0.10, AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            default -> {
                // NINGUNA: sin bonus ni malus.
            }
        }

        toApply.forEach(Runnable::run);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
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
