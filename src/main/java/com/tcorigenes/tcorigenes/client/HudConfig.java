// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Opciones de interfaz del jugador (personalizacion): se cambian desde la pestaña "Interfaz" de la pantalla de perfil y se guardan en
 * config/tcorigenes-client.toml. Son solo del cliente: no afectan al servidor ni a otros jugadores.
 */
public final class HudConfig {
    /** Colores fijos de la barra de vida; "gradient" es el verde-a-rojo segun la vida que queda. */
    public static final List<String> HEALTH_COLORS = List.of("gradient", "red", "green", "blue", "gold", "purple");

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_HUD;
    public static final ForgeConfigSpec.BooleanValue HEALTH_TEXT;
    public static final ForgeConfigSpec.ConfigValue<String> HEALTH_COLOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CUSTOM_HUD = builder.comment("Usa la barra de vida y la fila de proteccion del pack (false = corazones y armadura de vanilla)")
                .define("customHud", true);
        HEALTH_TEXT = builder.comment("Muestra el numero 'actual/maxima' sobre la barra de vida")
                .define("healthText", true);
        HEALTH_COLOR = builder.comment("Color de la barra de vida: gradient, red, green, blue, gold o purple")
                .define("healthColor", "gradient", value -> value instanceof String s && HEALTH_COLORS.contains(s));
        SPEC = builder.build();
    }

    private HudConfig() {
    }

    /** Color ARGB fijo para el nombre de color dado, o 0 si es el degradado. */
    public static int solidColor(String name) {
        return switch (name) {
            case "red" -> 0xFFE03030;
            case "green" -> 0xFF3FC84A;
            case "blue" -> 0xFF3A7BE0;
            case "gold" -> 0xFFE0B030;
            case "purple" -> 0xFF9A4AE0;
            default -> 0;
        };
    }

    public static String nextHealthColor() {
        int i = HEALTH_COLORS.indexOf(HEALTH_COLOR.get());
        String next = HEALTH_COLORS.get((i + 1) % HEALTH_COLORS.size());
        HEALTH_COLOR.set(next);
        SPEC.save();
        return next;
    }

    public static void toggle(ForgeConfigSpec.BooleanValue value) {
        value.set(!value.get());
        SPEC.save();
    }
}
