// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.elementaldamage;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Mod de daño elemental: 5 tipos de daño custom (luz, fuego, agua, lunar, ender) mas los
 * atributos de resistencia/debilidad y esquive que usan las razas de tcorigenes.
 * Fase 1 de lo que pediste: la parte de items/habilidades (Origenes de clase) queda para
 * despues, ver LORE_AND_PROGRESSION.md seccion 7.
 */
@Mod(ElementalDamage.MODID)
public class ElementalDamage {
    public static final String MODID = "elementaldamage";

    public ElementalDamage() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModAttributes.ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(ModAttributes::addToPlayer);
    }
}
