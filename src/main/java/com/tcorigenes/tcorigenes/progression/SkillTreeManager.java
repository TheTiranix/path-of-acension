package com.tcorigenes.tcorigenes.progression;

import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadout;
import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import com.tcorigenes.tcorigenes.progression.network.SkillSyncPacket;
import com.tudominio.elementaldamage.ModAttributes;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Logica server-side del arbol propio: desbloquear nodos, aplicar sus bonos como modifiers
 * transitorios (se reaplican en login/respawn/cambio de clase via refresh), bonos fijos de
 * origen (raza) y de pacto, y sincronizar el estado al cliente para la pantalla.
 */
public final class SkillTreeManager {
    public static final String PACT_KEY = "tc_pact";

    private record Fixed(String key, Supplier<Attribute> attribute, Operation op, double amount) {
        UUID id() {
            return UUID.nameUUIDFromBytes(("tcorigenes:fixed:" + key).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static Fixed raceBonus(Race race) {
        return switch (race) {
            case ANGEL -> new Fixed("raza_angel", () -> Attributes.ARMOR, Operation.ADDITION, 1.0);
            case DEMONIO -> new Fixed("raza_demonio", () -> Attributes.ATTACK_DAMAGE, Operation.ADDITION, 0.5);
            case DEVOTO -> new Fixed("raza_devoto", () -> Attributes.MAX_HEALTH, Operation.ADDITION, 2.0);
            case ENDER_WARRIOR -> new Fixed("raza_ender_warrior", () -> Attributes.MAX_HEALTH, Operation.ADDITION, 2.0);
            case HEREJE -> new Fixed("raza_hereje", () -> Attributes.MOVEMENT_SPEED, Operation.MULTIPLY_TOTAL, 0.05);
            case MALNACIDO -> new Fixed("raza_malnacido", () -> Attributes.ATTACK_SPEED, Operation.MULTIPLY_TOTAL, 0.05);
            case SIERVO_DE_LA_LUNA -> new Fixed("raza_siervo", () -> Attributes.ARMOR, Operation.ADDITION, 1.0);
            case HUMANO -> null;
        };
    }

    private static final Fixed[] ALL_RACE_BONUSES;
    private static final Fixed PACT_BLOOD = new Fixed("pacto_sangre", SkillTreeManager::lifeSteal, Operation.ADDITION, 0.15);
    private static final Fixed PACT_STEEL = new Fixed("pacto_acero", () -> ModAttributes.CRIT_CHANCE.get(), Operation.ADDITION, 0.10);

    static {
        Race[] races = Race.values();
        ALL_RACE_BONUSES = new Fixed[races.length];
        for (int i = 0; i < races.length; i++) {
            ALL_RACE_BONUSES[i] = raceBonus(races[i]);
        }
    }

    private SkillTreeManager() {
    }

    /** Vampirismo de Apothic Attributes; null si ese mod no esta. */
    private static Attribute lifeSteal() {
        return ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("attributeslib", "life_steal"));
    }

    /** Reaplica todos los bonos del arbol/origen/pacto y manda el estado al cliente. */
    public static void refresh(ServerPlayer player) {
        reapply(player);
        sync(player);
    }

    private static void reapply(Player player) {
        for (SkillNode node : SkillTree.all()) {
            remove(player, node.attribute().get(), node.modifierId());
        }
        for (Fixed fixed : ALL_RACE_BONUSES) {
            if (fixed != null) {
                remove(player, fixed.attribute().get(), fixed.id());
            }
        }
        remove(player, PACT_BLOOD.attribute().get(), PACT_BLOOD.id());
        remove(player, PACT_STEEL.attribute().get(), PACT_STEEL.id());

        Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(null);
        Fixed raceFixed = race == null ? null : raceBonus(race);
        if (raceFixed != null) {
            add(player, raceFixed.attribute().get(), raceFixed.id(), "Origen", raceFixed.amount(), raceFixed.op());
        }
        String pact = player.getPersistentData().getString(PACT_KEY);
        if (pact.equals("sangre")) {
            add(player, PACT_BLOOD.attribute().get(), PACT_BLOOD.id(), "Pacto de sangre", PACT_BLOOD.amount(), PACT_BLOOD.op());
        } else if (pact.equals("acero")) {
            add(player, PACT_STEEL.attribute().get(), PACT_STEEL.id(), "Dogma de acero", PACT_STEEL.amount(), PACT_STEEL.op());
        }

        PlayerClass cls = currentClass(player);
        Set<String> unlocked = unlockedNodes(player);
        for (SkillNode node : SkillTree.all()) {
            if (node.playerClass() == cls && unlocked.contains(node.storageKey())) {
                add(player, node.attribute().get(), node.modifierId(), "Arbol: " + node.title(), node.amount(), node.operation());
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void remove(Player player, Attribute attribute, UUID id) {
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static void add(Player player, Attribute attribute, UUID id, String name, double amount, Operation op) {
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addTransientModifier(new AttributeModifier(id, name, amount, op));
        }
    }

    private static PlayerClass currentClass(Player player) {
        return player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass()).orElse(PlayerClass.NINGUNA);
    }

    private static Set<String> unlockedNodes(Player player) {
        return player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                .map(loadout -> new HashSet<>(loadout.getUnlockedAbilityIds())).orElseGet(HashSet::new);
    }

    public static void sync(ServerPlayer player) {
        int points = player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                .map(PlayerAbilityLoadout.IPlayerAbilityLoadout::getSkillPoints).orElse(0);
        Networking.sendToPlayer(player, new SkillSyncPacket(points, currentClass(player).name(), unlockedNodes(player)));
    }

    public static void addPoints(ServerPlayer player, int amount) {
        player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                .ifPresent(loadout -> loadout.setSkillPoints(Math.max(0, loadout.getSkillPoints() + amount)));
        sync(player);
    }

    /** Disponible = clase correcta, no desbloqueado, y (es raiz o algun padre ya desbloqueado). */
    public static boolean isAvailable(SkillNode node, PlayerClass cls, Set<String> unlocked) {
        if (node.playerClass() != cls || unlocked.contains(node.storageKey())) {
            return false;
        }
        if (node.parents().isEmpty()) {
            return true;
        }
        for (String parent : node.parents()) {
            SkillNode parentNode = SkillTree.get(parent);
            if (parentNode != null && unlocked.contains(parentNode.storageKey())) {
                return true;
            }
        }
        return false;
    }

    public static void tryUnlock(ServerPlayer player, String nodeId) {
        SkillNode node = SkillTree.get(nodeId);
        if (node == null) {
            return;
        }
        player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY).ifPresent(loadout -> {
            if (!isAvailable(node, currentClass(player), loadout.getUnlockedAbilityIds())) {
                player.displayClientMessage(Component.literal("Ese nodo no está disponible."), true);
                return;
            }
            if (loadout.getSkillPoints() < node.cost()) {
                player.displayClientMessage(Component.literal("No tenés suficientes puntos (" + node.cost() + " necesarios)."), true);
                return;
            }
            loadout.setSkillPoints(loadout.getSkillPoints() - node.cost());
            loadout.unlockAbility(node.storageKey());
            if (node.abilityId() != null) {
                loadout.unlockAbility(node.abilityId());
            }
            player.displayClientMessage(Component.literal("¡Desbloqueaste " + node.title() + "!"), true);
        });
        refresh(player);
    }
}
