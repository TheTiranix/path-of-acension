// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.faction;

import com.tcorigenes.tcorigenes.TCOrigenes;
import com.tcorigenes.tcorigenes.faction.entity.FactionNpcEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TCOrigenes.MOD_ID);

    public static final RegistryObject<EntityType<FactionNpcEntity>> FACTION_NPC = ENTITY_TYPES.register("faction_npc",
            () -> EntityType.Builder.of(FactionNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("faction_npc"));

    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.core.WeakPointEntity>> WEAK_POINT = ENTITY_TYPES.register("weak_point",
            () -> EntityType.Builder.<com.tcorigenes.tcorigenes.core.WeakPointEntity>of(com.tcorigenes.tcorigenes.core.WeakPointEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .fireImmune()
                    .build("weak_point"));

    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.core.DamageDummyEntity>> DAMAGE_DUMMY = ENTITY_TYPES.register("damage_dummy",
            () -> EntityType.Builder.<com.tcorigenes.tcorigenes.core.DamageDummyEntity>of(com.tcorigenes.tcorigenes.core.DamageDummyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("damage_dummy"));

    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.ability.AnimaSpiritEntity>> ANIMA_SPIRIT = ENTITY_TYPES.register("anima_spirit",
            () -> EntityType.Builder.<com.tcorigenes.tcorigenes.ability.AnimaSpiritEntity>of(com.tcorigenes.tcorigenes.ability.AnimaSpiritEntity::new, MobCategory.MISC)
                    .sized(0.7F, 1.9F)
                    .clientTrackingRange(10)
                    .noSave()
                    .fireImmune()
                    .build("anima_spirit"));

    private static RegistryObject<EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>> god(String name) {
        float[] size = com.tcorigenes.tcorigenes.boss.GodSizes.SIZES.get(name);
        return ENTITY_TYPES.register("god_" + name,
                () -> EntityType.Builder.<com.tcorigenes.tcorigenes.boss.GodBossEntity>of(com.tcorigenes.tcorigenes.boss.GodBossEntity::new, MobCategory.MONSTER)
                        .sized(size[0], size[1])
                        .clientTrackingRange(24)
                        .fireImmune()
                        .build("god_" + name));
    }

    /** Los seis dioses jefe, en este orden: pater, luna, deiros, meidris, filis, tempo. */
    public static final String[] GOD_NAMES = {"pater", "luna", "deiros", "meidris", "filis", "tempo"};
    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>> GOD_PATER = god("pater");
    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>> GOD_LUNA = god("luna");
    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>> GOD_DEIROS = god("deiros");
    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>> GOD_MEIDRIS = god("meidris");
    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>> GOD_FILIS = god("filis");
    public static final RegistryObject<EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>> GOD_TEMPO = god("tempo");
    public static final java.util.List<RegistryObject<EntityType<com.tcorigenes.tcorigenes.boss.GodBossEntity>>> GODS =
            java.util.List.of(GOD_PATER, GOD_LUNA, GOD_DEIROS, GOD_MEIDRIS, GOD_FILIS, GOD_TEMPO);

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
