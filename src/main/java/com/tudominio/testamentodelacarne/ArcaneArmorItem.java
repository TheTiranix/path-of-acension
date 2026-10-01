// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import com.github.alexthe666.iceandfire.client.model.armor.ModelSeaSerpentArmor;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Armadura de arcane (pedido de alejandr0): blanco crema (blanco celestial) y con la forma de la armadura Tide Guardian de Ice and Fire:
 * usa su mismo modelo (hombreras y aletas) con una textura propia.
 */
public class ArcaneArmorItem extends ArmorItem {
    public ArcaneArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return new ModelSeaSerpentArmor(slot == EquipmentSlot.LEGS || slot == EquipmentSlot.HEAD);
            }
        });
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "testamentodelacarne:textures/models/armor/arcane_tide" + (slot == EquipmentSlot.LEGS ? "_legs" : "") + ".png";
    }
}
