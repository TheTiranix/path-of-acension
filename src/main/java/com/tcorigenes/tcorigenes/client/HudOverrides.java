// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.ability.PlayerAbility;
import com.tcorigenes.tcorigenes.ability.client.ClientAbilityCooldowns;
import com.tcorigenes.tcorigenes.ability.network.ActivateRacialAbilityPacket;
import com.tcorigenes.tcorigenes.core.Race;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Interfaz del jugador del pack: oscura, con marcos ornamentados bronce y oro y acentos carmesi (texturas de textures/gui/hud, hechas con
 * tools/gen_hud.py). Arriba de la barra rapida van tres filas de barras a cada lado de un rombo con el nivel: vida, hambre y sed a la izquierda;
 * mana (Iron's Spellbooks), estamina (ParCool) y experiencia a la derecha; tambien reemplaza la barra de los jefes. Reemplaza la vida, la proteccion, el hambre, la sed de Tough As Nails, la experiencia y la barra rapida de vanilla. La vida es una barra
 * que se llena segun vida actual / vida maxima con el texto "actual/maxima" (el color se elige en la pestaña "Interfaz": por defecto el
 * carmesi del pack, que late cuando queda poca). La proteccion son dos numeros: proteccion con un escudito y dureza con su icono. Se apaga
 * entero con la opcion customHud (HudConfig): vuelven los corazones y la barra de vanilla.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class HudOverrides {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/hud_icons.png");
    private static final ResourceLocation BAR = tex("bar");
    private static final ResourceLocation BOSS = tex("boss");
    private static final ResourceLocation CREST = tex("boss_crest");
    private static final ResourceLocation BADGE = tex("badge");
    private static final ResourceLocation EMBLEM = tex("emblem");
    private static final ResourceLocation EMBLEM_GEM = tex("emblem_gem");
    private static final ResourceLocation EMBLEM_GLOW = tex("emblem_glow");
    private static final ResourceLocation PLATE = tex("hotbar_plate");
    private static final ResourceLocation SLOT = tex("slot");
    private static final ResourceLocation SLOT_ON = tex("slot_selected");
    private static final ResourceLocation ICON_HEALTH = tex("icon_health");
    private static final ResourceLocation ICON_FOOD = tex("icon_food");
    private static final ResourceLocation ICON_THIRST = tex("icon_thirst");
    private static final ResourceLocation ICON_MANA = tex("icon_mana");
    private static final ResourceLocation ICON_STAMINA = tex("icon_stamina");
    private static final ResourceLocation ICON_XP = tex("icon_xp");
    private static final int FRAME_W = 92;     // marco de la barra (la textura tiene 4 px por pixel de interfaz)
    private static final int FRAME_H = 15;
    private static final int BAR_WIDTH = 86;   // hueco del marco donde se dibuja el relleno
    private static final int BAR_HEIGHT = 7;
    private static final int ROW1 = 70;        // distancia desde abajo de la pantalla al marco de cada fila de barras (3 filas, 14 px entre una y otra)
    private static final int ROW2 = 56;
    private static final int ROW3 = 42;
    private static final int CENTER_GAP = 27;  // de la mitad de la pantalla al borde interno de las barras (deja sitio a las alas del emblema)

    // paleta del pack
    private static final int EDGE = 0xFF45121A;
    private static final int EDGE_HOT = 0xFFE2242C;
    private static final int TEXT = 0xFFF0E0E0;
    private static final int CRIMSON_TOP = 0xFFC4202C;
    private static final int CRIMSON_BOTTOM = 0xFF8A1420;
    private static final int AMBER_TOP = 0xFFC98A32;
    private static final int AMBER_BOTTOM = 0xFF8A5A1E;
    private static final int WATER_TOP = 0xFF3F8CC8;
    private static final int WATER_BOTTOM = 0xFF2A64A0;
    private static final int XP_TOP = 0xFFEFCB62;
    private static final int XP_BOTTOM = 0xFFA9801F;
    private static final int STAMINA_TOP = 0xFF5CC86A;
    private static final int STAMINA_BOTTOM = 0xFF2C7A3C;
    private static final int MANA_TOP = 0xFF7A5CE8;
    private static final int MANA_BOTTOM = 0xFF3B2A9E;

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/hud/" + name + ".png");
    }

    private HudOverrides() {
    }

    // se dibujaba ocho veces el texto en cada cuadro creando el arreglo de desplazamientos de nuevo y formateando los numeros con String.format:
    // ahora el texto (y su ancho) se recalculan solo cuando cambian la vida o la armadura
    private static float cachedCurrent = Float.NaN;
    private static float cachedMax = Float.NaN;
    private static Component cachedHealthText = Component.empty();
    private static int cachedHealthWidth;
    private static double cachedArmor = Double.NaN;
    private static double cachedToughness = Double.NaN;
    private static Component cachedArmorText = Component.empty();
    private static Component cachedToughnessText = Component.empty();

    private static String number(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static Component styled(String text) {
        return Component.literal(text);
    }

    private static boolean survival(Minecraft mc) {
        return mc.gameMode != null && mc.gameMode.canHurtPlayer() && mc.getCameraEntity() instanceof LocalPlayer;
    }

    // ------------------------------------------------------------------ eventos
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui instanceof ForgeGui gui) || mc.player == null || mc.gameMode == null || !HudConfig.CUSTOM_HUD.get()) {
            return;
        }
        ResourceLocation id = event.getOverlay().id();
        GuiGraphics g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        LocalPlayer player = mc.player;

        if (id.equals(VanillaGuiOverlay.HOTBAR.id())) {
            if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR && mc.getCameraEntity() instanceof Player) {
                event.setCanceled(true);
                drawHotbar(g, mc, player, width, height);
            }
            return;
        }
        if (id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            if (mc.gameMode.hasExperience() && player.jumpableVehicle() == null) {
                event.setCanceled(true);
                drawExperience(g, mc, player, width, height);
            }
            return;
        }
        if (id.getNamespace().equals("irons_spellbooks") && id.getPath().equals("mana_overlay")) {
            event.setCanceled(true); // la barra de mana la dibuja este HUD, a la derecha del rombo
            return;
        }
        if (id.getNamespace().equals("parcool") && id.getPath().contains("stamina")) {
            event.setCanceled(true); // la estamina la dibuja este HUD, en la fila del medio a la derecha
            return;
        }
        if (!survival(mc)) {
            return;
        }
        int left = width / 2 - 91;
        int barL = width / 2 - CENTER_GAP - FRAME_W + 3;   // hueco interior de las barras de la izquierda
        int barR = width / 2 + CENTER_GAP + 3;             // y de la derecha
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
            int top = height - ROW1 + 4;
            g.drawManaged(() -> lowHealthVignette(g, player, width, height));
            // dos lotes: primero las barras y despues el texto, asi el numero queda siempre por encima aunque otro mod reordene los lotes
            g.drawManaged(() -> drawHealthBarShapes(g, player, barL, top));
            g.drawManaged(() -> drawHealthText(g, mc, player, barL, top));
            g.drawManaged(() -> drawEmblem(g, mc, player, width, height));
            if (manaMax(player) > 0) {
                g.drawManaged(() -> drawMana(g, mc, player, barR, top));
            }
            g.drawManaged(() -> drawStamina(g, mc, player, barR, height - ROW2 + 4));
            gui.leftHeight = ROW1 + 8; // lo que se apila encima (proteccion) va sobre la fila alta
        } else if (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) {
            event.setCanceled(true);
            int top = height - gui.leftHeight;
            g.drawManaged(() -> drawArmor(g, mc, player, barL - 3, top));
            gui.leftHeight += 10;
        } else if (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) && !(player.getVehicle() instanceof LivingEntity)) {
            event.setCanceled(true);
            int top = height - ROW2 + 4;
            g.drawManaged(() -> drawFood(g, mc, player, barL, top));
            gui.rightHeight = ROW1 + 8;
        } else if (id.getNamespace().equals("toughasnails") && id.getPath().contains("thirst") && !(player.getVehicle() instanceof LivingEntity)) {
            int thirst = thirstOf(player);
            if (thirst >= 0) {
                event.setCanceled(true);
                int top = height - ROW3 + 4;
                g.drawManaged(() -> drawThirst(g, mc, thirst, barL, top));
                gui.rightHeight = ROW1 + 8;
            }
        }
    }

    // ------------------------------------------------------------------ piezas
    /**
     * Barra con marco ornamentado y medallon con icono en el extremo de afuera: (x, y) es el hueco interior. Relleno con degradado, marcas cada 10 %,
     * brillo arriba y un destello que lo recorre; si "edge" no es el borde normal, un filete late en el hueco.
     */
    private static void bar(GuiGraphics g, int x, int y, int w, int h, float ratio, int top, int bottom, int edge, ResourceLocation icon) {
        g.blit(BAR, x - 3, y - 4, FRAME_W, FRAME_H, 0, 0, FRAME_W * 4, FRAME_H * 4, FRAME_W * 4, FRAME_H * 4);
        int filled = Math.round(w * Mth.clamp(ratio, 0.0F, 1.0F));
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + h, top, bottom);
            for (int i = 1; i < 10; i++) {
                int tx = x + w * i / 10;
                if (tx < x + filled) {
                    g.fill(tx, y, tx + 1, y + h, 0x26000000);
                }
            }
            g.fill(x, y, x + filled, y + 1, 0x50FFFFFF);
            g.fill(x, y + h - 1, x + filled, y + h, 0x30000000);
            int sx = x + (int) ((filled + 16) * (System.currentTimeMillis() % 2600L) / 2600.0F) - 8;
            int lo = Math.max(x, sx);
            int hi = Math.min(x + filled, sx + 5);
            if (hi > lo) {
                g.fill(lo, y, hi, y + h, 0x2CFFFFFF);
            }
        }
        if (edge != EDGE) {
            g.renderOutline(x, y, w, h, edge & 0x80FFFFFF);
        }
        boolean left = x + w / 2 < Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int bx = left ? x - 14 : x + w;
        g.blit(BADGE, bx, y - 4, 14, 14, 0, 0, 56, 56, 56, 56);
        g.blit(icon, bx + 2, y - 2, 10, 10, 0, 0, 40, 40, 40, 40);
    }

    private static void centered(GuiGraphics g, Minecraft mc, Component text, int cx, int y, int color) {
        g.drawString(mc.font, text, cx - mc.font.width(text) / 2, y, color, true);
    }

    private static int pulse(int quiet, int hot, double speed) {
        float t = (float) ((Math.sin(System.currentTimeMillis() / 1000.0 * speed) + 1.0) / 2.0);
        return lerp(quiet, hot, t);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gr = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    // ------------------------------------------------------------------ vida
    /** Con menos del 30 % de vida los bordes de la pantalla se tiñen de carmesi y laten (mas rapido bajo el 15 %). */
    private static void lowHealthVignette(GuiGraphics g, LocalPlayer player, int w, int h) {
        float ratio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (ratio > 0.3F) {
            return;
        }
        float beat = (float) ((Math.sin(System.currentTimeMillis() / 1000.0 * (ratio < 0.15F ? 7.0 : 4.0)) + 1.0) / 2.0);
        int base = (int) ((0.12F + 0.22F * beat) * (1.0F - ratio / 0.6F) * 255);
        for (int i = 0; i < 18; i++) {
            int color = ((base * (18 - i) / 18) << 24) | 0xC4202C;
            g.fill(0, i, w, i + 1, color);
            g.fill(0, h - 1 - i, w, h - i, color);
            g.fill(i, 0, i + 1, h, color);
            g.fill(w - 1 - i, 0, w - i, h, color);
        }
    }

    private static void drawHealthBarShapes(GuiGraphics g, LocalPlayer player, int x, int y) {
        float max = Math.max(1.0F, player.getMaxHealth());
        float current = Mth.clamp(player.getHealth(), 0.0F, max);
        float ratio = current / max;
        int solid = HudConfig.solidColor(HudConfig.HEALTH_COLOR.get());
        int top = solid != 0 ? solid : CRIMSON_TOP;
        int bottom = solid != 0 ? (solid & 0xFEFEFE) >> 1 | 0xFF000000 : CRIMSON_BOTTOM;
        int edge = ratio <= 0.3F ? pulse(EDGE, EDGE_HOT, 7.0) : EDGE; // poca vida: el borde late
        bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, ratio, top, bottom, edge, ICON_HEALTH);
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            int gold = Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * absorption / max));
            g.fill(x, y + BAR_HEIGHT - 2, x + gold, y + BAR_HEIGHT, 0xFFE6C040);
        }
    }

    private static void drawHealthText(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        if (!HudConfig.HEALTH_TEXT.get()) {
            return;
        }
        float max = Math.max(1.0F, player.getMaxHealth());
        float current = Mth.clamp(player.getHealth(), 0.0F, max);
        if (current != cachedCurrent || max != cachedMax) {
            cachedCurrent = current;
            cachedMax = max;
            cachedHealthText = styled(number(current) + " / " + number(max));
            cachedHealthWidth = mc.font.width(cachedHealthText);
        }
        float scale = cachedHealthWidth > BAR_WIDTH - 4 ? (BAR_WIDTH - 4) / (float) cachedHealthWidth : 1.0F;
        float tx = x + (BAR_WIDTH - cachedHealthWidth * scale) / 2.0F;
        float ty = y + (BAR_HEIGHT - 8 * scale) / 2.0F + 0.5F;
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(mc.font, cachedHealthText, 0, 0, TEXT, true);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ proteccion
    private static void drawArmor(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        double armorValue = player.getArmorValue();
        double toughnessValue = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (armorValue != cachedArmor || toughnessValue != cachedToughness) {
            cachedArmor = armorValue;
            cachedToughness = toughnessValue;
            cachedArmorText = styled(number(armorValue));
            cachedToughnessText = styled(number(toughnessValue));
        }
        g.blit(ICONS, x, y, 0, 0, 9, 9, 32, 16);
        int tx = x + 11;
        g.drawString(mc.font, cachedArmorText, tx, y + 1, TEXT, true);
        int next = tx + mc.font.width(cachedArmorText) + 8;
        g.blit(ICONS, next, y, 10, 0, 9, 9, 32, 16);
        g.drawString(mc.font, cachedToughnessText, next + 11, y + 1, 0xFF9AE9F5, true);
    }

    // ------------------------------------------------------------------ hambre y sed
    private static void drawFood(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        int food = player.getFoodData().getFoodLevel();
        boolean starving = player.hasEffect(MobEffects.HUNGER);
        int edge = food <= 6 ? pulse(EDGE, EDGE_HOT, 5.0) : EDGE;
        bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, food / 20.0F, starving ? 0xFF8FA040 : AMBER_TOP, starving ? 0xFF4A5A1C : AMBER_BOTTOM, edge, ICON_FOOD);
        float saturation = Math.min(player.getFoodData().getSaturationLevel(), food);
        if (saturation > 0.0F) {
            g.fill(x, y + BAR_HEIGHT - 2, x + Math.round(BAR_WIDTH * saturation / 20.0F), y + BAR_HEIGHT, 0xFFF5E2A8);
        }
        centered(g, mc, styled(Integer.toString(food)), x + BAR_WIDTH / 2, y, TEXT);
    }

    private static void drawThirst(GuiGraphics g, Minecraft mc, int thirst, int x, int y) {
        int edge = thirst <= 6 ? pulse(EDGE, EDGE_HOT, 5.0) : EDGE;
        bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, thirst / 20.0F, WATER_TOP, WATER_BOTTOM, edge, ICON_THIRST);
        centered(g, mc, styled(Integer.toString(thirst)), x + BAR_WIDTH / 2, y, TEXT);
    }

    private static Method thirstHelper;
    private static Method thirstGetter;
    private static boolean thirstMissing;

    /** Sed de Tough As Nails (0 a 20), o -1 si el mod no esta o cambio su API (en ese caso se deja su barra como viene). Se busca por reflexion. */
    private static int thirstOf(Player player) {
        if (thirstMissing) {
            return -1;
        }
        try {
            if (thirstHelper == null) {
                thirstHelper = Class.forName("toughasnails.api.thirst.ThirstHelper").getMethod("getThirst", Player.class);
                thirstGetter = Class.forName("toughasnails.api.thirst.IThirst").getMethod("getThirst");
            }
            return (Integer) thirstGetter.invoke(thirstHelper.invoke(null, player));
        } catch (Throwable t) {
            thirstMissing = true;
            return -1;
        }
    }

    // ------------------------------------------------------------------ emblema central: icono de la raza + cooldown de su habilidad racial
    /** Color de acento de cada raza (RGB): tine el resplandor, las gemas del emblema y el realce de la casilla elegida. */
    private static int accentOf(Race race) {
        return switch (race) {
            case ANGEL -> 0xFFF2B0;
            case DEVOTO -> 0xFFE08A;
            case DEMONIO -> 0xE02828;
            case SIERVO_DE_LA_LUNA -> 0x5A82FF;
            case MALNACIDO -> 0xB01040;
            case STONE_GIANT -> 0xB89060;
            case ENDER_WARRIOR -> 0xB050F0;
            case AUTOMATA -> 0x40D8E0;
            case HEREJE -> 0xA8A8A8;
            default -> 0xE0B050;
        };
    }

    private static void tint(GuiGraphics g, int rgb, float alpha) {
        g.setColor(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F, alpha);
    }

    private static String clock(int millis) {
        int seconds = (millis + 999) / 1000;
        return seconds >= 60 ? (seconds / 60) + ":" + String.format(Locale.ROOT, "%02d", seconds % 60) : Integer.toString(seconds);
    }

    /**
     * Rombo del centro: el icono de la raza elegida. Si la raza tiene habilidad racial, el rombo es su indicador de cooldown (una cortina oscura
     * que baja del tope y se va recogiendo, con el tiempo que falta) y late cuando esta lista.
     */
    private static void drawEmblem(GuiGraphics g, Minecraft mc, LocalPlayer player, int width, int height) {
        int cx = width / 2;
        int cy = height - 48; // centro de la fila del medio
        Race race = ClientRaceData.get(player.getUUID());
        int accent = accentOf(race);
        String abilityId = ActivateRacialAbilityPacket.RACE_ABILITIES.get(race);
        PlayerAbility ability = abilityId == null ? null : AbilityRegistry.get(abilityId);
        int remaining = ability == null ? 0 : ClientAbilityCooldowns.getRemainingMillis(abilityId);
        float beat = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 1000.0 * 2.2);
        boolean ready = ability != null && remaining <= 0;

        tint(g, accent, ready ? 0.45F + 0.35F * beat : 0.26F + 0.10F * beat);
        g.blit(EMBLEM_GLOW, cx - 28, cy - 28, 56, 56, 0, 0, 224, 224, 224, 224);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(EMBLEM, cx - 24, cy - 22, 48, 44, 0, 0, 192, 176, 192, 176);
        g.blit(raceIcon(race), cx - 8, cy - 8, 16, 16, 0, 0, 32, 32, 32, 32);
        if (remaining > 0) {
            float left = Mth.clamp(remaining / (ability.cooldownTicks() * 50.0F), 0.0F, 1.0F);
            int covered = Math.round(24 * left);
            for (int i = 0; i < covered; i++) { // la cortina se recorta al hueco del rombo (media diagonal 12)
                int half = (int) (12 - Math.abs(i + 0.5F - 12));
                g.fill(cx - half, cy - 12 + i, cx + half, cy - 11 + i, 0xB4000000);
            }
            centered(g, mc, styled(clock(remaining)), cx, cy - 4, 0xFFFFFFFF);
        }
        tint(g, accent, 1.0F);
        g.blit(EMBLEM_GEM, cx - 24, cy - 22, 48, 44, 0, 0, 192, 176, 192, 176);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation raceIcon(Race race) {
        String name = switch (race) {
            case SIERVO_DE_LA_LUNA -> "siervo";
            case ENDER_WARRIOR -> "ender";
            default -> race.name().toLowerCase(Locale.ROOT);
        };
        return ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/origin/" + name + ".png");
    }

    private static void drawExperience(GuiGraphics g, Minecraft mc, LocalPlayer player, int width, int height) {
        int x = width / 2 + CENTER_GAP + 3;
        int y = height - ROW3 + 4;
        g.drawManaged(() -> bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, player.experienceProgress, XP_TOP, XP_BOTTOM, EDGE, ICON_XP));
        g.drawManaged(() -> centered(g, mc, styled(Integer.toString(player.experienceLevel)), x + BAR_WIDTH / 2, y, 0xFFFFF0C0));
    }

    // ------------------------------------------------------------------ mana (Iron's Spellbooks, por reflexion)
    private static Method manaGetter;
    private static boolean manaMissing;

    private static double manaMax(Player player) {
        if (manaMissing || !ModList.get().isLoaded("irons_spellbooks")) {
            return 0;
        }
        var attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "max_mana"));
        return attribute == null ? 0 : player.getAttributeValue(attribute);
    }

    private static void drawMana(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        int mana;
        try {
            if (manaGetter == null) {
                manaGetter = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData").getMethod("getPlayerMana");
            }
            mana = (Integer) manaGetter.invoke(null);
        } catch (Throwable t) {
            manaMissing = true;
            return;
        }
        double max = manaMax(player);
        bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, (float) (mana / max), MANA_TOP, MANA_BOTTOM, EDGE, ICON_MANA);
        centered(g, mc, styled(mana + " / " + (int) max), x + BAR_WIDTH / 2, y, TEXT);
    }

    // ------------------------------------------------------------------ estamina (ParCool, por reflexion)
    private static Method staminaOf;
    private static Method staminaValue;
    private static Method staminaMax;
    private static Method staminaExhausted;
    private static boolean staminaMissing;

    private static void drawStamina(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        if (staminaMissing || !ModList.get().isLoaded("parcool")) {
            return;
        }
        int value;
        int max;
        boolean exhausted;
        try {
            if (staminaOf == null) {
                Class<?> type = Class.forName("com.alrex.parcool.common.capability.IStamina");
                staminaValue = type.getMethod("get");
                staminaMax = type.getMethod("getActualMaxStamina");
                staminaExhausted = type.getMethod("isExhausted");
                staminaOf = type.getMethod("get", Player.class);
            }
            Object stamina = staminaOf.invoke(null, player);
            if (stamina == null) {
                return;
            }
            value = (Integer) staminaValue.invoke(stamina);
            max = (Integer) staminaMax.invoke(stamina);
            exhausted = (Boolean) staminaExhausted.invoke(stamina);
        } catch (Throwable t) {
            staminaMissing = true;
            return;
        }
        if (max <= 0) {
            return;
        }
        int edge = exhausted ? pulse(EDGE, EDGE_HOT, 6.0) : EDGE;
        bar(g, x, y, BAR_WIDTH, BAR_HEIGHT, value / (float) max, exhausted ? 0xFF8A8A8A : STAMINA_TOP, exhausted ? 0xFF4A4A4A : STAMINA_BOTTOM, edge, ICON_STAMINA);
        centered(g, mc, styled(Integer.toString(value)), x + BAR_WIDTH / 2, y, TEXT);
    }

    // ------------------------------------------------------------------ barra de los jefes
    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!HudConfig.CUSTOM_HUD.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        BossEvent boss = event.getBossEvent();
        int x = event.getWindow().getGuiScaledWidth() / 2 - 95;
        int y = event.getY();
        event.setCanceled(true);
        int[] colors = bossColors(boss.getColor());
        g.drawManaged(() -> {
            g.blit(BOSS, x, y + 8, 190, 15, 0, 0, 760, 60, 760, 60);
            int filled = Math.round(182 * Mth.clamp(boss.getProgress(), 0.0F, 1.0F));
            if (filled > 0) {
                g.fillGradient(x + 4, y + 12, x + 4 + filled, y + 19, colors[0], colors[1]);
                g.fill(x + 4, y + 12, x + 4 + filled, y + 13, 0x50FFFFFF);
                int sx = x + 4 + (int) ((filled + 16) * (System.currentTimeMillis() % 3200L) / 3200.0F) - 8;
                int lo = Math.max(x + 4, sx);
                int hi = Math.min(x + 4 + filled, sx + 6);
                if (hi > lo) {
                    g.fill(lo, y + 12, hi, y + 19, 0x2CFFFFFF);
                }
            }
            g.blit(CREST, x + 95 - 22, y, 44, 18, 0, 0, 176, 72, 176, 72);
            Component name = boss.getName();
            g.drawString(mc.font, name, x + 95 - mc.font.width(name) / 2, y - 9, 0xFFF0E0C0, true);
        });
        event.setIncrement(38);
    }

    private static int[] bossColors(BossEvent.BossBarColor color) {
        return switch (color) {
            case RED -> new int[] {CRIMSON_TOP, CRIMSON_BOTTOM};
            case BLUE -> new int[] {WATER_TOP, WATER_BOTTOM};
            case GREEN -> new int[] {0xFF4FB85A, 0xFF2A7A36};
            case YELLOW -> new int[] {XP_TOP, XP_BOTTOM};
            case PURPLE -> new int[] {MANA_TOP, MANA_BOTTOM};
            case PINK -> new int[] {0xFFE070B0, 0xFF9A3A78};
            default -> new int[] {0xFFE8E8E8, 0xFF9A9A9A};
        };
    }

    // ------------------------------------------------------------------ barra rapida
    private static void drawHotbar(GuiGraphics g, Minecraft mc, LocalPlayer player, int width, int height) {
        int x0 = width / 2 - 91;
        int y0 = height - 22;
        int selected = player.getInventory().selected;
        ItemStack off = player.getOffhandItem();
        int ox = player.getMainArm().getOpposite() == HumanoidArm.LEFT ? x0 - 33 : x0 + 195;
        int accent = accentOf(ClientRaceData.get(player.getUUID()));
        float beat = 0.8F + 0.2F * (float) Math.sin(System.currentTimeMillis() / 1000.0 * 4.0);
        g.drawManaged(() -> {
            g.blit(PLATE, x0 - 9, y0 - 2, 200, 28, 0, 0, 800, 112, 800, 112);
            for (int i = 0; i < 9; i++) {
                g.blit(SLOT, x0 + i * 20, y0 + 1, 20, 20, 0, 0, 80, 80, 80, 80);
            }
            if (!off.isEmpty()) {
                g.blit(SLOT, ox + 1, y0 + 1, 20, 20, 0, 0, 80, 80, 80, 80);
            }
            tint(g, accent, beat);
            g.blit(SLOT_ON, x0 + selected * 20 - 2, y0 - 1, 24, 24, 0, 0, 96, 96, 96, 96);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        });
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            g.renderItem(stack, x0 + 2 + i * 20, y0 + 3);
            g.renderItemDecorations(mc.font, stack, x0 + 2 + i * 20, y0 + 3);
        }
        if (!off.isEmpty()) {
            g.renderItem(off, ox + 3, y0 + 3);
            g.renderItemDecorations(mc.font, off, ox + 3, y0 + 3);
        }
    }
}
