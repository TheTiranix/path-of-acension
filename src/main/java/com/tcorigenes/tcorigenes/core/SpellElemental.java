// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tudominio.elementaldamage.ModDamageTypes;
import com.tudominio.elementaldamage.PendingElementalHits;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Los hechizos de Iron's Spellbooks entran en el sistema de daño elemental (diseño de la magia): cada escuela de magia pega con el elemento
 * del pack (fuego, hielo, rayo = aire, sagrado = luz, ender, naturaleza). Sangre, eldritch y evocacion no tienen elemento y quedan como estan.
 * Reglas, iguales que para las armas: con el Prisma Convertidor todo el golpe pasa a su elemento; si la raza tiene un elemento propio solo
 * se convierten los hechizos de ese elemento (los demas pegan como daño magico normal, sin elemental: no se anulan para no dejar inutil al mago).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class SpellElemental {
    private static final Map<String, ResourceKey<DamageType>> SCHOOLS = Map.of(
            "fire_magic", ModDamageTypes.FIRE_ELEMENTAL,
            "ice_magic", ModDamageTypes.ICE,
            "lightning_magic", ModDamageTypes.AIR,
            "holy_magic", ModDamageTypes.LIGHT,
            "ender_magic", ModDamageTypes.ENDER_ELEMENTAL,
            "nature_magic", ModDamageTypes.NATURAL);

    private SpellElemental() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker) || attacker.level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        var key = event.getSource().typeHolder().unwrapKey().orElse(null);
        if (key == null || !key.location().getNamespace().equals("irons_spellbooks")) {
            return;
        }
        ResourceKey<DamageType> element = SCHOOLS.get(key.location().getPath());
        if (element == null) {
            return;
        }
        ResourceKey<DamageType> converted = ElementalRestriction.converterElement(attacker);
        if (converted != null) {
            element = converted;
        } else {
            ResourceKey<DamageType> race = ElementalRestriction.raceElement(attacker);
            if (race != null && !race.equals(element)) {
                return;
            }
        }
        float total = event.getAmount();
        event.setCanceled(true); // el golpe entero pasa a ser el elemental, no se suma aparte
        PendingElementalHits.hurtNow(event.getEntity(), attacker, element, total);
    }
}
