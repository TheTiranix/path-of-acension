// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

/**
 * Un nodo del arbol de habilidades propio. Un nodo da un bono de atributo (attribute != null)
 * y/o desbloquea una habilidad activa (abilityId != null). Esta disponible si el jugador tiene
 * la clase del nodo y alguno de sus padres ya desbloqueado (o si no tiene padres: es la raiz).
 * x/y son coordenadas de grilla para dibujarlo (la raiz esta en 0,0).
 */
public record SkillNode(
        String id,
        PlayerClass playerClass,
        String title,
        Supplier<Item> icon,
        int x,
        int y,
        int cost,
        List<String> parents,
        Supplier<Attribute> attribute,
        AttributeModifier.Operation operation,
        double amount,
        String abilityId) {

    /**
     * Fase del arbol (todas las clases tienen 3): la 1 va de la raiz al nodo 8 de cada camino, la 2 del 9 al 16 y la 3 del 17 al 24
     * mas la piedra clave. El nodo x de la grilla es su posicion en el camino.
     */
    public int phase() {
        return x <= 8 ? 1 : x <= 16 ? 2 : 3;
    }

    /** Titulo en el idioma del juego (los titulos estan escritos en español en SkillTree). */
    public String title() {
        if (title.startsWith("Cumbre: ")) {
            return com.tcorigenes.tcorigenes.core.Tr.s("Cumbre: ") + com.tcorigenes.tcorigenes.core.Tr.s(title.substring(8));
        }
        return com.tcorigenes.tcorigenes.core.Tr.s(title);
    }

    public UUID modifierId() {
        return UUID.nameUUIDFromBytes(("tcorigenes:skill:" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Clave con la que se guarda en PlayerAbilityLoadout (prefijo para no mezclarse con habilidades). */
    public String storageKey() {
        return SkillTree.NODE_PREFIX + id;
    }
}
