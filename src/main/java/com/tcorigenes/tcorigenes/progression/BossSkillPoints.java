package com.tcorigenes.tcorigenes.progression;

import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Matar un jefe da puntos para el arbol de habilidades de PassiveSkillTree (se otorgan con su
 * propio comando "skilltree points add", que ademas los sincroniza con el cliente).
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class BossSkillPoints {
    private static final Set<EntityType<?>> BOSSES = Set.of(
            EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN);

    private BossSkillPoints() {
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        var type = event.getEntity().getType();
        if (!BOSSES.contains(type) && !type.is(Tags.EntityTypes.BOSSES)) {
            return;
        }
        int points = type == EntityType.ENDER_DRAGON || type == EntityType.WITHER ? 3 : 2;
        killer.getServer().getCommands().performPrefixedCommand(
                killer.getServer().createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                "skilltree points add " + killer.getGameProfile().getName() + " " + points);
        killer.displayClientMessage(Component.literal("Jefe derrotado: +" + points + " puntos de habilidad."), false);
    }
}
