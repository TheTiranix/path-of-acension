// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Cola de golpes elementales "diferidos": el daño extra que le suma un mob a su ataque normal
 * (ver MobElementalAttackHandler) o el rayo de aire (ver ElementalDamageEvents) tiene que ser un
 * daño APARTE, no sumado al mismo evento de golpe (para que se vea como un hit propio en los
 * mods de damage numbers, y para que resistencias/indicador se calculen de forma independiente).
 *
 * Ojo con el delay: Minecraft vanilla tiene una ventana de invulnerabilidad de 20 ticks tras
 * cualquier golpe (LivingEntity.hurt: "if (invulnerableTime > 10) { if (amount <= lastHurt)
 * return false; ... }"). Si el golpe diferido llega DENTRO de esa ventana (invulnerableTime > 10,
 * o sea menos de ~10 ticks despues del golpe original) Y su monto es MENOR O IGUAL al del golpe
 * que lo disparo, Minecraft lo descarta enteramente sin hacer nada de daño. El rayo de aire (la
 * mitad del daño base que lo activo) SIEMPRE es menor o igual, asi que con 1 tick de delay nunca
 * hacia daño. Por eso el delay minimo es SAFE_DELAY_TICKS (mas de 10), para que cuando el golpe
 * diferido llegue, invulnerableTime ya haya bajado de 10 y el golpe se aplique completo sin
 * importar cuanto sea comparado con el anterior.
 */
public final class PendingElementalHits {
    /** Mas de 10 ticks: fuera de la ventana "solo aplica si es mayor al golpe anterior" de vanilla. */
    public static final int SAFE_DELAY_TICKS = 11;
    private record PendingHit(LivingEntity target, LivingEntity attacker, ResourceKey<DamageType> element,
                               float amount, long fireAtTick) {
    }

    private static final List<PendingHit> QUEUE = new ArrayList<>();

    private PendingElementalHits() {
    }

    public static void queue(LivingEntity target, LivingEntity attacker, ResourceKey<DamageType> element, float amount, long fireAtTick) {
        QUEUE.add(new PendingHit(target, attacker, element, amount, fireAtTick));
    }

    /** Llamar todos los ticks: la cola normalmente esta vacia o tiene un par de items nomas.
     *  Primero saca de la cola los que ya vencieron y RECIEN DESPUES les aplica el daño: si se
     *  aplicara adentro del while de arriba, un ElementalDamageSource.hurt que encadenara otro
     *  queue() (por ejemplo un mob que responde el golpe con otro ataque elemental) modificaria
     *  QUEUE en medio de la misma iteracion y tiraba ConcurrentModificationException. */
    public static void tick(long currentGameTime) {
        if (QUEUE.isEmpty()) {
            return;
        }
        List<PendingHit> due = null;
        Iterator<PendingHit> it = QUEUE.iterator();
        while (it.hasNext()) {
            PendingHit hit = it.next();
            if (currentGameTime >= hit.fireAtTick()) {
                it.remove();
                if (due == null) {
                    due = new ArrayList<>();
                }
                due.add(hit);
            }
        }
        if (due == null) {
            return;
        }
        for (PendingHit hit : due) {
            if (hit.target().isAlive() && hit.attacker().isAlive()) {
                ElementalDamageSource.hurt(hit.target(), hit.element(), hit.attacker(), hit.amount());
            }
        }
    }
}
