// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.favor.Deity;
import com.tcorigenes.tcorigenes.favor.FavorManager;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import java.lang.reflect.Method;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Libros de hechizos iniciales (diseño de la magia): el Ritualista Arcano recibe el libro de SU RAZA (Angel, Devoto, Demonio, Siervo de la Luna, Malnacido,
 * Gigante Rocoso, Ender Warrior; el Hereje, a quien los dioses ignoran, uno de magia neutra), distinto para cada raza. El Humano recibe un libro con la
 * magia del dios con el que esta a favor (el de mayor favor, desde 10 y sin empate).
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
            case "pater" -> new String[] {"electrocute", "ball_lightning", "shockwave"};
            case "deiros" -> new String[] {"firebolt", "burning_dash", "flaming_strike"};
            case "meidris" -> new String[] {"oakskin", "acid_orb", "poison_arrow"};
            case "filis" -> new String[] {"guiding_bolt", "healing_circle", "blessing_of_life"};
            case "luna" -> new String[] {"ray_of_siphoning", "heartstop", "magic_missile"};
            case "ender" -> new String[] {"magic_missile", "dragon_breath"};
            case "angel" -> new String[] {"divine_smite", "sunbeam", "wisp"};
            case "devoto" -> new String[] {"heal", "fortify", "cleanse"};
            case "demonio" -> new String[] {"fireball", "scorch", "heat_surge"};
            case "siervo" -> new String[] {"eldritch_blast", "planar_sight", "abyssal_shroud"};
            case "malnacido" -> new String[] {"blood_slash", "blood_needles", "blood_step"};
            case "gigante" -> new String[] {"stomp", "root", "earthquake"};
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

    /** Libro propio de cada raza (ninguna comparte libro con otra); null = el Humano, que recibe el del dios con mas favor. */
    private static String raceBook(Race race) {
        return switch (race) {
            case ANGEL -> "angel";
            case DEVOTO -> "devoto";
            case DEMONIO -> "demonio";
            case SIERVO_DE_LA_LUNA -> "siervo";
            case MALNACIDO -> "malnacido";
            case STONE_GIANT -> "gigante";
            case ENDER_WARRIOR -> "ender";
            case HEREJE -> "neutral";
            default -> null;
        };
    }

    private static String leader(ServerPlayer player) {
        Deity patron = FavorManager.patron(player);
        if (patron != null && patron != Deity.TEMPO) {
            return patron.name().toLowerCase(java.util.Locale.ROOT);
        }
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

    private static Method getMagicData;
    private static Method getSyncedData;
    private static Method getSpellByKey;
    private static Method needsLearning;
    private static Method isLearned;
    private static Method learn;
    private static boolean learnBroken;

    /**
     * Iron's Spellbooks obliga a "aprender" los hechizos de la escuela eldritch (la magia de Luna) antes de lanzarlos ("No puedes entender la
     * complejidad de este Hechizo"): los del libro de la raza se marcan como aprendidos, por reflexion porque el mod no es dependencia de compilacion.
     */
    private static void learnRequired(ServerPlayer player, String[] spells) {
        if (learnBroken) {
            return;
        }
        try {
            if (learn == null) {
                Class<?> magic = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                Class<?> spellType = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                Class<?> synced = Class.forName("io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData");
                getMagicData = magic.getMethod("getPlayerMagicData", LivingEntity.class);
                getSyncedData = magic.getMethod("getSyncedData");
                getSpellByKey = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry").getMethod("getSpell", ResourceLocation.class);
                needsLearning = spellType.getMethod("needsLearning");
                isLearned = synced.getMethod("isSpellLearned", spellType);
                learn = synced.getMethod("learnSpell", spellType);
            }
            Object synced = getSyncedData.invoke(getMagicData.invoke(null, player));
            for (String id : spells) {
                Object spell = getSpellByKey.invoke(null, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", id));
                if ((Boolean) needsLearning.invoke(spell) && !(Boolean) isLearned.invoke(synced, spell)) {
                    learn.invoke(synced, spell);
                }
            }
        } catch (Throwable t) {
            learnBroken = true;
        }
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
        String own = raceBook(race);
        if (own != null) {
            grant(player, own); // cada raza tiene su libro; no recibe los de los dioses
            learnRequired(player, spellsOf(own));
            return;
        }
        String god = leader(player);
        if (god != null) {
            grant(player, god); // Humano: el dios con mas favor
        }
    }
}
