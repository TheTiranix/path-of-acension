// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import net.minecraft.locale.Language;

/** Parte cliente de Tr (separada para que el servidor dedicado no cargue clases del cliente). Devuelve el texto crudo, sin formato. */
final class TrClient {
    private TrClient() {
    }

    static String translate(String spanish) {
        String key = Tr.key(spanish);
        Language language = Language.getInstance();
        return language.has(key) ? language.getOrDefault(key) : spanish;
    }
}
