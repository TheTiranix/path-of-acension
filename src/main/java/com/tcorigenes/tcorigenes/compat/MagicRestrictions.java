// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import java.lang.reflect.Method;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

/**
 * La magia es cosa del Ritualista Arcano. Los demas solo tienen acceso limitado:
 * - Iron's Spellbooks: cualquiera puede lanzar hechizos de nivel 1; de nivel 2 para arriba, solo el Ritualista, y su tope sube con el
 *   nivel de progresion (ver ProgressionTier y MAGE_LEVEL_CAP): 2 en el Overworld, 3 tras el Nether/Aether, 4 tras Twilight/Alex's Caves,
 *   sin tope tras matar al Ender Dragon.
 * - Ars Nouveau: solo el Ritualista, y los glifos de tier 2 y 3 se desbloquean con el nivel de progresion 2 y 3.
 * Se engancha por reflexion (sin compilar contra esos mods) y solo si estan instalados.
 */
public final class MagicRestrictions {
    private static final String IRONS_EVENT = "io.redspace.ironsspellbooks.api.events.SpellPreCastEvent";
    private static final String ARS_EVENT = "com.hollingsworth.arsnouveau.api.event.SpellCastEvent";

    /** Nivel maximo de hechizo (Iron's Spellbooks) del Ritualista segun el nivel de progresion 1..4; el 4 no tiene tope. */
    private static final int[] MAGE_LEVEL_CAP = {2, 2, 3, 4, Integer.MAX_VALUE};

    private MagicRestrictions() {
    }

    public static void register() {
        if (ModList.get().isLoaded("irons_spellbooks")) {
            hook(IRONS_EVENT, event -> {
                Player player = playerOf(event);
                if (player == null) {
                    return;
                }
                int level = spellLevel(event);
                if (!castSourceAllowed(player, event)) {
                    if (!player.level().isClientSide()) {
                        player.displayClientMessage(Component.literal(com.tcorigenes.tcorigenes.core.Tr.s(
                                "Necesitas un baston en alguna mano, o el hechizo imbuido en un item que lleves puesto.")), true);
                    }
                    event.setCanceled(true);
                    return;
                }
                if (!isMage(player)) {
                    if (level > 1) {
                        deny(player, "pa.m.mage_lvl2");
                        event.setCanceled(true);
                    }
                } else if (!(player.isCreative() && player.hasPermissions(2))) {
                    int tier = com.tcorigenes.tcorigenes.core.ProgressionTier.get(player);
                    int cap = MAGE_LEVEL_CAP[Math.min(tier, 4)];
                    if (level > cap) {
                        if (!player.level().isClientSide()) {
                            player.displayClientMessage(Component.translatable("tcorigenes.magic.cap", cap), true);
                        }
                        event.setCanceled(true);
                    }
                }
            });
        }
        if (ModList.get().isLoaded("ars_nouveau")) {
            hook(ARS_EVENT, event -> {
                Player player = playerOf(event);
                if (player == null) {
                    return;
                }
                if (!isMage(player)) {
                    deny(player, "pa.m.mage_ars");
                    event.setCanceled(true);
                } else if (!(player.isCreative() && player.hasPermissions(2))) {
                    int allowed = Math.min(3, com.tcorigenes.tcorigenes.core.ProgressionTier.get(player));
                    int needed = highestGlyphTier(event);
                    if (needed > allowed) {
                        if (!player.level().isClientSide()) {
                            player.displayClientMessage(Component.translatable("tcorigenes.magic.glyph_tier", needed), true);
                        }
                        event.setCanceled(true);
                    }
                }
            });
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hook(String className, java.util.function.Consumer<Event> handler) {
        try {
            Class<? extends Event> type = (Class<? extends Event>) Class.forName(className);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, (Class) type, (java.util.function.Consumer) handler);
        } catch (ReflectiveOperationException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger(MagicRestrictions.class).warn("No se pudo enganchar {}: {}", className, e.toString());
        }
    }

    /**
     * Pedido de alejandr0: solo se pueden castear hechizos si estan imbuidos en una equipacion puesta (espada, pechera, escudo...: fuente
     * SWORD de Iron's Spellbooks) o si hay cualquier baston en alguna de las dos manos. Los hechizos de comandos y de mobs no se tocan.
     */
    private static boolean castSourceAllowed(Player player, Event event) {
        String source;
        try {
            source = String.valueOf(event.getClass().getMethod("getCastSource").invoke(event));
        } catch (ReflectiveOperationException e) {
            return true;
        }
        if (source.equals("SWORD") || source.equals("COMMAND") || source.equals("MOB") || source.equals("NONE")
                || player.isCreative() && player.hasPermissions(2)) {
            return true;
        }
        return isStaff(player.getMainHandItem()) || isStaff(player.getOffhandItem()) || fromStarterBook(player, event, source);
    }

    /**
     * Pedido del usuario: los hechizos de los libros iniciales (los de GodSpellbooks: libro soulbound con los hechizos bloqueados) se lanzan sin
     * baston; cualquier otro hechizo sigue pidiendolo. El libro tiene que estar puesto (slot de Curios) y el hechizo ser uno de los suyos.
     */
    private static boolean fromStarterBook(Player player, Event event, String source) {
        if (!source.equals("SPELLBOOK")) {
            return false;
        }
        String spellId;
        try {
            spellId = String.valueOf(event.getClass().getMethod("getSpellId").invoke(event));
        } catch (ReflectiveOperationException e) {
            return false;
        }
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(stack -> isStarterBook(stack, spellId))).orElse(false);
    }

    private static boolean isStarterBook(net.minecraft.world.item.ItemStack stack, String spellId) {
        if (!com.tcorigenes.tcorigenes.core.SoulboundItems.hasMarker(stack) || !stack.hasTag()) {
            return false;
        }
        var data = stack.getTag().getCompound("ISB_Spells").getList("data", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < data.size(); i++) {
            var spell = data.getCompound(i);
            if (spell.getBoolean("locked") && spell.getString("id").equals(spellId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStaff(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        if (path.contains("quarterstaff")) {
            return false; // el baston de combate (arma cuerpo a cuerpo) no es un baston magico
        }
        return path.contains("staff") || stack.getItem().getClass().getName().startsWith("io.redspace.ironsspellbooks.item.weapons.Staff");
    }

    private static Player playerOf(Event event) {
        try {
            Method getEntity = event.getClass().getMethod("getEntity");
            return getEntity.invoke(event) instanceof Player player ? player : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static int spellLevel(Event event) {
        try {
            return (int) event.getClass().getMethod("getSpellLevel").invoke(event);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    /** Mayor tier (1..3) entre los glifos del hechizo de Ars Nouveau: spell.recipe[i].getConfigTier().value, por reflexion. */
    private static int highestGlyphTier(Event event) {
        try {
            Object spell = event.getClass().getField("spell").get(event);
            Object recipe = spell.getClass().getField("recipe").get(spell);
            int highest = 1;
            for (Object part : (Iterable<?>) recipe) {
                Object tier = part.getClass().getMethod("getConfigTier").invoke(part);
                highest = Math.max(highest, tier.getClass().getField("value").getInt(tier));
            }
            return highest;
        } catch (ReflectiveOperationException | ClassCastException e) {
            return 1;
        }
    }

    private static boolean isMage(Player player) {
        return player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass() == PlayerClass.RITUALISTA_ARCANO).orElse(false)
                || player.hasPermissions(2) && player.isCreative();
    }

    private static void deny(Player player, String message) {
        if (!player.level().isClientSide()) {
            player.displayClientMessage(Component.translatable(message), true);
        }
    }
}
