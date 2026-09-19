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

    public UUID modifierId() {
        return UUID.nameUUIDFromBytes(("tcorigenes:skill:" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Clave con la que se guarda en PlayerAbilityLoadout (prefijo para no mezclarse con habilidades). */
    public String storageKey() {
        return SkillTree.NODE_PREFIX + id;
    }
}
