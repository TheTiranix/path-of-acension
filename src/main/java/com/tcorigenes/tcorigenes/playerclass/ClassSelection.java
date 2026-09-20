// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.playerclass;

import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Punto unico para asignar una clase (usado por la pantalla de eleccion y por /setclass). */
public final class ClassSelection {
    private ClassSelection() {
    }

    public static PlayerClass currentClass(ServerPlayer player) {
        return player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass()).orElse(PlayerClass.NINGUNA);
    }

    public static void apply(ServerPlayer player, PlayerClass selectedClass) {
        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .ifPresent(data -> data.setPlayerClass(selectedClass));
        ClassAttributeManager.updateAttributes(player, selectedClass);
        // La habilidad equipada (tecla G) tiene que ser la de la clase nueva: si venia de otra clase
        // se quedaba la vieja (ej. Guardia Total del Escudero siendo Berserker).
        String classAbility = com.tcorigenes.tcorigenes.progression.SkillTree.abilityOf(selectedClass);
        player.getCapability(com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                .ifPresent(loadout -> loadout.setEquippedAbilityId(
                        classAbility != null && loadout.isUnlocked(classAbility) ? classAbility : null));
        if (selectedClass == PlayerClass.GUERRERO_ANIMA) {
            // Espada ligada al alma desde el principio (solo si todavia no la tiene).
            var sword = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "espada_anima_1"));
            if (sword != null && !player.getInventory().contains(new ItemStack(sword))) {
                player.getInventory().add(new ItemStack(sword));
            }
        }
    }
}
