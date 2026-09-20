package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.core.capability.event.ModEvents;
import com.tudominio.elementaldamage.ModDamageTypes;
import com.tudominio.elementaldamage.PendingElementalHits;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Daño elemental propio de cada raza, en relacion al DAÑO TOTAL del golpe (lo que pega el arma en
 * mano, sea cuerpo a cuerpo, proyectil o magia). Hay dos formas:
 * - "X% mas de daño elemental": se SUMA un golpe elemental aparte por X% del total (Demonio-fuego,
 *   Angel-luz, Siervo-lunar de noche a la luna).
 * - "X% se transforma en daño elemental": se RESTA X% al golpe normal y se convierte en un golpe
 *   elemental aparte (Ender Warrior 5% ender, Gigante Rocoso 10% tierra).
 * El golpe elemental sale diferido (PendingElementalHits), como cualquier otro daño elemental.
 * Los golpes que ya son elementales no generan otro (evita la recursion).
 */
public final class RacialElemental {
    private RacialElemental() {
    }

    public static void apply(LivingHurtEvent event, Player attacker) {
        float total = event.getAmount();
        if (total <= 0.0F || attacker.level().isClientSide()) {
            return;
        }
        Race race = attacker.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
        LivingEntity target = event.getEntity();
        switch (race) {
            case DEMONIO -> extra(target, attacker, ModDamageTypes.FIRE_ELEMENTAL, total * 0.10F);
            case ANGEL -> extra(target, attacker, ModDamageTypes.LIGHT, total * 0.10F);
            case SIERVO_DE_LA_LUNA -> {
                if (ModEvents.isMoonExposed(attacker)) {
                    extra(target, attacker, ModDamageTypes.LUNAR, total * 0.10F);
                }
            }
            case ENDER_WARRIOR -> convert(event, target, attacker, ModDamageTypes.ENDER_ELEMENTAL, 0.05F);
            case STONE_GIANT -> convert(event, target, attacker, ModDamageTypes.EARTH, 0.10F);
            default -> {
            }
        }
    }

    private static void extra(LivingEntity target, Player attacker, ResourceKey<DamageType> element, float amount) {
        PendingElementalHits.queue(target, attacker, element, amount,
                target.level().getGameTime() + PendingElementalHits.SAFE_DELAY_TICKS);
    }

    private static void convert(LivingHurtEvent event, LivingEntity target, Player attacker, ResourceKey<DamageType> element, float fraction) {
        float total = event.getAmount();
        event.setAmount(total * (1.0F - fraction));
        extra(target, attacker, element, total * fraction);
    }
}
