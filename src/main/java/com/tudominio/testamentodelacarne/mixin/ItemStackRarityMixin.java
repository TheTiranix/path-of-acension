// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pedido de alejandr0: sin colores en el nombre de armas y herramientas (excepto las de Celestisynth), para despues
 * implementar una escala de colores segun el nivel del arma. El color del nombre sale de la rareza del item.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackRarityMixin {

    @Inject(method = "m_41791_", at = @At("RETURN"), cancellable = true, remap = false)
    private void testamentodelacarne$plainWeaponNames(CallbackInfoReturnable<Rarity> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (cir.getReturnValue() == Rarity.COMMON) {
            return;
        }
        var item = self.getItem();
        if (com.tcorigenes.tcorigenes.weapon.PlainNameItems.matches(ForgeRegistries.ITEMS.getKey(item))) {
            cir.setReturnValue(Rarity.COMMON);
            return;
        }
        if (item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null && !id.getNamespace().equals("celestisynth")) {
                cir.setReturnValue(Rarity.COMMON);
            }
        }
    }
}
