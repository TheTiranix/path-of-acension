// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import com.tudominio.elementaldamage.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Bonos "de daño en general, incluyendo proyectiles, magia y daño elemental" de las razas.
 * El cuerpo a cuerpo del jugador ya recibe esos bonos por el atributo de daño de ataque
 * (RaceAttributeManager / ModEvents), asi que aca se aplican solo al daño que NO es un golpe
 * cuerpo a cuerpo (flechas, hechizos, daño elemental), para no contarlos dos veces.
 * - Demonio: +5% en general; ignora un 7% de la armadura de la victima en TODO ataque.
 * - Siervo de la Luna (expuesto a la luna): +10% en general.
 * - Malnacido: maldito -> proyectiles y magia 35% mas debiles; purificado -> +15% en general.
 * - Ritualista sin matrimonio: -10% de daño fisico (las flechas; el melee va por atributo).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class GeneralDamageRules {
    private static final float DEMONIO_ARMOR_IGNORE = 0.07F;

    private GeneralDamageRules() {
    }

    private static boolean isMagic(DamageSource source, boolean elemental) {
        if (elemental) {
            return false;
        }
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            return true;
        }
        var key = source.typeHolder().unwrapKey().orElse(null);
        // Hechizos de mods (Iron's Spellbooks, Ars Nouveau...) usan tipos de daño de su propio namespace.
        return key != null && !key.location().getNamespace().equals("minecraft");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker) || attacker.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        Race race = attacker.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
        PlayerClass cls = attacker.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).map(d -> d.getPlayerClass()).orElse(PlayerClass.NINGUNA);

        var key = source.typeHolder().unwrapKey().orElse(null);
        boolean elemental = key != null && ModDamageTypes.ALL.contains(key);
        boolean melee = source.is(DamageTypes.PLAYER_ATTACK);
        boolean projectile = source.getDirectEntity() instanceof Projectile;
        boolean magic = isMagic(source, elemental);

        float multiplier = 1.0F;
        if (!melee) {
            switch (race) {
                case DEMONIO -> multiplier *= 1.05F;
                case SIERVO_DE_LA_LUNA -> {
                    if (ModEvents.isMoonExposed(attacker)) {
                        multiplier *= 1.10F;
                    }
                }
                case MALNACIDO -> {
                    if (attacker.getPersistentData().getBoolean("malnacido_purificado")) {
                        multiplier *= 1.15F;
                    }
                }
                default -> {
                }
            }
        }
        if (race == Race.MALNACIDO && !attacker.getPersistentData().getBoolean("malnacido_purificado")
                && (projectile || magic)) {
            multiplier *= 0.65F;
        }
        if (cls == PlayerClass.RITUALISTA_ARCANO && !attacker.getPersistentData().getBoolean("matrimonio_consagrado")
                && projectile && !elemental && !magic) {
            multiplier *= 0.90F;
        }
        if (multiplier != 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }

        if (race == Race.DEMONIO && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            LivingEntity target = event.getEntity();
            float armor = (float) target.getArmorValue();
            if (armor > 0.0F) {
                float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                float damage = event.getAmount();
                float normal = CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
                float ignoring = CombatRules.getDamageAfterAbsorb(damage, armor * (1.0F - DEMONIO_ARMOR_IGNORE), toughness);
                if (normal > 0.0F) {
                    event.setAmount(damage * (ignoring / normal));
                }
            }
        }
    }
}
