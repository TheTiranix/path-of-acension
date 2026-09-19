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

    public static Iterable<SkillNode> all() {
        return NODES.values();
    }

    private record Perk(String title, Supplier<Item> icon, Supplier<Attribute> attribute, Operation op, double amount) {
    }

    private static Perk perk(String title, Supplier<Item> icon, Supplier<Attribute> attribute, Operation op, double amount) {
        return new Perk(title, icon, attribute, op, amount);
    }

    private static void branch(PlayerClass cls, String key, Perk root, Perk[] a, Perk[] b, Perk keystone, String abilityId) {
        add(cls, key + "_raiz", root, 0, 0, 1, List.of(), null);
        String prevA = key + "_raiz";
        String prevB = key + "_raiz";
        for (int i = 0; i < 3; i++) {
            String idA = key + "_a" + (i + 1);
            String idB = key + "_b" + (i + 1);
            add(cls, idA, a[i], i + 1, -1, 1, List.of(prevA), null);
            add(cls, idB, b[i], i + 1, 1, 1, List.of(prevB), null);
            prevA = idA;
            prevB = idB;
        }
        add(cls, key + "_clave", keystone, 4, 0, 2, List.of(prevA, prevB), abilityId);
    }

    private static void add(PlayerClass cls, String id, Perk perk, int x, int y, int cost, List<String> parents, String abilityId) {
        NODES.put(id, new SkillNode(id, cls, perk.title(), perk.icon(), x, y, cost, parents,
                perk.attribute(), perk.op(), perk.amount(), abilityId));
    }

    private static final Operation ADD = Operation.ADDITION;
    private static final Operation PCT = Operation.MULTIPLY_TOTAL;

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
                perk("Sed de Sangre", () -> Items.IRON_AXE, dmg, PCT, 0.05),
                new Perk[]{
                        perk("Filo Sediento", () -> Items.IRON_SWORD, dmg, PCT, 0.05),
                        perk("Golpes Veloces", () -> Items.SUGAR, atkSpeed, PCT, 0.05),
                        perk("Golpe Certero", () -> Items.BLAZE_POWDER, crit, ADD, 0.05)},
                new Perk[]{
                        perk("Sangre Endurecida", () -> Items.APPLE, hp, ADD, 2.0),
                        perk("Piel Curtida", () -> Items.LEATHER, armor, ADD, 1.0),
                        perk("Vigor Salvaje", () -> Items.GOLDEN_APPLE, hp, ADD, 2.0)},
                perk("Furia Berserker", () -> Items.NETHERITE_AXE, dmg, PCT, 0.05), "tcorigenes:furia_berserker");

        branch(PlayerClass.ESCUDERO, "escudero",
                perk("Guardia Firme", () -> Items.SHIELD, armor, ADD, 1.0),
                new Perk[]{
                        perk("Piel de Hierro", () -> Items.IRON_CHESTPLATE, armor, ADD, 1.0),
                        perk("Guardia Templada", () -> Items.IRON_INGOT, tough, ADD, 1.0),
                        perk("Muralla", () -> Items.IRON_BLOCK, armor, ADD, 2.0)},
                new Perk[]{
                        perk("Pies Firmes", () -> Items.IRON_BOOTS, knock, ADD, 0.1),
                        perk("Resistencia", () -> Items.COOKED_BEEF, hp, ADD, 2.0),
                        perk("Aguante", () -> Items.GOLDEN_APPLE, hp, ADD, 4.0)},
                perk("Guardia Total", () -> Items.NETHERITE_CHESTPLATE, armor, ADD, 1.0), "tcorigenes:guardia_total");

        branch(PlayerClass.ARQUERO, "arquero",
                perk("Paso Ligero", () -> Items.FEATHER, speed, PCT, 0.02),
                new Perk[]{
                        perk("Pulso Firme", () -> Items.BOW, atkSpeed, PCT, 0.05),
                        perk("Tiro Potente", () -> Items.ARROW, bow, ADD, 0.05),
                        perk("Punteria", () -> Items.SPECTRAL_ARROW, crit, ADD, 0.05)},
                new Perk[]{
                        perk("Sombra del Bosque", () -> Items.LEATHER_BOOTS, speed, PCT, 0.03),
                        perk("Aliento Largo", () -> Items.APPLE, hp, ADD, 2.0),
                        perk("Camuflaje", () -> Items.LEATHER_CHESTPLATE, armor, ADD, 1.0)},
                perk("Ojo de Halcon", () -> Items.ENDER_EYE, bow, ADD, 0.05), "tcorigenes:ojo_de_halcon");

        branch(PlayerClass.GUERRERO_ANIMA, "anima",
                perk("Espiritu Afilado", () -> Items.SOUL_TORCH, dmg, PCT, 0.04),
                new Perk[]{
                        perk("Danza del Alma", () -> Items.SOUL_SAND, atkSpeed, PCT, 0.04),
                        perk("Corte Espectral", () -> Items.IRON_SWORD, dmg, PCT, 0.04),
                        perk("Golpe Etereo", () -> Items.GHAST_TEAR, critDmg, ADD, 0.10)},
                new Perk[]{
                        perk("Velo Espiritual", () -> Items.PHANTOM_MEMBRANE, armor, ADD, 1.0),
                        perk("Vigor Etereo", () -> Items.GLOW_BERRIES, hp, ADD, 2.0),
                        perk("Alma de Acero", () -> Items.IRON_INGOT, tough, ADD, 1.0)},
                perk("Espiritu Ánima", () -> Items.SOUL_LANTERN, dmg, PCT, 0.05), "tcorigenes:espiritu_anima");

        branch(PlayerClass.RITUALISTA_ARCANO, "ritualista",
                perk("Pacto de Vida", () -> Items.REDSTONE, hp, PCT, 0.05),
                new Perk[]{
                        perk("Sigilo Ritual", () -> Items.AMETHYST_SHARD, tough, ADD, 1.0),
                        perk("Aura Protectora", () -> Items.LAPIS_LAZULI, armor, ADD, 1.0),
                        perk("Sangre Consagrada", () -> Items.GHAST_TEAR, hp, PCT, 0.05)},
                new Perk[]{
                        perk("Poder Prohibido", () -> Items.BLAZE_POWDER, dmg, PCT, 0.05),
                        perk("Ritual Oscuro", () -> Items.WITHER_ROSE, dmg, PCT, 0.05),
                        perk("Mirada Arcana", () -> Items.ENDER_EYE, crit, ADD, 0.05)},
                perk("Maestria Arcana", () -> Items.ENCHANTED_BOOK, dmg, PCT, 0.10), null);
    }
}
