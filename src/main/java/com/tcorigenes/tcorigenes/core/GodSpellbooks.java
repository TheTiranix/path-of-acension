// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.favor.Deity;
import com.tcorigenes.tcorigenes.favor.FavorManager;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Libros de hechizos de los dioses (diseño de la magia): el Ritualista Arcano recibe un libro con la magia del dios con el que esta a favor
 * (el de mayor favor, desde 10 y sin empate); el Hereje, a quien los dioses ignoran, recibe uno de magia neutra (evocacion, sin elemento).
 * Pater: rayo. Deiros: fuego. Meidris: naturaleza. Filis: sagrada (curacion). Luna: eldritch (magia oscura).
 * Es un libro de cobre de Iron's Spellbooks con los hechizos ya grabados (bloqueados) y marcado soulbound. Si el favor cambia de dueño, llega el
 * libro del nuevo dios y se conservan los anteriores. Se entrega una sola vez por libro.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class GodSpellbooks {
    private static final String GRANTED_KEY = "pa_god_books";
    private static final int MIN_FAVOR = 10;
    private static final Deity[] GODS = {Deity.PATER, Deity.MEIDRIS, Deity.FILIS, Deity.LUNA, Deity.DEIROS};

    private GodSpellbooks() {
    }

    private static String[] spellsOf(String id) {
        return switch (id) {
            case "pater" -> new String[] {"lightning_bolt", "electrocute", "charge"};
            case "deiros" -> new String[] {"firebolt", "burning_dash", "scorch"};
            case "meidris" -> new String[] {"oakskin", "root", "poison_splash"};
            case "filis" -> new String[] {"heal", "cleanse", "haste"};
            case "luna" -> new String[] {"eldritch_blast", "planar_sight", "abyssal_shroud"};
            case "ender" -> new String[] {"magic_missile", "evasion", "teleport"};
            default -> new String[] {"fang_strike", "shield", "gust"}; // neutro
        };
    }

    private static ItemStack buildBook(String id) {
        Item base = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "copper_spell_book"));
        if (base == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(base);
        CompoundTag container = new CompoundTag();
        container.put("maxSpells", IntTag.valueOf(5));
        container.put("mustEquip", ByteTag.valueOf(true));
        container.put("spellWheel", ByteTag.valueOf(true));
        ListTag data = new ListTag();
        String[] spells = spellsOf(id);
        for (int i = 0; i < spells.length; i++) {
            CompoundTag spell = new CompoundTag();
            spell.putString("id", "irons_spellbooks:" + spells[i]);
            spell.putInt("level", 1);
            spell.putInt("index", i);
            spell.putBoolean("locked", true);
            data.add(spell);
        }
        container.put("data", data);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("ISB_Spells", container);
        CompoundTag display = new CompoundTag();
        display.putString("Name", Component.Serializer.toJson(Component.translatable("tcorigenes.magic.book." + id)
                .withStyle(s -> s.withItalic(false).withBold(true))));
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("tcorigenes.magic.book.lore." + id)
                .withStyle(s -> s.withItalic(false).withColor(0xAAAAAA)))));
        display.put("Lore", lore);
        tag.put("display", display);
        SoulboundItems.markSoulbound(stack);
        return stack;
    }

    /** Libros permitidos por raza (el primero es el de la raza); null = cualquiera segun el favor. Un angel no dispara rayos ni un malnacido usa magia sagrada. */
    private static String[] allowedBooks(Race race) {
        return switch (race) {
            case ANGEL -> new String[] {"filis"};
            case DEVOTO -> new String[] {"filis", "meidris"};
            case DEMONIO -> new String[] {"deiros"};
            case SIERVO_DE_LA_LUNA -> new String[] {"luna"};
            case MALNACIDO -> new String[] {"luna"};
            case STONE_GIANT -> new String[] {"meidris"};
            case ENDER_WARRIOR -> new String[] {"ender"};
            default -> null;
        };
    }

    private static String leader(ServerPlayer player) {
        Deity best = null;
        int bestFavor = MIN_FAVOR - 1;
        boolean tie = false;
        for (Deity god : GODS) {
            int favor = FavorManager.getFavor(player, god);
            if (favor > bestFavor) {
                best = god;
                bestFavor = favor;
                tie = false;
            } else if (favor == bestFavor && best != null) {
                tie = true;
            }
        }
        return best == null || tie ? null : best.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static void grant(ServerPlayer player, String id) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag granted = persisted.getCompound(GRANTED_KEY);
        if (granted.getBoolean(id)) {
            return;
        }
        ItemStack book = buildBook(id);
        if (book.isEmpty()) {
            return;
        }
        granted.putBoolean(id, true);
        persisted.put(GRANTED_KEY, granted);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        Component name = book.getHoverName();
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        player.displayClientMessage(Component.translatable("tcorigenes.magic.book_granted", name), false);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 100 != 0
                || !ModList.get().isLoaded("irons_spellbooks")) {
            return;
        }
        boolean mage = player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(data -> data.getPlayerClass() == PlayerClass.RITUALISTA_ARCANO).orElse(false);
        if (!mage) {
            return;
        }
        Race race = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).map(info -> info.getRace()).orElse(Race.HUMANO);
        if (race == Race.AUTOMATA) {
            return; // el Autómata tiene prohibida la magia (ver AutomataMagicBan)
        }
        if (race == Race.HEREJE) {
            grant(player, "neutral");
            return;
        }
        String god = leader(player);
        String[] allowed = allowedBooks(race);
        if (allowed == null) {
            if (god != null) {
                grant(player, god); // Humano: el dios con mas favor
            }
            return;
        }
        // las razas con magia propia solo reciben libros de su afinidad; si el dios con mas favor no es de ellas, reciben el de la raza
        for (String candidate : allowed) {
            if (candidate.equals(god)) {
                grant(player, god);
                return;
            }
        }
        grant(player, allowed[0]);
    }
}
