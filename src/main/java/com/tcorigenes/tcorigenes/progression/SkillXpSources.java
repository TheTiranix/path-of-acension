// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression;

import com.tcorigenes.tcorigenes.compat.MobScaling;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * De donde sale el XP del arbol de habilidades (se canjea por puntos, ver SkillTreeManager#buyPoint): matar mobs. Cada mob da XP segun su vida y su
 * nivel (los de zonas lejanas y de otras dimensiones dan mas) y los jefes dan diez veces mas. No dan XP las mascotas ni los sirvientes.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class SkillXpSources {
    private SkillXpSources() {
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getSource().getEntity() instanceof ServerPlayer killer)
                || !(event.getEntity() instanceof Mob mob) || (mob instanceof TamableAnimal tame && tame.isTame())
                || mob.getPersistentData().contains("tc_servant_owner")) {
            return;
        }
        long xp = Math.max(2L, Math.round(mob.getMaxHealth() / 5.0 + MobScaling.levelOf(mob) * 2.0));
        if (mob.getType().is(Tags.EntityTypes.BOSSES)) {
            xp *= 10L;
        }
        SkillTreeManager.addXp(killer, xp);
        killer.displayClientMessage(Component.translatable("tcorigenes.skillxp.gain", xp), true);
    }
}
