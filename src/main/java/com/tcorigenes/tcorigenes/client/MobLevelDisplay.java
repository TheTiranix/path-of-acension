package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.compat.MobScaling;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Muestra "Nv. X" sobre la cabeza del mob hostil al que estas apuntando (hasta 24 bloques) y
 * lo oculta cuando dejas de mirarlo. El nivel se deduce del modificador de vida que
 * MobScaling le pone en el servidor (el cliente ya lo recibe con los atributos del mob).
 */
public final class MobLevelDisplay {
    private static final double RANGE = 24.0;
    private static int targetedId = -1;

    private MobLevelDisplay() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        targetedId = -1;
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(mc.player, eye, end,
                mc.player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0),
                entity -> entity instanceof Mob && entity.isAlive() && !entity.isSpectator(), RANGE * RANGE);
        if (hit != null) {
            targetedId = hit.getEntity().getId();
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (entity.getId() != targetedId || !(entity instanceof Mob monster)) {
            return;
        }
        int level = 1;
        AttributeInstance health = monster.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier modifier = health == null ? null : health.getModifier(MobScaling.HEALTH_ID);
        if (modifier != null) {
            level += (int) Math.round(modifier.getAmount() / MobScaling.BONUS_PER_LEVEL);
        }
        ChatFormatting color = level >= 20 ? ChatFormatting.DARK_RED : level >= 10 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
        Component content = monster.hasCustomName() ? monster.getCustomName() : monster.getType().getDescription();
        event.setContent(Component.empty().append(content).append(" ")
                .append(Component.literal("[Nv. " + level + "]").withStyle(color)));
        event.setResult(Event.Result.ALLOW);
    }
}
