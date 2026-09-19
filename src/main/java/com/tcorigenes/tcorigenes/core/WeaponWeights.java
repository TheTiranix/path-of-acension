package com.tcorigenes.tcorigenes.core;

import com.tudominio.testamentodelacarne.AnimaSwordItem;
import java.util.regex.Pattern;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * "Destreza requerida" de cada arma/escudo. No hay una tabla por item (son cientos de mods):
 * se calcula por formula a partir del daño de ataque, con reglas por tipo:
 * - melee normal: 6 + 2.5 x daño (un par de espadas de hierro suma 42, dos de netherite 52)
 * - melee de dos manos (por nombre: greatsword, claymore, martillo, guadaña, alabarda...): 60 + daño
 * - arco 15, ballesta 20, escudo 12, escudo grande (tower/great/large/heavy) 30
 * La suma de lo que llevas en ambas manos es el requerimiento total que se compara contra 50.
 */
public final class WeaponWeights {
    public static final int THRESHOLD = 50;
    private static final Pattern TWO_HANDED = Pattern.compile(
            "great.?sword|great.?axe|claymore|zweihander|battle.?axe|war.?hammer|hammer|scythe|halberd|glaive|colossal|great.?blade|executioner");
    private static final Pattern LARGE_SHIELD = Pattern.compile("tower|great|large|heavy|kite");

    private WeaponWeights() {
    }

    private static String path(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.getPath();
    }

    private static double attackDamage(ItemStack stack) {
        double total = 0.0;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                total += modifier.getAmount();
            }
        }
        return total;
    }

    public static boolean isTwoHanded(ItemStack stack) {
        return TWO_HANDED.matcher(path(stack)).find();
    }

    /** 0 si el item no es un arma ni un escudo. */
    public static int weightOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        var item = stack.getItem();
        if (item instanceof ShieldItem || path(stack).contains("shield")) {
            return LARGE_SHIELD.matcher(path(stack)).find() ? 30 : 12;
        }
        if (item instanceof BowItem) {
            return 15;
        }
        if (item instanceof CrossbowItem) {
            return 20;
        }
        if (item instanceof DiggerItem && !(item instanceof AxeItem)) {
            return 0;
        }
        double damage = attackDamage(stack);
        if (damage <= 0.0) {
            return 0;
        }
        if (isTwoHanded(stack)) {
            return (int) Math.round(60 + damage);
        }
        return (int) Math.round(6 + 2.5 * damage);
    }

    public static boolean isAnimaSword(ItemStack stack) {
        return stack.getItem() instanceof AnimaSwordItem;
    }

    /** Requerimiento total: lo que llevas en la mano principal mas lo de la secundaria. */
    public static int totalWeight(net.minecraft.world.entity.LivingEntity entity) {
        return weightOf(entity.getMainHandItem()) + weightOf(entity.getOffhandItem());
    }
}
