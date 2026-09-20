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

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
