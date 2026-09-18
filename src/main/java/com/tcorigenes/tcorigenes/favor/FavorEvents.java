package com.tcorigenes.tcorigenes.favor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Fuentes de favor "organicas" (fuera de ofrendas en altar, ver AltarBlock, que aplican a los
 * 6 dioses por igual). Reescrito segun el lore final confirmado por el usuario:
 * - Pater (tirano perfeccionista): craftear, construir, rezar en su altar.
 * - Meidris (compasiva, naturaleza): cultivar, criar animales, cocinar/craftear comida,
 *   consagrar el Matrimonio de Carne (ver VinculoDeCarneItem.consecrate).
 * - Deiros (angel caido, rebeldia): matar mobs, hacer explotar cosas.
 * - Filis (hija de Meidris): explorar lugares nuevos, lootear cofres.
 * - Luna (hija de Deiros) y Tempo (apatico, controla el End) no fueron redefinidos en ese
 *   pedido, asi que sus mecanicas provisorias anteriores se mantienen sin cambios.
 * Pendiente (requieren sistemas que todavia no existen en el pack): "magia blanca" de Meidris
 * y "quests de aldea" de Filis.
 * Los montos siguen siendo un punto de partida razonable, no vinieron especificados exactos.
 */
@EventBusSubscriber(modid = "tcorigenes")
public class FavorEvents {
    private static final int MEIDRIS_FAVOR_PER_HARVEST = 1;
    private static final int MEIDRIS_FAVOR_PER_BREEDING = 5;
    private static final int MEIDRIS_FAVOR_PER_FOOD_CRAFT = 1;
    private static final int DEIROS_FAVOR_PER_GOLEM_KILL = 15;
    private static final int DEIROS_FAVOR_PER_MOB_KILL = 2;
    private static final int DEIROS_FAVOR_PER_EXPLOSION = 4;
    private static final int TEMPO_FAVOR_PER_ENDERMAN_KILL = 2;
    private static final int TEMPO_FAVOR_PER_DRAGON_KILL = 100;
    private static final int PATER_FAVOR_PER_CRAFT = 1;
    private static final int PATER_FAVOR_PER_BLOCK_PLACED = 1;
    private static final double PATER_BLOCK_PLACE_CHANCE = 0.1;
    private static final int FILIS_FAVOR_PER_EXPLORATION = 3;
    private static final double FILIS_EXPLORATION_DISTANCE = 300.0;
    private static final int FILIS_FAVOR_PER_LOOT_CHEST = 2;
    private static final int FILIS_TICK_INTERVAL = 200;

    private static final Map<UUID, Vec3> FILIS_LAST_POS = new HashMap<>();
    private static final Set<BlockPos> FILIS_OPENED_CHESTS = new HashSet<>();

    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer breeder) {
            FavorManager.addFavor(breeder, Deity.MEIDRIS, MEIDRIS_FAVOR_PER_BREEDING);
            breeder.displayClientMessage(Component.literal(
                    "Meidris sonríe ante la nueva vida. (+" + MEIDRIS_FAVOR_PER_BREEDING + " favor)"), true);
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
        serverPlayer.displayClientMessage(Component.literal(
                "Luna valora tu hallazgo arcano. (+" + enchantmentCount + " favor)"), true);
    }

    @SubscribeEvent
    public static void onCropHarvest(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || !(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (event.getState().getBlock() instanceof CropBlock crop && crop.isMaxAge(event.getState())) {
            FavorManager.addFavor(serverPlayer, Deity.MEIDRIS, MEIDRIS_FAVOR_PER_HARVEST);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        FavorManager.addFavor(serverPlayer, Deity.PATER, PATER_FAVOR_PER_CRAFT);

        ItemStack crafted = event.getCrafting();
        if (crafted.getItem().isEdible()) {
            FavorManager.addFavor(serverPlayer, Deity.MEIDRIS, MEIDRIS_FAVOR_PER_FOOD_CRAFT);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        // Chance baja para que construir de favor de a poco, sin poder "farmear" favor infinito
        // solo colocando y rompiendo el mismo bloque miles de veces.
        if (serverPlayer.getRandom().nextDouble() < PATER_BLOCK_PLACE_CHANCE) {
            FavorManager.addFavor(serverPlayer, Deity.PATER, PATER_FAVOR_PER_BLOCK_PLACED);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof IronGolem) {
            FavorManager.addFavor(killer, Deity.DEIROS, DEIROS_FAVOR_PER_GOLEM_KILL);
            killer.displayClientMessage(Component.literal(
                    "Deiros aplaude tu desafío al orden establecido. (+" + DEIROS_FAVOR_PER_GOLEM_KILL + " favor)"), true);
        } else if (event.getEntity() instanceof Monster) {
            FavorManager.addFavor(killer, Deity.DEIROS, DEIROS_FAVOR_PER_MOB_KILL);
        }

        if (event.getEntity() instanceof EnderMan) {
            FavorManager.addFavor(killer, Deity.TEMPO, TEMPO_FAVOR_PER_ENDERMAN_KILL);
            killer.displayClientMessage(Component.literal(
                    "Algo ajeno a este mundo toma nota. (+" + TEMPO_FAVOR_PER_ENDERMAN_KILL + " favor)"), true);
        } else if (event.getEntity() instanceof EnderDragon) {
            FavorManager.addFavor(killer, Deity.TEMPO, TEMPO_FAVOR_PER_DRAGON_KILL);
            killer.displayClientMessage(Component.literal(
                    "Has superado la prueba del Fin. (+" + TEMPO_FAVOR_PER_DRAGON_KILL + " favor)"), true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getExplosion().getExploder() instanceof PrimedTnt tnt
                && tnt.getOwner() instanceof ServerPlayer owner) {
            FavorManager.addFavor(owner, Deity.DEIROS, DEIROS_FAVOR_PER_EXPLOSION);
        }
    }

    @SubscribeEvent
    public static void onChestOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (event.getContainer() instanceof ChestMenu chestMenu
                && chestMenu.getContainer() instanceof BlockEntity blockEntity) {
            BlockPos pos = blockEntity.getBlockPos().immutable();
            // Solo la primera vez que se abre ESE cofre/barril en este server (no persiste entre
            // reinicios): aproxima "lootear" sin depender de saber si tenia loot table sin resolver.
            if (FILIS_OPENED_CHESTS.add(pos)) {
                FavorManager.addFavor(serverPlayer, Deity.FILIS, FILIS_FAVOR_PER_LOOT_CHEST);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.tickCount % FILIS_TICK_INTERVAL != 0) {
            return;
        }
        Vec3 pos = serverPlayer.position();
        Vec3 last = FILIS_LAST_POS.get(serverPlayer.getUUID());
        if (last == null || last.distanceTo(pos) >= FILIS_EXPLORATION_DISTANCE) {
            FILIS_LAST_POS.put(serverPlayer.getUUID(), pos);
            if (last != null) {
                FavorManager.addFavor(serverPlayer, Deity.FILIS, FILIS_FAVOR_PER_EXPLORATION);
            }
        }
    }
}
