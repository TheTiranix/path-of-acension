// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import com.tudominio.elementaldamage.ModDamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Pedido de alejandr0: bajar el daño de la habilidad de solo click derecho de Frostbound (-60%) y Crescentia (-35%).
 * Las habilidades de Celestisynth hacen todo su daño dentro del tick de su ataque; los mixins de esas dos habilidades
 * (FrostboundDanceAttackMixin, CrescentiaBarrageAttackMixin) marcan aca el factor mientras dura ese tick, y este
 * handler lo aplica al daño normal que sale de ahi (antes que cualquier otro handler, asi el ciclo elemental y los
 * bonos ya parten del daño reducido; los golpes elementales de nuestro sistema ya salen del monto reducido).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class SkillDamageScale {
    private static Player owner;
    private static float scale = 1.0F;

    private SkillDamageScale() {
    }

    public static void begin(Player player, float factor) {
        owner = player;
        scale = factor;
    }

    public static void end() {
        owner = null;
        scale = 1.0F;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (owner == null || event.getSource().getEntity() != owner) {
            return;
        }
        var type = event.getSource().typeHolder().unwrapKey().orElse(null);
        if (type != null && ModDamageTypes.ALL.contains(type)) {
            return; // ya sale de un monto reducido (WeaponElemental convierte el golpe entero)
        }
        event.setAmount(event.getAmount() * scale);
    }
}
