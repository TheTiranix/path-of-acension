package com.tudominio.testamentodelacarne;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.tudominio.testamentodelacarne.util.ModTiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Espada "atada al alma" del jugador: no se puede dañar/reparar (isDamageable/isRepairable = false),
 * usa el ANIMA_TIER (ver ModTiers) y sus tres niveles solo varian en dano/velocidad/alcance de ataque.
 */
public class AnimaSwordItem extends SwordItem {
    private final Multimap<Attribute, AttributeModifier> attributeModifiers;

    public AnimaSwordItem(float attackDamage, float attackSpeed, float attackRange, Properties properties) {
        super(ModTiers.ANIMA_TIER, 0, 0.0F, properties);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", attackSpeed, AttributeModifier.Operation.ADDITION));

        Attribute attackRangeAttribute = (Attribute) ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse("forge:attack_range"));
        if (attackRangeAttribute != null) {
            builder.put(attackRangeAttribute,
                    new AttributeModifier("Weapon range modifier", attackRange - 3.0, AttributeModifier.Operation.ADDITION));
        }

        this.attributeModifiers = builder.build();
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.attributeModifiers : super.getDefaultAttributeModifiers(slot);
    }
}
