// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Traduccion de textos del mod (pedido: poder jugarlo en ingles). Los textos del codigo estan escritos en español; en ingles se buscan en
 * el archivo de idioma con la clave "pa.t.<hash del texto en español>". Si el idioma del juego no tiene esa clave (español), queda el texto
 * original. En un servidor dedicado no hay cliente: devuelve siempre el español.
 */
public final class Tr {
    private Tr() {
    }

    public static String key(String spanish) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(spanish.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("pa.t.");
            for (int i = 0; i < 5; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return "pa.t.none";
        }
    }

    public static String s(String spanish) {
        if (!FMLEnvironment.dist.isClient()) {
            return spanish;
        }
        return TrClient.translate(spanish);
    }

    /** s() con formato: el texto (ya traducido) lleva %s. */
    public static String f(String spanishTemplate, Object... args) {
        return String.format(s(spanishTemplate), args);
    }
}
