// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import com.tudominio.testamentodelacarne.util.MiscArmorMaterials;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Casco y botas de Naga Scale: usan las texturas de la armadura de Twilight Forest para que el set se vea completo. */
public class NagaArmorItem extends ArmorItem {
    public NagaArmorItem(ArmorItem.Type type, Properties properties) {
        super(MiscArmorMaterials.NAGA, type, properties);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "twilightforest:textures/armor/naga_scale_" + (slot == EquipmentSlot.LEGS ? 2 : 1) + ".png";
    }
}
