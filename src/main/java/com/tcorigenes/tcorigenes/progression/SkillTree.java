// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tudominio.elementaldamage.ModAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Definicion del arbol propio: una rama por clase con la misma forma (raiz, dos caminos de 3
 * nodos y una piedra clave al final que pide cualquiera de los dos caminos). La piedra clave
 * desbloquea la habilidad activa de la clase cuando existe una.
 *
 *            A1 - A2 - A3 \
 *   raiz <                  > clave
 *            B1 - B2 - B3 /
 */
public final class SkillTree {
    public static final String NODE_PREFIX = "node:";
    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();

    private SkillTree() {
    }

    public static SkillNode get(String id) {
        return NODES.get(id);
    }

    public static List<SkillNode> forClass(PlayerClass playerClass) {
        List<SkillNode> result = new ArrayList<>();
        for (SkillNode node : NODES.values()) {
            if (node.playerClass() == playerClass) {
                result.add(node);
            }
        }
        return result;
    }

    /** Id de la habilidad activa de la clase (la que da su piedra clave), o null si no tiene. */
    public static String abilityOf(PlayerClass playerClass) {
        for (SkillNode node : NODES.values()) {
            if (node.playerClass() == playerClass && node.abilityId() != null) {
                return node.abilityId();
            }
        }
        return null;
    }

    public static Iterable<SkillNode> all() {
        return NODES.values();
    }

    private record Perk(String title, Supplier<Item> icon, Supplier<Attribute> attribute, Operation op, double amount) {
    }

    private static Perk perk(String title, Supplier<Item> icon, Supplier<Attribute> attribute, Operation op, double amount) {
        return new Perk(title, icon, attribute, op, amount);
    }

    /** Cuanto rinde cada nodo de un camino respecto de la "unidad" de su estadistica: empieza casi
     *  nada y los ultimos suman mucho. La estadistica rota entre las del camino (nodo 1, 2, 3, 1...),
     *  asi que la 3a recibe el multiplicador mas grande (x6) en el ultimo nodo. */
    private static final double[] RAMP = {1, 1, 1, 2, 2, 2, 3, 4, 6};
    public static final int NODES_PER_PATH = RAMP.length;

    private record Stat(Supplier<Attribute> attribute, Operation op, double unit) {
    }

    private record Named(String title, Supplier<Item> icon) {
    }

    private static Named n(String title, Supplier<Item> icon) {
        return new Named(title, icon);
    }

    private static Stat stat(Supplier<Attribute> attribute, Operation op, double unit) {
        return new Stat(attribute, op, unit);
    }

    /** Rama de 20 nodos: raiz, dos caminos de 9 nodos y una piedra clave que pide cualquiera de los dos. */
    private static void branch(PlayerClass cls, String key, Perk root,
                               Stat[] statsA, Named[] a, Stat[] statsB, Named[] b, Perk keystone, String abilityId) {
        add(cls, key + "_raiz", root, 0, 0, 1, List.of(), null);
        String prevA = key + "_raiz";
        String prevB = key + "_raiz";
        for (int i = 0; i < RAMP.length; i++) {
            int cost = i < 6 ? 1 : 2;
            String idA = key + "_a" + (i + 1);
            String idB = key + "_b" + (i + 1);
            add(cls, idA, pathPerk(a[i], statsA[i % statsA.length], RAMP[i]), i + 1, -1, cost, List.of(prevA), null);
            add(cls, idB, pathPerk(b[i], statsB[i % statsB.length], RAMP[i]), i + 1, 1, cost, List.of(prevB), null);
            prevA = idA;
            prevB = idB;
        }
        add(cls, key + "_clave", keystone, RAMP.length + 1, 0, 3, List.of(prevA, prevB), abilityId);
    }

    private static Perk pathPerk(Named named, Stat stat, double factor) {
        double amount = Math.round(stat.unit() * factor * 1000.0) / 1000.0;
        return new Perk(named.title(), named.icon(), stat.attribute(), stat.op(), amount);
    }

    private static void add(PlayerClass cls, String id, Perk perk, int x, int y, int cost, List<String> parents, String abilityId) {
        NODES.put(id, new SkillNode(id, cls, perk.title(), perk.icon(), x, y, cost, parents,
                perk.attribute(), perk.op(), perk.amount(), abilityId));
    }

    private static final Operation ADD = Operation.ADDITION;
    private static final Operation PCT = Operation.MULTIPLY_BASE; // % menor: se suma con los de items, ver OriginBonuses

    static {
        Supplier<Attribute> dmg = () -> Attributes.ATTACK_DAMAGE;
        Supplier<Attribute> atkSpeed = () -> Attributes.ATTACK_SPEED;
        Supplier<Attribute> hp = () -> Attributes.MAX_HEALTH;
        Supplier<Attribute> armor = () -> Attributes.ARMOR;
        Supplier<Attribute> tough = () -> Attributes.ARMOR_TOUGHNESS;
        Supplier<Attribute> knock = () -> Attributes.KNOCKBACK_RESISTANCE;
        Supplier<Attribute> speed = () -> Attributes.MOVEMENT_SPEED;
        Supplier<Attribute> crit = () -> ModAttributes.CRIT_CHANCE.get();
        Supplier<Attribute> critDmg = () -> ModAttributes.CRIT_DAMAGE.get();
        Supplier<Attribute> bow = () -> ModAttributes.BOW_DAMAGE_MULT.get();

        branch(PlayerClass.BERSERKER, "berserker",
                perk("Sed de Sangre", () -> Items.IRON_AXE, dmg, PCT, 0.02),
                new Stat[]{stat(dmg, PCT, 0.01), stat(atkSpeed, PCT, 0.01), stat(crit, ADD, 0.01)},
                new Named[]{
                        n("Filo Sediento", () -> Items.IRON_SWORD), n("Golpes Veloces", () -> Items.SUGAR),
                        n("Golpe Certero", () -> Items.BLAZE_POWDER), n("Furia Creciente", () -> Items.GOLDEN_AXE),
                        n("Ritmo de Batalla", () -> Items.CLOCK), n("Instinto Asesino", () -> Items.SPIDER_EYE),
                        n("Hoja Carmesi", () -> Items.DIAMOND_SWORD), n("Danza de Guerra", () -> Items.BLAZE_ROD),
                        n("Verdugo", () -> Items.DIAMOND_AXE)},
                new Stat[]{stat(hp, ADD, 1.0), stat(armor, ADD, 0.5), stat(knock, ADD, 0.02)},
                new Named[]{
                        n("Sangre Endurecida", () -> Items.APPLE), n("Piel Curtida", () -> Items.LEATHER),
                        n("Postura Firme", () -> Items.IRON_BOOTS), n("Vigor Salvaje", () -> Items.GOLDEN_APPLE),
                        n("Cuero Cicatrizado", () -> Items.LEATHER_CHESTPLATE), n("Cuerpo de Roble", () -> Items.OAK_LOG),
                        n("Corazon de Bestia", () -> Items.COOKED_BEEF), n("Piel de Oso", () -> Items.CHAINMAIL_CHESTPLATE),
                        n("Inamovible", () -> Items.ANVIL)},
                perk("Furia Berserker", () -> Items.NETHERITE_AXE, dmg, PCT, 0.10), "tcorigenes:furia_berserker");

        branch(PlayerClass.ESCUDERO, "escudero",
                perk("Guardia Firme", () -> Items.SHIELD, armor, ADD, 1.0),
                new Stat[]{stat(armor, ADD, 0.5), stat(tough, ADD, 0.5), stat(knock, ADD, 0.02)},
                new Named[]{
                        n("Piel de Hierro", () -> Items.IRON_CHESTPLATE), n("Guardia Templada", () -> Items.IRON_INGOT),
                        n("Pies Firmes", () -> Items.IRON_BOOTS), n("Escudo Reforzado", () -> Items.SHIELD),
                        n("Placas Gruesas", () -> Items.IRON_HELMET), n("Peso Muerto", () -> Items.ANVIL),
                        n("Muralla", () -> Items.IRON_BLOCK), n("Coraza de Diamante", () -> Items.DIAMOND_CHESTPLATE),
                        n("Bastion", () -> Items.NETHERITE_INGOT)},
                new Stat[]{stat(hp, ADD, 1.0), stat(armor, ADD, 0.5), stat(tough, ADD, 0.5)},
                new Named[]{
                        n("Resistencia", () -> Items.COOKED_BEEF), n("Cota Pesada", () -> Items.CHAINMAIL_CHESTPLATE),
                        n("Temple", () -> Items.FURNACE), n("Aguante", () -> Items.GOLDEN_APPLE),
                        n("Casco Grueso", () -> Items.CHAINMAIL_HELMET), n("Voluntad de Hierro", () -> Items.IRON_BARS),
                        n("Corazon de Acero", () -> Items.ENCHANTED_GOLDEN_APPLE), n("Guardian", () -> Items.DIAMOND_HELMET),
                        n("Coloso", () -> Items.NETHERITE_CHESTPLATE)},
                perk("Guardia Total", () -> Items.NETHERITE_CHESTPLATE, armor, ADD, 3.0), "tcorigenes:guardia_total");

        branch(PlayerClass.ARQUERO, "arquero",
                perk("Paso Ligero", () -> Items.FEATHER, speed, PCT, 0.02),
                new Stat[]{stat(atkSpeed, PCT, 0.01), stat(crit, ADD, 0.01), stat(bow, ADD, 0.02)},
                new Named[]{
                        n("Pulso Firme", () -> Items.BOW), n("Punteria", () -> Items.SPECTRAL_ARROW),
                        n("Tiro Potente", () -> Items.ARROW), n("Cuerda Tensa", () -> Items.STRING),
                        n("Ojo Entrenado", () -> Items.SPYGLASS), n("Punta de Pedernal", () -> Items.FLINT),
                        n("Plumas Veloces", () -> Items.FEATHER), n("Ballesta Maestra", () -> Items.CROSSBOW),
                        n("Tirador de Elite", () -> Items.TARGET)},
                new Stat[]{stat(speed, PCT, 0.005), stat(hp, ADD, 1.0), stat(armor, ADD, 0.5)},
                new Named[]{
                        n("Sombra del Bosque", () -> Items.LEATHER_BOOTS), n("Aliento Largo", () -> Items.APPLE),
                        n("Camuflaje", () -> Items.LEATHER_CHESTPLATE), n("Zancada", () -> Items.SUGAR),
                        n("Pulmones de Cazador", () -> Items.GLOW_BERRIES), n("Cota de Cazador", () -> Items.CHAINMAIL_CHESTPLATE),
                        n("Viento en Popa", () -> Items.PHANTOM_MEMBRANE), n("Vigor del Halcon", () -> Items.GOLDEN_CARROT),
                        n("Manto del Fantasma", () -> Items.LEATHER_HELMET)},
                perk("Ojo de Halcon", () -> Items.ENDER_EYE, bow, ADD, 0.15), "tcorigenes:ojo_de_halcon");

        branch(PlayerClass.GUERRERO_ANIMA, "anima",
                perk("Espiritu Afilado", () -> Items.SOUL_TORCH, dmg, PCT, 0.02),
                new Stat[]{stat(atkSpeed, PCT, 0.01), stat(critDmg, ADD, 0.02), stat(dmg, PCT, 0.01)},
                new Named[]{
                        n("Danza del Alma", () -> Items.SOUL_SAND), n("Golpe Etereo", () -> Items.GHAST_TEAR),
                        n("Corte Espectral", () -> Items.IRON_SWORD), n("Ritmo Anima", () -> Items.CLOCK),
                        n("Eco del Filo", () -> Items.AMETHYST_SHARD), n("Tajo del Alma", () -> Items.DIAMOND_SWORD),
                        n("Torbellino Etereo", () -> Items.ENDER_EYE), n("Alma Furiosa", () -> Items.BLAZE_ROD),
                        n("Espada Eterna", () -> Items.NETHERITE_SWORD)},
                new Stat[]{stat(armor, ADD, 0.5), stat(hp, ADD, 1.0), stat(tough, ADD, 0.5)},
                new Named[]{
                        n("Velo Espiritual", () -> Items.PHANTOM_MEMBRANE), n("Vigor Etereo", () -> Items.GLOW_BERRIES),
                        n("Alma de Acero", () -> Items.IRON_INGOT), n("Manto de Almas", () -> Items.SOUL_CAMPFIRE),
                        n("Latido Anima", () -> Items.HEART_OF_THE_SEA), n("Piel Espectral", () -> Items.CHAINMAIL_LEGGINGS),
                        n("Aura Etereo", () -> Items.END_CRYSTAL), n("Corazon Ancestral", () -> Items.GOLDEN_APPLE),
                        n("Guardian Espectral", () -> Items.DIAMOND_CHESTPLATE)},
                perk("Espiritu Ánima", () -> Items.SOUL_LANTERN, dmg, PCT, 0.08), "tcorigenes:espiritu_anima");

        branch(PlayerClass.RITUALISTA_ARCANO, "ritualista",
                perk("Pacto de Vida", () -> Items.REDSTONE, hp, PCT, 0.03),
                new Stat[]{stat(dmg, PCT, 0.01), stat(crit, ADD, 0.01), stat(critDmg, ADD, 0.02)},
                new Named[]{
                        n("Poder Prohibido", () -> Items.BLAZE_POWDER), n("Mirada Arcana", () -> Items.ENDER_EYE),
                        n("Ritual Oscuro", () -> Items.WITHER_ROSE), n("Sigilo Ritual", () -> Items.AMETHYST_SHARD),
                        n("Runa Ardiente", () -> Items.MAGMA_CREAM), n("Libro Prohibido", () -> Items.BOOK),
                        n("Pacto de Sangre", () -> Items.REDSTONE_BLOCK), n("Circulo Arcano", () -> Items.ENCHANTING_TABLE),
                        n("Palabra Impia", () -> Items.WRITTEN_BOOK)},
                new Stat[]{stat(hp, ADD, 1.0), stat(armor, ADD, 0.5), stat(tough, ADD, 0.5)},
                new Named[]{
                        n("Sangre Consagrada", () -> Items.GHAST_TEAR), n("Aura Protectora", () -> Items.LAPIS_LAZULI),
                        n("Ofrenda", () -> Items.GLISTERING_MELON_SLICE), n("Vinculo de Carne", () -> Items.REDSTONE_TORCH),
                        n("Manto Ritual", () -> Items.LEATHER_CHESTPLATE), n("Escudo de Runas", () -> Items.SHIELD),
                        n("Vida Prestada", () -> Items.GOLDEN_APPLE), n("Aura Sagrada", () -> Items.TOTEM_OF_UNDYING),
                        n("Inmortal", () -> Items.NETHER_STAR)},
                perk("Maestria Arcana", () -> Items.ENCHANTED_BOOK, dmg, PCT, 0.10), null);
    }
}
