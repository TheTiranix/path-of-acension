// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.effect;

import com.tcorigenes.tcorigenes.TCOrigenes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Enzima Acuatica: efecto beneficioso que vuelve al Ender Warrior inmune a TODO daño por agua
 * (nadar, lluvia, beber agua/pociones, ahogo). Es solo el marcador: las reglas que lo consultan
 * estan en ModEvents y EnderWarriorRules. Se consigue con una pocion (ver TCOrigenes#commonSetup).
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TCOrigenes.MOD_ID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, TCOrigenes.MOD_ID);

    private static final class MarkerEffect extends MobEffect {
        MarkerEffect(int color) {
            super(MobEffectCategory.BENEFICIAL, color);
        }
    }

    public static final RegistryObject<MobEffect> AQUATIC_ENZYME = EFFECTS.register("enzima_acuatica", () -> new MarkerEffect(0x2A6BFF));

    public static final RegistryObject<Potion> AQUATIC_ENZYME_POTION = POTIONS.register("enzima_acuatica",
            () -> new Potion("enzima_acuatica", new MobEffectInstance(AQUATIC_ENZYME.get(), 20 * 60 * 3)));

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        POTIONS.register(modEventBus);
    }
}
