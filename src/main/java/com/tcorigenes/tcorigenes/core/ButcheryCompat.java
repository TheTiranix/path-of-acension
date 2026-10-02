// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.io.File;
import java.io.FileReader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pedido de alejandr0: compatibilidad de Butcher's Delight (colgar y descuartizar animales) con los animales de otros mods. El mod
 * solo conoce las vacas, cerdos, ovejas, etc. de vanilla: al morir uno de esos suelta su cadaver (item "dead_cow", "deadpig"...) y
 * se va, sin drops. Aca los animales de OTROS mods hacen lo mismo con el cadaver del animal de vanilla que mas se les parece por
 * tamaño (gallina, cerdo, vaca, llama o hoglin). No se tocan los bebes, las mascotas con dueño, los jefes ni los bichos diminutos.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class ButcheryCompat {
    private static final TagKey<Item> KNIVES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "tools/knives"));
    private static Boolean onlyKnifeDrops;

    private ButcheryCompat() {
    }

    /** "onlyknifedrops" de la config de Butcher's Delight: si esta, solo los cuchillos hacen que quede cadaver. */
    private static boolean onlyKnife() {
        if (onlyKnifeDrops == null) {
            onlyKnifeDrops = false;
            File file = new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "config", "ButchersDelight.json");
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(reader, com.google.gson.JsonObject.class);
                    if (json != null && json.has("onlyknifedrops")) {
                        onlyKnifeDrops = json.get("onlyknifedrops").getAsBoolean();
                    }
                } catch (java.io.IOException | RuntimeException e) {
                    // sin config legible: se queda en "cualquier arma"
                }
            }
        }
        return onlyKnifeDrops;
    }

    /**
     * Pedido de alejandr0: solo los animales que tiene sentido comer dejan cadaver (los dragones, que se contaban como "animales" por ser
     * TamableAnimal, ya no). Caso por caso: Alex's Mobs (los herbivoros y aves de caza), y otros mods con fauna comestible.
     * entidad -> cadaver de Butcher's Delight que mas se le parece.
     */
    private static final java.util.Map<String, String> EDIBLE = new java.util.HashMap<>();

    static {
        // Alex's Mobs
        EDIBLE.put("alexsmobs:bison", "dead_cow");
        EDIBLE.put("alexsmobs:moose", "dead_cow");
        EDIBLE.put("alexsmobs:tusklin", "dead_cow");
        EDIBLE.put("alexsmobs:gazelle", "deadgoat");
        EDIBLE.put("alexsmobs:kangaroo", "deadsheep");
        EDIBLE.put("alexsmobs:emu", "deadllama");
        EDIBLE.put("alexsmobs:roadrunner", "deadchiken");
        // Twilight Forest
        EDIBLE.put("twilightforest:boar", "deadpig");
        EDIBLE.put("twilightforest:deer", "deadsheep");
        EDIBLE.put("twilightforest:bighorn_sheep", "deadsheep");
        EDIBLE.put("twilightforest:dwarf_rabbit", "deadrabbitwhite");
        // Aether
        EDIBLE.put("aether:phyg", "deadpig");
        EDIBLE.put("aether:flying_cow", "dead_cow");
        EDIBLE.put("aether:sheepuff", "deadsheep");
        EDIBLE.put("aether:moa", "deadchiken");
        // Alex's Caves
        EDIBLE.put("alexscaves:sea_pig", "deadpig");
        EDIBLE.put("alexscaves:grottoceratops", "dead_cow");
    }

    private static String carcassFor(LivingEntity entity, ResourceLocation typeId) {
        return EDIBLE.get(typeId.toString());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !ModList.get().isLoaded("butchersdelight") || entity instanceof Player || entity.isBaby()) {
            return;
        }
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeId == null || typeId.getNamespace().equals("minecraft") || typeId.getNamespace().equals("tcorigenes")) {
            return; // los de vanilla ya los maneja el propio mod
        }
        if (!EDIBLE.containsKey(typeId.toString())) {
            return;
        }
        if ((entity instanceof TamableAnimal tamable && tamable.isTame())
                || (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) || entity.getMaxHealth() >= 450.0F) {
            return;
        }
        if (onlyKnife()) {
            Entity killer = event.getSource().getEntity();
            if (!(killer instanceof LivingEntity living) || !living.getMainHandItem().is(KNIVES)) {
                return;
            }
        }
        String name = carcassFor(entity, typeId);
        Item carcass = name == null ? null : ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("butchersdelight", name));
        if (carcass == null || carcass == net.minecraft.world.item.Items.AIR) {
            return;
        }
        ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack(carcass));
        drop.setPickUpDelay(10);
        entity.level().addFreshEntity(drop);
        event.setCanceled(true);
        entity.discard();
    }
}
