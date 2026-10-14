// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.weapon;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Proteccion magica (pedido de alejandr0): la armadura protege de la magia igual que del daño normal. El daño magico (el "magic" de vanilla y
 * de pociones, y los daños de hechizos de otros mods) atraviesa la armadura en el juego base; aca se le aplica la misma reduccion que le haria
 * la proteccion y la tenacidad del que lo recibe. Los hechizos que ya pasan por la armadura (no son bypasses_armor) no se tocan.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class MagicProtection {
    private MagicProtection() {
    }

    private static boolean isMagic(net.minecraft.world.damagesource.DamageSource source) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return false;
        }
        var key = source.typeHolder().unwrapKey().orElse(null);
        if (key == null) {
            return false;
        }
        String ns = key.location().getNamespace();
        String path = key.location().getPath();
        if (ns.equals("minecraft")) {
            return path.equals("magic") || path.equals("indirect_magic") || path.equals("dragon_breath") || path.equals("sonic_boom");
        }
        return path.contains("magic") || path.contains("spell") || ns.equals("ars_nouveau") || ns.equals("ars_elemental");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F || !isMagic(event.getSource())) {
            return;
        }
        var entity = event.getEntity();
        float armor = (float) entity.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (armor <= 0.0F) {
            return;
        }
        event.setAmount(CombatRules.getDamageAfterAbsorb(event.getAmount(), armor, toughness));
    }
}
