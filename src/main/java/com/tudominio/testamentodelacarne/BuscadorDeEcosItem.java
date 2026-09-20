// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Herramienta de exploracion/deteccion: consume un Fragmento de Memoria por uso.
 * Click derecho normal -> resalta (Glowing) monstruos en 32 bloques.
 * Shift + click derecho -> localiza la estructura etiquetada "on_treasure_maps" mas cercana (server-side).
 */
public class BuscadorDeEcosItem extends Item {
    public BuscadorDeEcosItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        ItemStack fragmento = new ItemStack((ItemLike) ModItems.FRAGMENTO_DE_MEMORIA.get());

        if (!player.getInventory().contains(fragmento)) {
            if (level.isClientSide()) {
                player.displayClientMessage(
                        Component.literal("El Buscador de Ecos permanece en silencio. Necesita un Fragmento de Memoria.").withStyle(ChatFormatting.RED), false);
                level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5F, 1.0F, false);
            }
            return InteractionResultHolder.fail(heldStack);
        }

        if (!player.getAbilities().instabuild) {
            player.getInventory().removeItem(player.getInventory().findSlotMatchingItem(fragmento), 1);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) level;
                BlockPos structurePos = serverLevel.findNearestMapStructure(StructureTags.ON_TREASURE_MAPS, player.blockPosition(), 100, false);
                if (structurePos != null) {
                    player.displayClientMessage(Component.literal(
                            "El buscador resuena con una estructura ancestral en las coordenadas: X=" + structurePos.getX() + ", Z=" + structurePos.getZ()
                    ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
                } else {
                    player.displayClientMessage(Component.literal("El buscador no detecta ninguna estructura de poder cercano.").withStyle(ChatFormatting.GRAY), false);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        } else {
            AABB area = new AABB(player.blockPosition()).inflate(32.0);
            List<LivingEntity> entidades = level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity instanceof Monster && entity.isAlive());
            entidades.forEach(monstruo -> monstruo.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, true)));
            player.displayClientMessage(Component.literal(
                    "El Buscador emite un pulso etéreo, revelando " + entidades.size() + " ecos hostiles cercanos."
            ).withStyle(ChatFormatting.DARK_PURPLE), false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return InteractionResultHolder.success(heldStack);
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.testamentodelacarne.buscador_de_ecos").withStyle(ChatFormatting.GOLD));
        pTooltipComponents.add(Component.literal("Clic Derecho: Revela enemigos.").withStyle(ChatFormatting.GRAY));
        pTooltipComponents.add(Component.literal("Shift + Clic Derecho: Busca estructuras.").withStyle(ChatFormatting.GRAY));
    }
}
