package com.tcorigenes.tcorigenes.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

/** Unico lugar que toca la API de Curios (asi el resto del mod no la carga si no esta). */
public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static boolean isEquipped(LivingEntity entity, Item item) {
        return ModList.get().isLoaded("curios")
                && CuriosApi.getCuriosInventory(entity).map(handler -> handler.isEquipped(item)).orElse(false);
    }

    /** Hay algun brazalete (de cualquier material) puesto. */
    public static boolean hasBracelet(LivingEntity entity) {
        return ModList.get().isLoaded("curios")
                && CuriosApi.getCuriosInventory(entity)
                        .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof com.tcorigenes.tcorigenes.item.BrazaleteItem))
                        .orElse(false);
    }

    /** Hay algo (cualquier item) puesto en el slot de Curios con ese id. */
    public static boolean hasAnyInSlot(LivingEntity entity, String slotId) {
        return ModList.get().isLoaded("curios")
                && CuriosApi.getCuriosInventory(entity).map(handler -> handler.getStacksHandler(slotId)
                        .map(stacks -> {
                            for (int i = 0; i < stacks.getStacks().getSlots(); i++) {
                                ItemStack stack = stacks.getStacks().getStackInSlot(i);
                                if (!stack.isEmpty()) {
                                    return true;
                                }
                            }
                            return false;
                        }).orElse(false)).orElse(false);
    }
}
