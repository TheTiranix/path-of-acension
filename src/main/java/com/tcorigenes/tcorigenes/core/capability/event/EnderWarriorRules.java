package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.compat.TanCompat;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tudominio.elementaldamage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.EnderManAngerEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Reglas del Ender Warrior con el agua: no necesita beber (la sed se fuerza al maximo), tomar
 * agua/pociones lo lastima ignorando armadura, la lluvia lo lastima un poco (la armadura
 * completa de cualquier tipo lo protege), y mirar a un Enderman no lo enfurece.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class EnderWarriorRules {
    private static final float DRINK_DAMAGE = 4.0F;
    private static final float RAIN_DAMAGE = 0.2F;

    private EnderWarriorRules() {
    }

    private static boolean isEnderWarrior(Player player) {
        return player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace() == Race.ENDER_WARRIOR).orElse(false);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !isEnderWarrior(player)) {
            return;
        }
        if (TanCompat.isLoaded()) {
            TanCompat.fillThirst(player);
        }
        if (player.tickCount % 20 == 0 && player.level().isRainingAt(player.blockPosition().above()) && !wearingFullArmor(player)) {
            com.tudominio.elementaldamage.ElementalDamageSource.hurt(player, ModDamageTypes.WATER_ELEMENTAL, player, RAIN_DAMAGE);
            enderSmoke(player);
        }
    }

    private static boolean wearingFullArmor(Player player) {
        return !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                && !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }

    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isEnderWarrior(player)) {
            return;
        }
        ItemStack stack = event.getItem();
        boolean isWater = stack.is(Items.POTION) && PotionUtils.getPotion(stack) != null;
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && id.getNamespace().equals("toughasnails") && id.getPath().contains("water")) {
            isWater = true;
        }
        if (isWater) {
            // magic() ignora armadura, como se pidio.
            player.hurt(player.damageSources().magic(), DRINK_DAMAGE);
            enderSmoke(player);
        }
    }

    @SubscribeEvent
    public static void onEndermanAnger(EnderManAngerEvent event) {
        if (isEnderWarrior(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private static void enderSmoke(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
    }
}
