// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage.event;

import com.tudominio.elementaldamage.MobElementalAffinity;
import com.tudominio.elementaldamage.PendingElementalHits;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Por ahora (simplificacion consciente, a pedido, provisoria hasta confirmar que los efectos
 * andan bien), TODOS los mobs pueden hacer un poco de daño elemental EXTRA aparte de su golpe
 * normal: no se le suma al mismo golpe (para que sea un daño realmente separado, con su propio
 * numero/indicador/resistencia), sino que se encola un segundo golpe 1 tick despues (ver
 * PendingElementalHits) con el elemento de su afinidad (o cualquiera al azar si no tiene ninguno
 * tematico). Una vez confirmado que anda bien, volver a algo tipo "20% de los mobs, no todos".
 */
@EventBusSubscriber(modid = "elementaldamage")
public class MobElementalAttackHandler {
    private static final float EXTRA_DAMAGE_FRACTION = 0.30F;

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            MobElementalAffinity.rollIfNeeded(living);
        }
    }

    /** Llamado desde ElementalDamageEvents#onLivingHurt (mismo paquete) para cualquier golpe que
     *  no sea ya uno de nuestros 9 elementos: si quien pega es un mob, le encola el extra. */
    static void applyExtraElementalDamage(LivingEntity target, DamageSource source, float baseDamage) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker instanceof Player) {
            return;
        }
        if (source.getDirectEntity() != source.getEntity()) {
            return; // solo cuerpo a cuerpo directo (nada de flechas/explosiones/proyectiles)
        }
        if (!(target.level() instanceof ServerLevel serverLevel) || baseDamage <= 0.0F) {
            return;
        }

        List<ResourceKey<DamageType>> affinities = MobElementalAffinity.get(attacker);
        if (affinities.isEmpty()) {
            return;
        }

        ResourceKey<DamageType> chosen = affinities.get(attacker.getRandom().nextInt(affinities.size()));
        // Piso de 5: con la escala continua un extra chico (mobs debiles pegan 2-4) igual genera
        // ALGO de efecto, pero para que sea claramente notorio mientras se prueba, garantizamos
        // un minimo. Bajar/sacar esto una vez confirmado que los efectos andan bien.
        float extra = Math.max(baseDamage * EXTRA_DAMAGE_FRACTION, 5.0F);
        PendingElementalHits.queue(target, attacker, chosen, extra, serverLevel.getGameTime() + PendingElementalHits.SAFE_DELAY_TICKS);
    }
}
