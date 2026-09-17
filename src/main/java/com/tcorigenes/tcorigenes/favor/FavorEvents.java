package com.tcorigenes.tcorigenes.favor;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Las formas "organicas" de ganar favor que existen hasta ahora, una por dios y bien distintas
 * entre si (fuera de los altares/ofrendas, ver AltarBlock):
 * - Pater (orden/purga): matar al ULTIMO aldeano de una aldea (0 aldeanos vivos en 48 bloques).
 * - Filis (redencion/vida): hacer reproducir animales y que tengan crias.
 * - Luna (oculto/arcano, provisorio): encontrar/conseguir objetos encantados al recogerlos.
 * - Meidris (naturaleza salvaje): usar hueso molido para hacer crecer plantas.
 * - Deiros (rebeldia/fuego, angel caido): matar a un Iron Golem (el guardian del orden).
 * - Tempo (secreto, prueba del End): matar endermans, con un bonus grande por el Dragon del End.
 * Los montos son un punto de partida razonable, no vinieron especificados exactos.
 */
@EventBusSubscriber(modid = "tcorigenes")
public class FavorEvents {
    private static final double VILLAGE_WIPE_RADIUS = 48.0;
    private static final int PATER_FAVOR_PER_VILLAGE_WIPE = 20;
    private static final int FILIS_FAVOR_PER_BIRTH = 5;
    private static final int MEIDRIS_FAVOR_PER_BONEMEAL = 1;
    private static final int DEIROS_FAVOR_PER_GOLEM_KILL = 15;
    private static final int TEMPO_FAVOR_PER_ENDERMAN_KILL = 2;
    private static final int TEMPO_FAVOR_PER_DRAGON_KILL = 100;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || villager.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        AABB area = new AABB(villager.blockPosition()).inflate(VILLAGE_WIPE_RADIUS);
        List<Villager> remaining = villager.level().getEntitiesOfClass(Villager.class, area,
                other -> other != villager && other.isAlive());
        if (remaining.isEmpty()) {
            FavorManager.addFavor(killer, Deity.PATER, PATER_FAVOR_PER_VILLAGE_WIPE);
            killer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Pater se regocija: has exterminado una aldea entera. (+" + PATER_FAVOR_PER_VILLAGE_WIPE + " favor)"), true);
        }
    }

    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer breeder) {
            FavorManager.addFavor(breeder, Deity.FILIS, FILIS_FAVOR_PER_BIRTH);
            breeder.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Filis sonríe ante la nueva vida. (+" + FILIS_FAVOR_PER_BIRTH + " favor)"), true);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return;
        }
        int enchantmentCount = EnchantmentHelper.getEnchantments(event.getItem().getItem()).size();
        if (enchantmentCount <= 0) {
            return;
        }
        // Provisorio: asignado a Luna (magia oculta/arcana) hasta confirmar con el usuario a que
        // dios corresponde de verdad "encontrar objetos de encantamiento" en el lore real.
        FavorManager.addFavor(serverPlayer, Deity.LUNA, enchantmentCount);
        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Luna valora tu hallazgo arcano. (+" + enchantmentCount + " favor)"), true);
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        FavorManager.addFavor(serverPlayer, Deity.MEIDRIS, MEIDRIS_FAVOR_PER_BONEMEAL);
        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Meidris celebra el crecimiento salvaje. (+" + MEIDRIS_FAVOR_PER_BONEMEAL + " favor)"), true);
    }

    @SubscribeEvent
    public static void onGuardianOrBeyondDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof IronGolem) {
            FavorManager.addFavor(killer, Deity.DEIROS, DEIROS_FAVOR_PER_GOLEM_KILL);
            killer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Deiros aplaude tu desafío al orden establecido. (+" + DEIROS_FAVOR_PER_GOLEM_KILL + " favor)"), true);
        } else if (event.getEntity() instanceof EnderMan) {
            FavorManager.addFavor(killer, Deity.TEMPO, TEMPO_FAVOR_PER_ENDERMAN_KILL);
            killer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Algo ajeno a este mundo toma nota. (+" + TEMPO_FAVOR_PER_ENDERMAN_KILL + " favor)"), true);
        } else if (event.getEntity() instanceof EnderDragon) {
            FavorManager.addFavor(killer, Deity.TEMPO, TEMPO_FAVOR_PER_DRAGON_KILL);
            killer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Has superado la prueba del Fin. (+" + TEMPO_FAVOR_PER_DRAGON_KILL + " favor)"), true);
        }
    }
}
