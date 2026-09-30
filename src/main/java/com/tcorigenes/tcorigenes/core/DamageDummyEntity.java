// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Dummy de daño (pedido de alejandr0): un muñeco que registra en el chat el DAÑO REAL de cada golpe que recibe, no un DPS ni
 * un promedio. El numero sale del evento de daño final (despues de todos los modificadores del pack: clase, critico,
 * elementales, resistencias), y cada daño elemental que entra como golpe aparte (ver PendingElementalHits) se anota como una
 * linea propia con su elemento. No recibe daño de verdad (se cancela) ni retroceso, asi no se muere ni se mueve, y cada golpe
 * cuenta (se le baja el tiempo de invulnerabilidad).
 */
public class DamageDummyEntity extends Mob {
    public DamageDummyEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1000.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        this.invulnerableTime = 0; // que cada golpe cuente, aunque caiga pegado al anterior
        return super.hurt(source, amount);
    }

    private static String elementLabel(DamageSource source) {
        ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
        if (key == null || !key.location().getNamespace().equals("elementaldamage")) {
            return "físico";
        }
        String path = key.location().getPath();
        if (path.contains("fire")) return "fuego";
        if (path.contains("water")) return "agua";
        if (path.contains("light")) return "luz";
        if (path.contains("lunar")) return "lunar";
        if (path.contains("ender")) return "ender";
        if (path.contains("ice")) return "hielo";
        if (path.contains("earth")) return "tierra";
        if (path.contains("air")) return "aire";
        if (path.contains("natural")) return "natural";
        return path;
    }

    @EventBusSubscriber(modid = "tcorigenes")
    public static final class Events {
        private Events() {
        }

        /** Despues de todos los demas: el monto que llega aca es el daño final que recibiria cualquier entidad. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onDamage(LivingDamageEvent event) {
            if (!(event.getEntity() instanceof DamageDummyEntity dummy) || dummy.level().isClientSide()) {
                return;
            }
            DamageSource source = event.getSource();
            if (source.getEntity() instanceof Player player) {
                player.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "Dummy » %,.1f de daño (%s)", event.getAmount(),
                        elementLabel(source))).withStyle(ChatFormatting.GREEN));
            }
            event.setCanceled(true); // no recibe daño de verdad
            dummy.setHealth(dummy.getMaxHealth());
        }
    }
}
