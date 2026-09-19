package com.tcorigenes.tcorigenes.compat;

import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Los mobs hostiles nacen mas fuertes cuanto mas lejos del spawn del mundo y segun la
 * dimension (Nether/End ya vienen con un bonus base). Se aplica UNA sola vez por mob (flag en
 * persistentData) como MULTIPLY_TOTAL sobre vida y daño de ataque.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class MobScaling {
    private static final String SCALED_KEY = "tc_mob_scaled";
    public static final UUID HEALTH_ID = UUID.fromString("6d1c0f52-3b0a-4a55-9c1e-2f7a8b1d0a01");
    private static final UUID DAMAGE_ID = UUID.fromString("6d1c0f52-3b0a-4a55-9c1e-2f7a8b1d0a02");
    private static final double BLOCKS_PER_LEVEL = 500.0;
    private static final int MAX_LEVEL = 20;
    public static final double BONUS_PER_LEVEL = 0.04;
    private static final double NETHER_BONUS = 0.30;
    private static final double END_BONUS = 0.60;

    private MobScaling() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Monster mob)) {
            return;
        }
        if (mob.getPersistentData().getBoolean(SCALED_KEY)) {
            return;
        }
        mob.getPersistentData().putBoolean(SCALED_KEY, true);

        var spawn = event.getLevel().getSharedSpawnPos();
        double dx = mob.getX() - spawn.getX();
        double dz = mob.getZ() - spawn.getZ();
        int level = (int) Math.min(MAX_LEVEL, Math.sqrt(dx * dx + dz * dz) / BLOCKS_PER_LEVEL);

        double bonus = level * BONUS_PER_LEVEL;
        if (event.getLevel().dimension() == Level.NETHER) {
            bonus += NETHER_BONUS;
        } else if (event.getLevel().dimension() == Level.END) {
            bonus += END_BONUS;
        }
        if (bonus <= 0.0) {
            return;
        }

        apply(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "TC health scaling", bonus);
        apply(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, "TC damage scaling", bonus);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void apply(AttributeInstance attribute, UUID id, String name, double bonus) {
        if (attribute != null && attribute.getModifier(id) == null) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
