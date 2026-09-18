package com.tcorigenes.tcorigenes.compat;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Clown Head / The Thinkers (scary_mobs) y Beholder (Alex's Caves) spawnean demasiado seguido a
 * pedido; ninguno de esos mods expone un config de spawn rate para bajarlo, asi que se hace aca:
 * se deja pasar solo un 5% de sus spawns NATURALES (spawn eggs, /summon, etc. no se tocan).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class RareMobSpawnLimiter {
    private static final double ALLOWED_FRACTION = 0.05;
    private static final Set<String> LIMITED_ENTITIES = Set.of(
            "scary_mobs:clown_head",
            "scary_mobs:the_thinkers",
            "alexscaves:beholder_eye"
    );

    private RareMobSpawnLimiter() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (id != null && LIMITED_ENTITIES.contains(id.toString()) && event.getLevel().getRandom().nextDouble() >= ALLOWED_FRACTION) {
            event.setSpawnCancelled(true);
        }
    }
}
