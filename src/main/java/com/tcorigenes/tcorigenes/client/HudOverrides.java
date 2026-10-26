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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Interfaz del jugador del pack: marcos finos y translucidos rojos (texturas de textures/gui/hud, hechas con
 * tools/gen_hud.py). Sobre la barra rapida van dos filas de barras a cada lado de un rombo chico, separadas de el: vida y
 * hambre (con la sed de Tough As Nails al lado) a la izquierda; mana (Iron's Spellbooks) y estamina (ParCool) a la derecha. El rombo lleva el
 * icono de la raza y, si tiene habilidad racial, hace de indicador de su cooldown. La experiencia es una linea fina sobre la barra rapida con
 * el nivel al lado. Tambien reemplaza la proteccion (dos numeros), y la barra rapida con casillas sueltas (la barra de los jefes queda la de vanilla). La vida se
 * llena segun vida actual / maxima (el color se elige en la pestaña "Interfaz" y late cuando queda poca). Se apaga entero con la opcion
 * customHud (HudConfig): vuelven los corazones y la barra de vanilla.
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class HudOverrides {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/hud_icons.png");
    private static final ResourceLocation VANILLA_ICONS = ResourceLocation.withDefaultNamespace("textures/gui/icons.png"); // las burbujas de aire
    private static final ResourceLocation BAR = tex("bar");
    private static final ResourceLocation BAR_HALF = tex("bar_half");
    private static final ResourceLocation EMBLEM = tex("emblem");
    private static final ResourceLocation EMBLEM_GEM = tex("emblem_gem");
    private static final ResourceLocation EMBLEM_GLOW = tex("emblem_glow");
    private static final ResourceLocation SLOT = tex("slot");
    private static final ResourceLocation SLOT_ON = tex("slot_selected");
    private static final ResourceLocation SLOT_BIG = tex("slot_big");
    private static final ResourceLocation ICON_HEALTH = tex("icon_health");
    private static final ResourceLocation ICON_FOOD = tex("icon_food");
    private static final ResourceLocation ICON_THIRST = tex("icon_thirst");
    private static final ResourceLocation ICON_MANA = tex("icon_mana");
    private static final ResourceLocation ICON_STAMINA = tex("icon_stamina");
    private static final ResourceLocation DIGITS = tex("digits");
    private static final String GLYPHS = "0123456789/:.";   // orden de las celdas de digits.png (7x9 px de interfaz cada una)

    private static final int FRAME_W = 92;     // marco de la barra; las texturas tienen 4 px por pixel de interfaz
    private static final int FRAME_H = 11;
    private static final int BAR_WIDTH = 86;   // hueco del marco donde se dibuja el relleno (3 px de marco a cada lado, 2 arriba y abajo)
    private static final int HALF_WIDTH = 39;  // hueco de la media barra (hambre y sed comparten la fila)
    private static final int BAR_HEIGHT = 7;
    private static final int ROW1 = 50;        // distancia desde abajo de la pantalla al marco de la fila alta
    private static final int ROW2 = 37;        // idem, fila baja
    private static final int CENTER_GAP = 19;  // de la mitad de la pantalla al borde interno de cada marco (el rombo mide 15 de media diagonal)

    // paleta
    private static final int EDGE = 0xFF2A3A4C;
    private static final int EDGE_HOT = 0xFFFF4458;
    private static final int TEXT = 0xFFFFEEEE;
    private static final int HEALTH_TOP = 0xFFF0586A;
    private static final int HEALTH_BOTTOM = 0xFFB02440;
    private static final int FOOD_TOP = 0xFFF0B050;
    private static final int FOOD_BOTTOM = 0xFFB87828;
    private static final int WATER_TOP = 0xFF58C8F8;
    private static final int WATER_BOTTOM = 0xFF2C80C8;
    private static final int STAMINA_TOP = 0xFF60E0A8;
    private static final int STAMINA_BOTTOM = 0xFF2C9870;
    private static final int MANA_TOP = 0xFFA080FF;
    private static final int MANA_BOTTOM = 0xFF5038D0;
    private static final int XP_TOP = 0xFFFFE890;
    private static final int XP_BOTTOM = 0xFFC8A040;

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("tcorigenes", "textures/gui/hud/" + name + ".png");
    }

    private HudOverrides() {
    }

    // el texto de la vida y de la armadura se recalcula solo cuando cambian los valores
    private static float cachedCurrent = Float.NaN;
    private static float cachedMax = Float.NaN;
    private static String cachedHealthText = "";
    private static double cachedArmor = Double.NaN;
    private static double cachedToughness = Double.NaN;
    private static String cachedArmorText = "";
    private static String cachedToughnessText = "";

    // los numeros de las barras se acumulan y se dibujan en un segundo lote, asi quedan siempre por encima de formas y texturas
    private static final int MAX_LABELS = 8;
    private static final String[] LABEL_TEXT = new String[MAX_LABELS];
    private static final int[] LABEL_X = new int[MAX_LABELS];
    private static final int[] LABEL_Y = new int[MAX_LABELS];
    private static final int[] LABEL_COLOR = new int[MAX_LABELS];
    private static int labels;

    private static String number(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static boolean survival(Minecraft mc) {
        return mc.gameMode != null && mc.gameMode.canHurtPlayer() && mc.getCameraEntity() instanceof LocalPlayer;
    }

    private static void label(String text, int centerX, int y, int color) {
        if (labels < MAX_LABELS) {
            LABEL_TEXT[labels] = text;
            LABEL_X[labels] = centerX;
            LABEL_Y[labels] = y;
            LABEL_COLOR[labels] = color;
            labels++;
        }
    }

    private static float advance(char c) {
        return c >= '0' && c <= '9' ? 6.0F : c == '/' ? 5.0F : 3.0F; // los digitos, la barra, y ':' '.' ' '
    }

    private static float glyphWidth(String text) {
        float width = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            width += advance(text.charAt(i));
        }
        return width;
    }

    /**
     * Numeros con la textura digits.png (glifos angulares de aspecto antiguo, con contorno oscuro), alineados a la izquierda en x. La altura
     * de la celda es 9; "y" es el tope de un hueco de barra (7 de alto), asi que el numero queda centrado en el.
     */
    private static void glyphs(GuiGraphics g, String text, float x, float y, int color, float scale) {
        tint(g, color & 0xFFFFFF, 1.0F);
        g.pose().pushPose();
        g.pose().translate(x, y + (BAR_HEIGHT - 9 * scale) / 2.0F, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        float cursor = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int cell = GLYPHS.indexOf(c);
            if (cell >= 0) {
                g.blit(DIGITS, Math.round(cursor), 0, 7, 9, cell * 28, 0, 28, 36, 364, 36);
            }
            cursor += advance(c);
        }
        g.pose().popPose();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Igual, centrado en cx; si no cabe en 70 px se achica. */
    private static void centeredGlyphs(GuiGraphics g, String text, float cx, float y, int color, float maxScale) {
        float width = glyphWidth(text);
        float scale = Math.min(maxScale, 70.0F / Math.max(1.0F, width));
        glyphs(g, text, cx - width * scale / 2.0F, y, color, scale);
    }

    private static void drawLabels(GuiGraphics g) {
        for (int i = 0; i < labels; i++) {
            centeredGlyphs(g, LABEL_TEXT[i], LABEL_X[i], LABEL_Y[i], LABEL_COLOR[i], 0.85F);
        }
        labels = 0;
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
                drawExperience(g, player, width, height);
            }
            return;
        }
        if (id.getNamespace().equals("irons_spellbooks") && id.getPath().equals("mana_overlay")) {
            event.setCanceled(true); // la barra de mana la dibuja este HUD, arriba a la derecha del rombo
            return;
        }
        if (id.getNamespace().equals("parcool") && id.getPath().contains("stamina")) {
            event.setCanceled(true); // la estamina la dibuja este HUD, abajo a la derecha del rombo
            return;
        }
        if (!survival(mc)) {
            return;
        }
        boolean riding = player.getVehicle() instanceof LivingEntity;
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
            // todas las barras y el rombo van en el mismo evento: el rombo se dibuja ultimo y tapa los extremos de los marcos
            g.drawManaged(() -> drawBars(g, player, width, height, riding));
            g.drawManaged(() -> drawLabels(g));
            gui.leftHeight = ROW1 + 8; // lo que se apila encima (proteccion) va sobre la fila alta
        } else if (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) {
            event.setCanceled(true);
            int top = height - gui.leftHeight;
            g.drawManaged(() -> drawArmor(g, player, width / 2 - CENTER_GAP - FRAME_W, top));
            gui.leftHeight += 10;
        } else if (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) && !riding) {
            event.setCanceled(true); // ya la dibujo drawBars
            gui.rightHeight = ROW1 + 8;
        } else if (id.getNamespace().equals("toughasnails") && id.getPath().contains("thirst") && !riding && thirstOf(player) >= 0) {
            event.setCanceled(true); // idem
            gui.rightHeight = ROW1 + 8;
        }
    }

    /**
     * Tough As Nails (9.x) no registra una capa de sed: GlitchCore envuelve la capa del aire y TAN dibuja sus gotas ahi dentro. Como la sed ya
     * tiene su barra, se cancela la capa del aire antes que nadie (prioridad maxima) y las burbujas de vanilla se dibujan a mano (misma cuenta
     * que Gui.renderAir), sobre las filas de barras.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAirOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id()) || !(mc.gui instanceof ForgeGui gui) || mc.player == null
                || !HudConfig.CUSTOM_HUD.get() || !survival(mc) || mc.player.getVehicle() instanceof LivingEntity || thirstOf(mc.player) < 0) {
            return;
        }
        event.setCanceled(true);
        int air = mc.player.getAirSupply();
        int max = mc.player.getMaxAirSupply();
        if (mc.player.isEyeInFluid(FluidTags.WATER) || air < max) {
            int y = event.getWindow().getGuiScaledHeight() - gui.rightHeight;
            int x = event.getWindow().getGuiScaledWidth() / 2 + 91;
            gui.rightHeight += 10;
            int full = Mth.ceil((air - 2) * 10.0 / max);
            int shown = Mth.ceil(air * 10.0 / max); // las "completas" y la ultima a medias (la que revienta)
            for (int i = 0; i < shown; i++) {
                event.getGuiGraphics().blit(VANILLA_ICONS, x - i * 8 - 9, y, i < full ? 16 : 25, 18, 9, 9);
            }
        }
    }

    private static void drawBars(GuiGraphics g, LocalPlayer player, int width, int height, boolean riding) {
        int cx = width / 2;
        int barL = cx - CENTER_GAP - FRAME_W + 3;   // hueco interior de las barras de la izquierda
        int barR = cx + CENTER_GAP + 3;             // y de la derecha
        int row1 = height - ROW1 + 2;
        int row2 = height - ROW2 + 2;
        lowHealthVignette(g, player, width, height);
        drawHealth(g, player, barL, row1);
        if (!riding) {
            drawFood(g, player, barL, row2, thirstOf(player));
        }
        if (manaMax(player) > 0) {
            drawMana(g, player, barR, row1);
        }
        drawStamina(g, player, barR, row2);
        drawEmblem(g, player, cx, height - 38);
    }

    // ------------------------------------------------------------------ piezas
    /**
     * Barra con marco fino: (x, y) es el hueco interior. Relleno con degradado y brillo arriba (las de la izquierda se llenan hacia el rombo, las de
     * la derecha tambien), un destello lo recorre y un icono chico va en el extremo de afuera; si "edge" no es el borde normal, un filete late.
     */
    private static void bar(GuiGraphics g, int x, int y, int w, float ratio, int top, int bottom, int edge, ResourceLocation icon, boolean left) {
        int fw = w + 6;
        g.blit(w == HALF_WIDTH ? BAR_HALF : BAR, x - 3, y - 2, fw, FRAME_H, 0, 0, fw * 4, FRAME_H * 4, fw * 4, FRAME_H * 4);
        int filled = Math.round(w * Mth.clamp(ratio, 0.0F, 1.0F));
        if (filled > 0) {
            int x0 = left ? x : x + w - filled;
            int x1 = x0 + filled;
            g.fillGradient(x0, y, x1, y + BAR_HEIGHT, top, bottom);
            g.fill(x0, y, x1, y + 3, 0x24FFFFFF);
            g.fill(x0, y, x1, y + 1, 0x44FFFFFF);
            int sx = x0 + (int) ((filled + 16) * (System.currentTimeMillis() % 2600L) / 2600.0F) - 8;
            int lo = Math.max(x0, sx);
            int hi = Math.min(x1, sx + 5);
            if (hi > lo) {
                g.fill(lo, y, hi, y + BAR_HEIGHT, 0x2CFFFFFF);
            }
        }
        if (edge != EDGE) {
            g.renderOutline(x, y, w, BAR_HEIGHT, edge & 0x80FFFFFF);
        }
        g.flush(); // las formas van en lote; sin esto el icono (textura) quedaria debajo del relleno
        g.blit(icon, left ? x + 1 : x + w - 8, y, 7, 7, 0, 0, 40, 40, 40, 40);
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

    private static void drawHealth(GuiGraphics g, LocalPlayer player, int x, int y) {
        float max = Math.max(1.0F, player.getMaxHealth());
        float current = Mth.clamp(player.getHealth(), 0.0F, max);
        float ratio = current / max;
        int solid = HudConfig.solidColor(HudConfig.HEALTH_COLOR.get());
        int top = solid != 0 ? solid : HEALTH_TOP;
        int bottom = solid != 0 ? (solid & 0xFEFEFE) >> 1 | 0xFF000000 : HEALTH_BOTTOM;
        int edge = ratio <= 0.3F ? pulse(EDGE, EDGE_HOT, 7.0) : EDGE; // poca vida: el borde late
        bar(g, x, y, BAR_WIDTH, ratio, top, bottom, edge, ICON_HEALTH, true);
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            int gold = Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * absorption / max));
            g.fill(x, y + BAR_HEIGHT - 2, x + gold, y + BAR_HEIGHT, 0xFFE6C040);
        }
        if (HudConfig.HEALTH_TEXT.get()) {
            if (current != cachedCurrent || max != cachedMax) {
                cachedCurrent = current;
                cachedMax = max;
                cachedHealthText = number(current) + " / " + number(max);
            }
            label(cachedHealthText, x + BAR_WIDTH / 2 + 3, y, TEXT);
        }
    }

    // ------------------------------------------------------------------ proteccion
    private static void drawArmor(GuiGraphics g, LocalPlayer player, int x, int y) {
        double armorValue = player.getArmorValue();
        double toughnessValue = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (armorValue != cachedArmor || toughnessValue != cachedToughness) {
            cachedArmor = armorValue;
            cachedToughness = toughnessValue;
            cachedArmorText = number(armorValue);
            cachedToughnessText = number(toughnessValue);
        }
        g.blit(ICONS, x, y, 0, 0, 9, 9, 32, 16);
        int tx = x + 11;
        glyphs(g, cachedArmorText, tx, y + 1, TEXT, 0.9F);
        int next = tx + (int) (glyphWidth(cachedArmorText) * 0.9F) + 6;
        g.blit(ICONS, next, y, 10, 0, 9, 9, 32, 16);
        glyphs(g, cachedToughnessText, next + 11, y + 1, 0xFF9AE9F5, 0.9F);
    }

    // ------------------------------------------------------------------ hambre y sed (comparten la fila baja de la izquierda)
    private static void drawFood(GuiGraphics g, LocalPlayer player, int x, int y, int thirst) {
        int food = player.getFoodData().getFoodLevel();
        boolean starving = player.hasEffect(MobEffects.HUNGER);
        int edge = food <= 6 ? pulse(EDGE, EDGE_HOT, 5.0) : EDGE;
        int w = thirst >= 0 ? HALF_WIDTH : BAR_WIDTH;
        bar(g, x, y, w, food / 20.0F, starving ? 0xFF8FA040 : FOOD_TOP, starving ? 0xFF4A5A1C : FOOD_BOTTOM, edge, ICON_FOOD, true);
        float saturation = Math.min(player.getFoodData().getSaturationLevel(), food);
        if (saturation > 0.0F) {
            g.fill(x, y + BAR_HEIGHT - 2, x + Math.round(w * saturation / 20.0F), y + BAR_HEIGHT, 0xFFF5E2A8);
        }
        label(Integer.toString(food), x + w / 2 + 3, y, TEXT);
        if (thirst >= 0) {
            int tx = x + HALF_WIDTH + 8;
            int thirstEdge = thirst <= 6 ? pulse(EDGE, EDGE_HOT, 5.0) : EDGE;
            bar(g, tx, y, HALF_WIDTH, thirst / 20.0F, WATER_TOP, WATER_BOTTOM, thirstEdge, ICON_THIRST, true);
            label(Integer.toString(thirst), tx + HALF_WIDTH / 2 + 3, y, TEXT);
        }
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
    /** Color de acento de cada raza (RGB): tine el resplandor, las gemas del rombo y el realce de la casilla elegida. */
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
            default -> 0xD42A40;
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
    private static void drawEmblem(GuiGraphics g, LocalPlayer player, int cx, int cy) {
        Race race = ClientRaceData.get(player.getUUID());
        int accent = accentOf(race);
        String abilityId = ActivateRacialAbilityPacket.RACE_ABILITIES.get(race);
        PlayerAbility ability = abilityId == null ? null : AbilityRegistry.get(abilityId);
        int remaining = ability == null ? 0 : ClientAbilityCooldowns.getRemainingMillis(abilityId);
        float beat = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 1000.0 * 2.2);
        boolean ready = ability != null && remaining <= 0;

        g.flush(); // las formas de las barras van en lote: salen antes de que el rombo las tape
        tint(g, accent, ready ? 0.45F + 0.35F * beat : 0.24F + 0.10F * beat);
        g.blit(EMBLEM_GLOW, cx - 22, cy - 22, 44, 44, 0, 0, 176, 176, 176, 176);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(EMBLEM, cx - 15, cy - 15, 30, 30, 0, 0, 120, 120, 120, 120);
        g.blit(raceIcon(race), cx - 8, cy - 8, 16, 16, 0, 0, 32, 32, 32, 32);
        if (remaining > 0) {
            float left = Mth.clamp(remaining / (ability.cooldownTicks() * 50.0F), 0.0F, 1.0F);
            int covered = Math.round(22 * left);
            for (int i = 0; i < covered; i++) { // la cortina se recorta al hueco del rombo (media diagonal 11)
                int half = (int) (11 - Math.abs(i + 0.5F - 11));
                g.fill(cx - half, cy - 11 + i, cx + half, cy - 10 + i, 0xB4000000);
            }
            label(clock(remaining), cx, cy - 4, 0xFFFFFFFF);
        }
        g.flush();
        tint(g, accent, 1.0F);
        g.blit(EMBLEM_GEM, cx - 15, cy - 15, 30, 30, 0, 0, 120, 120, 120, 120);
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

    // ------------------------------------------------------------------ experiencia: linea fina sobre la barra rapida, con el nivel a la derecha
    private static void drawExperience(GuiGraphics g, LocalPlayer player, int width, int height) {
        int x = width / 2 - 91;
        int y = height - 24;
        g.drawManaged(() -> {
            g.fill(x - 1, y - 1, x + 171, y + 3, 0xA0060A12);
            int filled = Math.round(170 * Mth.clamp(player.experienceProgress, 0.0F, 1.0F));
            if (filled > 0) {
                g.fillGradient(x, y, x + filled, y + 2, XP_TOP, XP_BOTTOM);
            }
        });
        String level = Integer.toString(player.experienceLevel);
        g.drawManaged(() -> glyphs(g, level, x + 182 - glyphWidth(level) * 0.7F, y - 3, 0xFFFFF0C0, 0.7F));
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

    private static void drawMana(GuiGraphics g, LocalPlayer player, int x, int y) {
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
        bar(g, x, y, BAR_WIDTH, (float) (mana / max), MANA_TOP, MANA_BOTTOM, EDGE, ICON_MANA, false);
        label(mana + " / " + (int) max, x + BAR_WIDTH / 2 - 3, y, TEXT);
    }

    // ------------------------------------------------------------------ estamina (ParCool, por reflexion)
    private static Method staminaOf;
    private static Method staminaValue;
    private static Method staminaMax;
    private static Method staminaExhausted;
    private static boolean staminaMissing;

    private static void drawStamina(GuiGraphics g, LocalPlayer player, int x, int y) {
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
        bar(g, x, y, BAR_WIDTH, value / (float) max, exhausted ? 0xFF8A8A8A : STAMINA_TOP, exhausted ? 0xFF4A4A4A : STAMINA_BOTTOM, edge, ICON_STAMINA, false);
        label(Integer.toString(value), x + BAR_WIDTH / 2 - 3, y, TEXT);
    }

    // ------------------------------------------------------------------ barra rapida: casillas sueltas y la mano secundaria aparte
    private static void drawHotbar(GuiGraphics g, Minecraft mc, LocalPlayer player, int width, int height) {
        int x0 = width / 2 - 91;
        int y0 = height - 22;
        int selected = player.getInventory().selected;
        ItemStack off = player.getOffhandItem();
        int ox = player.getMainArm().getOpposite() == HumanoidArm.LEFT ? x0 - 30 : x0 + 188;
        int accent = accentOf(ClientRaceData.get(player.getUUID()));
        float beat = 0.8F + 0.2F * (float) Math.sin(System.currentTimeMillis() / 1000.0 * 4.0);
        g.drawManaged(() -> {
            for (int i = 0; i < 9; i++) {
                g.blit(SLOT, x0 + i * 20 + 1, y0 + 2, 18, 18, 0, 0, 72, 72, 72, 72);
            }
            g.blit(SLOT_BIG, ox, y0 - 1, 24, 24, 0, 0, 96, 96, 96, 96);
            tint(g, accent, beat);
            g.blit(SLOT_ON, x0 + selected * 20 - 1, y0, 22, 22, 0, 0, 88, 88, 88, 88);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        });
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            g.renderItem(stack, x0 + 2 + i * 20, y0 + 3);
            g.renderItemDecorations(mc.font, stack, x0 + 2 + i * 20, y0 + 3);
        }
        if (!off.isEmpty()) {
            g.renderItem(off, ox + 4, y0 + 3);
            g.renderItemDecorations(mc.font, off, ox + 4, y0 + 3);
        }
    }
}
