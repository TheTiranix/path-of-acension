// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import com.github.L_Ender.cataclysm.client.model.CMModelLayers;
import com.github.L_Ender.cataclysm.client.model.armor.Ignitium_Armor_Model;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Armadura con el modelo del ignitium de Cataclysm (pedido de alejandr0), pero sin los cuernos y con lo que sobresale de los hombros un 75%
 * mas chico. La textura propia (crema con detalles de diamante para el arcano, acero negro con detalles de diamante para el black steel)
 * esta en textures/models/armor/<prefijo>_ignitium(.png | _legs.png).
 */
public class IgnitiumStyleArmorItem extends ArmorItem {
    private static Ignitium_Armor_Model main;
    private static Ignitium_Armor_Model legs;
    private final String prefix;

    public IgnitiumStyleArmorItem(String prefix, ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
        this.prefix = prefix;
    }

    private static void part(ModelPart parent, String name, java.util.function.Consumer<ModelPart> action) {
        try {
            action.accept(parent.getChild(name));
        } catch (java.util.NoSuchElementException ignored) {
            // otra version del modelo: se deja como esta
        }
    }

    private static Ignitium_Armor_Model build(net.minecraft.client.model.geom.ModelLayerLocation layer) {
        Ignitium_Armor_Model model = new Ignitium_Armor_Model(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
        part(model.head, "right_horn", p -> p.visible = false);
        part(model.head, "left_horn", p -> p.visible = false);
        for (ModelPart arm : new ModelPart[] {model.rightArm, model.leftArm}) {
            for (String name : new String[] {"right_shoulderpad", "left_shoulderpad"}) {
                part(arm, name, p -> {
                    p.xScale = 0.25F;
                    p.yScale = 0.25F;
                    p.zScale = 0.25F;
                });
            }
        }
        return model;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (slot == EquipmentSlot.LEGS) {
                    if (legs == null) {
                        legs = build(CMModelLayers.IGNITIUM_ARMOR_MODEL_LEGS);
                    }
                    return legs;
                }
                if (main == null) {
                    main = build(CMModelLayers.IGNITIUM_ARMOR_MODEL);
                }
                return main;
            }
        });
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "testamentodelacarne:textures/models/armor/" + this.prefix + "_ignitium" + (slot == EquipmentSlot.LEGS ? "_legs" : "") + ".png";
    }
}
