package com.tcorigenes.tcorigenes.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Se carga sola (ver ModEvents#chargeLunarBattery, throttled cada 20 ticks) mientras el
 * Siervo de la Luna la tiene en el inventario de noche a cielo abierto. Al usarla, gasta
 * toda la carga acumulada (max 200) por un buff de Fuerza+Velocidad proporcional.
 */
public class BateriaLunarItem extends Item {
    public static final int MAX_CHARGE = 200;

    public BateriaLunarItem(Properties properties) {
        super(properties);
    }

    public static int getCharge(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("lunar_charge");
    }

    public static void setCharge(ItemStack stack, int charge) {
        stack.getOrCreateTag().putInt("lunar_charge", Math.min(MAX_CHARGE, Math.max(0, charge)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }
        int charge = getCharge(stack);
        if (charge <= 0) {
            player.displayClientMessage(Component.literal("La batería está descargada."), true);
            return InteractionResultHolder.fail(stack);
        }
        int durationTicks = charge; // 1 tick de carga = 1 tick de buff
        int amplifier = charge >= MAX_CHARGE ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, amplifier, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, amplifier, false, true, true));
        setCharge(stack, 0);
        player.displayClientMessage(Component.literal("La energía lunar recorre tu cuerpo."), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Carga: " + getCharge(stack) + " / " + MAX_CHARGE).withStyle(ChatFormatting.AQUA));
    }
}
