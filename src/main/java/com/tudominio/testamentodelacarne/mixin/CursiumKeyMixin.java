// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne.mixin;

import com.tcorigenes.tcorigenes.core.ClimateImmunity;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Pedido de alejandr0: las teclas de la armadura de cursium (vision fantasma del casco, paso atras de las botas) solo funcionan con el set
 * completo. Se corta el paquete de la tecla cuando la pieza asociada es de cursium y el set no esta completo.
 */
@Mixin(targets = "com.github.L_Ender.cataclysm.message.MessageArmorKey", remap = false)
public abstract class CursiumKeyMixin {

    @Inject(method = "lambda$handle$0(Ljava/util/function/Supplier;Lcom/github/L_Ender/cataclysm/message/MessageArmorKey;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void testamentodelacarne$setOnlyKey(Supplier<NetworkEvent.Context> context, @Coerce Object message, CallbackInfo ci) {
        try {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            int index = message.getClass().getField("equipmentSlot").getInt(message);
            EquipmentSlot[] slots = EquipmentSlot.values();
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getItemBySlot(slots[Math.max(0, Math.min(slots.length - 1, index))]).getItem());
            if (id != null && id.getNamespace().equals("cataclysm") && id.getPath().startsWith("cursium_") && !ClimateImmunity.wearsSkymetalSet(player)) {
                ci.cancel();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // si no se puede leer, no se toca el comportamiento original
        }
    }
}
