// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Al morir un jugador, en vez de que sus cosas se desparramen por el piso, quedan guardadas en su CUERPO
 * (un ArmorStand con cartel "Cuerpo de X"): alguien tiene que "rematarlo" (atacarlo) para que suelte todo,
 * con un golpe seco y sangriento.
 * <p>
 * Simplificacion consciente: el cuerpo queda de pie (con el cartel bien claro), no tirado en el piso de
 * verdad; un cadaver tumbado con la piel real del jugador necesitaria un renderer y un modelo propios.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class CorpseHandler {
    private static final String CORPSE_TAG = "tc_corpse";
    private static final String ITEMS_TAG = "tc_corpse_items";
    private static final int LIFETIME_TICKS = 20 * 60 * 20; // 20 min: no se queda para siempre tirado

    private CorpseHandler() {
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        event.setCanceled(true); // nada se tira suelto: todo va adentro del cuerpo

        ListTag items = new ListTag();
        for (var itemEntity : event.getDrops()) {
            items.add(itemEntity.getItem().save(new CompoundTag()));
        }

        ArmorStand corpse = new ArmorStand(EntityType.ARMOR_STAND, level);
        corpse.setPos(player.getX(), player.getY(), player.getZ());
        corpse.setYRot(player.getYRot());
        corpse.setCustomName(Component.literal("Cuerpo de " + player.getName().getString()));
        corpse.setCustomNameVisible(true);
        corpse.setNoBasePlate(true);
        corpse.setInvisible(false);
        corpse.setInvulnerable(true); // no lo mata nada: solo se remata con AttackEntityEvent, ver abajo
        corpse.setNoGravity(false);
        CompoundTag persisted = corpse.getPersistentData();
        persisted.putBoolean(CORPSE_TAG, true);
        persisted.putLong("tc_corpse_expires", level.getGameTime() + LIFETIME_TICKS);
        persisted.put(ITEMS_TAG, items);
        level.addFreshEntity(corpse);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker) || !(event.getTarget() instanceof ArmorStand corpse)
                || !corpse.getPersistentData().getBoolean(CORPSE_TAG)) {
            return;
        }
        event.setCanceled(true);
        execute(attacker, corpse);
    }

    private static void execute(ServerPlayer attacker, ArmorStand corpse) {
        if (!(corpse.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, corpse.getX(), corpse.getY() + 1.0, corpse.getZ(), 18, 0.3, 0.4, 0.3, 0.05);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, corpse.getX(), corpse.getY() + 1.0, corpse.getZ(), 25, 0.4, 0.5, 0.4, 0.02);
        level.playSound(null, corpse.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, corpse.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.2F, 0.5F);
        level.playSound(null, corpse.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.5F);

        ListTag items = corpse.getPersistentData().getList(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            stacks.add(ItemStack.of(items.getCompound(i)));
        }
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                var itemEntity = new net.minecraft.world.entity.item.ItemEntity(level, corpse.getX(), corpse.getY() + 0.5, corpse.getZ(), stack);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }
        if (attacker != null) {
            attacker.displayClientMessage(Component.literal("Rematás el cuerpo. Sus cosas caen al piso.")
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
        corpse.discard();
    }

    /** Los cuerpos muy viejos (nadie los remató) se limpian solos para no acumular ArmorStands. */
    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.getServer().getTickCount() % 200 != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(
                    -3.0E7, -3.0E7, -3.0E7, 3.0E7, 3.0E7, 3.0E7), s -> s.getPersistentData().getBoolean(CORPSE_TAG))) {
                if (level.getGameTime() > stand.getPersistentData().getLong("tc_corpse_expires")) {
                    execute(null, stand); // se "remata solo": mejor que perder los items para siempre
                }
            }
        }
    }
}
