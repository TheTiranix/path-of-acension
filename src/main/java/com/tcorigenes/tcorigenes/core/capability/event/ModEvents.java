// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider;
import com.tcorigenes.tcorigenes.attributes.RaceAttributeManager;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import com.tcorigenes.tcorigenes.item.BateriaLunarItem;
import com.tcorigenes.tcorigenes.item.ModItems;
import com.tcorigenes.tcorigenes.playerclass.ClassAttributeManager;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import com.tcorigenes.tcorigenes.playerclass.capability.PlayerClassProvider;
import com.tudominio.elementaldamage.ModDamageTypes;
import com.tudominio.testamentodelacarne.AnimaSwordItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Bonos condicionales que no viven en RaceAttributeManager/ClassAttributeManager porque
 * dependen del tick/contexto: DEVOTO (regen+curacion), SIERVO_DE_LA_LUNA (buff nocturno +
 * carga de bateria), ANGEL (inmune a caida + planeo), DEMONIO (inmune a fuego/lava),
 * ENDER_WARRIOR (debil al ahogo), MALNACIDO (veneno cura, inmune a inanicion, 35% de errar),
 * HEREJE (marca de punto debil automatica cada 10s), ARQUERO (bono vs marcados),
 * Guerrero Anima/Ritualista (restriccion de armas).
 */
public class ModEvents {
    /** Daño de Tough As Nails (no hay constantes vanilla para esto). */
    private static final ResourceKey<DamageType> TOUGH_AS_NAILS_HYPERTHERMIA =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("toughasnails", "hyperthermia"));
    private static final ResourceKey<DamageType> TOUGH_AS_NAILS_THIRST =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("toughasnails", "thirst"));
    /** Jugador -> ultimo nivel de exhaustion de hambre visto (para el 50% extra del Ender Warrior). */
    private static final Map<UUID, Float> LAST_EXHAUSTION = new HashMap<>();


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Player player = event.player;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.tcorigenes.tcorigenes.ability.TemporaryInvulnerability.tick(serverPlayer);
            if (player.tickCount % 10 == 0 && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.tcorigenes.tcorigenes.ability.AnimaSpiritTracker.tick(serverLevel);
            }
        }

        player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> {
            Race playerRace = raceInfo.getRace();

            if (playerRace == Race.DEVOTO) {
                CompoundTag persistentData = player.getPersistentData();
                int regenTimer = persistentData.getInt("player_regen_timer");
                if (++regenTimer >= 50) {
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.heal(1.0F);
                    }
                    regenTimer = 0;
                }
                persistentData.putInt("player_regen_timer", regenTimer);
            }

            if (playerRace == Race.SIERVO_DE_LA_LUNA) {
                handleSiervoDeLaLunaTick(player);
            }

            // Malnacido purificado: regeneracion II permanente, sin MobEffect (1 de vida cada 25 ticks).
            if (playerRace == Race.MALNACIDO && player.getPersistentData().getBoolean("malnacido_purificado")) {
                MobEffectInstance oldRegen = player.getEffect(MobEffects.REGENERATION);
                if (oldRegen != null && oldRegen.getDuration() > 100000) {
                    player.removeEffect(MobEffects.REGENERATION); // el efecto infinito de versiones anteriores
                }
                if (player.tickCount % 25 == 0 && player.getHealth() < player.getMaxHealth()) {
                    player.heal(1.0F);
                }
            }

            if (playerRace == Race.ANGEL) {
                // Planeo simplificado: si esta cayendo y no esta en el suelo, aplica Caida Lenta.
                // (sin MobEffect: los efectos raciales ya no aparecen como efectos de pocion; la desaceleracion
                // real corre en el cliente, ver client.RacialPassivesClient, y aca se anula el daño por caida)
                if (!player.onGround() && player.getDeltaMovement().y < -0.05 && !player.getAbilities().flying) {
                    player.fallDistance = 0.0F;
                }
            }

            if (playerRace == Race.HEREJE && player.tickCount % 200 == 0) {
                if (player instanceof net.minecraft.server.level.ServerPlayer weakPointPlayer) {
                    com.tcorigenes.tcorigenes.core.WeakPointManager.markForHereje(weakPointPlayer);
                }
            }

            if (playerRace == Race.ENDER_WARRIOR) {
                boolean wearingEscafandra = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ESCAFANDRA.get());
                if (wearingEscafandra) {
                    // Respirar bajo el agua sin MobEffect (no aparece como efecto de pocion).
                    player.setAirSupply(player.getMaxAirSupply());
                }
                // Para nadar sin daño hace falta el combo completo: escafandra + pechera + pantalon +
                // botas + guantes (guante izquierdo Y derecho en "hands" de Curios, o el slot de guantes
                // del Aether) + brazalete.
                boolean swimProtected = wearingEscafandra
                        && !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                        && !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                        && !player.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                        && (com.tcorigenes.tcorigenes.compat.CuriosCompat.allSlotsOccupied(player, "hands")
                                || com.tcorigenes.tcorigenes.compat.CuriosCompat.hasAnyInSlot(player, "aether_gloves"))
                        && com.tcorigenes.tcorigenes.compat.CuriosCompat.hasAnyInSlot(player, "bracelet");
                // La Enzima Acuatica da inmunidad total al agua sin necesitar nada de lo anterior.
                swimProtected = swimProtected || player.hasEffect(com.tcorigenes.tcorigenes.effect.ModEffects.AQUATIC_ENZYME.get());
                if (player.tickCount % 20 == 0 && player.isInWater() && !swimProtected) {
                    // Como el Enderman: el agua le hace daño. Usa el tipo elemental de agua (no
                    // "drown") para que ademas se le aplique su propia debilidad de +20% (ver
                    // RaceAttributeManager).
                    com.tudominio.elementaldamage.ElementalDamageSource.hurt(player, ModDamageTypes.WATER_ELEMENTAL, player, 1.0F);
                }
                tickEnderWarriorHunger(player);
                if (player.tickCount % 5 == 0 && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // Particula distintiva (violeta, estilo Enderman/portal) para notar la raza a simple vista.
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.3, 0.5, 0.3, 0.0);
                }
            }
        });
    }

    /** El hambre le baja un 50% mas rapido: cada vez que la exhaustion (lo que hace bajar el
     *  hambre) sube, se le suma un 50% extra de esa misma suba. */
    private static void tickEnderWarriorHunger(Player player) {
        var foodData = player.getFoodData();
        float current = foodData.getExhaustionLevel();
        float last = LAST_EXHAUSTION.getOrDefault(player.getUUID(), current);
        float delta = current - last;
        if (delta > 0.0F) {
            // +50% normal; con calor extremo el consumo de hambre se duplica (x2 en total).
            boolean extremeHeat = com.tcorigenes.tcorigenes.compat.TanCompat.isLoaded()
                    && com.tcorigenes.tcorigenes.compat.TanCompat.isExtremeHeat(player);
            foodData.addExhaustion(delta * (extremeHeat ? 1.0F : 0.5F));
            current = foodData.getExhaustionLevel();
        }
        LAST_EXHAUSTION.put(player.getUUID(), current);
    }


    private static final UUID LUNAR_DRAW_SPEED_ID = UUID.fromString("e8a2d7a8-8a2c-4b8a-9a2d-7a8a2d7a8a31");
    private static final UUID LUNAR_ATTACK_SPEED_ID = UUID.fromString("e8a2d7a8-8a2c-4b8a-9a2d-7a8a2d7a8a30");

    /** "Expuesto a la luna": de noche y a cielo abierto. */
    public static boolean isMoonExposed(Player player) {
        Level world = player.level();
        return world.isNight() && world.canSeeSky(player.blockPosition());
    }

    private static void handleSiervoDeLaLunaTick(Player player) {
        AttributeInstance damageInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance attackSpeedInstance = player.getAttribute(Attributes.ATTACK_SPEED);
        AttributeInstance drawSpeedInstance = player.getAttribute(com.tudominio.elementaldamage.ModAttributes.DRAW_SPEED.get());
        AttributeInstance protectionInstance = player.getAttribute(Attributes.ARMOR);
        if (protectionInstance != null) {
            protectionInstance.removeModifier(RaceAttributeManager.LUNAR_PROTECTION_MODIFIER_ID);
        }

        if (isMoonExposed(player)) {
            // +10% daño general y +5% velocidad de ataque (draw speed: sin atributo todavia), de
            // jerarquia "absoluta" (MULTIPLY_TOTAL) como cualquier bono de raza.
            // Se suman con el % de raza y clase (ver OriginBonuses).
            com.tcorigenes.tcorigenes.attributes.OriginBonuses.set(player, "moon", Attributes.ATTACK_DAMAGE, 0.10);
            com.tcorigenes.tcorigenes.attributes.OriginBonuses.set(player, "moon", Attributes.ATTACK_SPEED, 0.05);

            if (drawSpeedInstance != null && drawSpeedInstance.getModifier(LUNAR_DRAW_SPEED_ID) == null) {
                drawSpeedInstance.addTransientModifier(new AttributeModifier(
                        LUNAR_DRAW_SPEED_ID, "Lunar Draw Speed Buff", 0.05, AttributeModifier.Operation.ADDITION));
            }

            if (player.tickCount % 20 == 0) {
                // Vision nocturna y regeneracion lunar 1: 3% de la vida maxima por segundo.
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(player.getMaxHealth() * 0.03F);
                }
                // Carga la Bateria Lunar si la tiene en el inventario.
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.getItem() == ModItems.BATERIA_LUNAR.get()) {
                        int charge = BateriaLunarItem.getCharge(stack);
                        if (charge < BateriaLunarItem.MAX_CHARGE) {
                            BateriaLunarItem.setCharge(stack, charge + 20);
                        }
                        break;
                    }
                }
            }
        } else {
            com.tcorigenes.tcorigenes.attributes.OriginBonuses.set(player, "moon", Attributes.ATTACK_DAMAGE, 0.0);
            com.tcorigenes.tcorigenes.attributes.OriginBonuses.set(player, "moon", Attributes.ATTACK_SPEED, 0.0);
            if (drawSpeedInstance != null) {
                drawSpeedInstance.removeModifier(LUNAR_DRAW_SPEED_ID);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> {
                if (raceInfo.getRace() == Race.DEVOTO) {
                    // +50% a TODA curacion: natural, pociones, efectos y robo de vida (este evento
                    // intercepta cualquier heal sea cual sea la fuente).
                    event.setAmount(event.getAmount() * 1.5F);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        // Stun de daño elemental de tierra: "incapaz de atacar" mientras dure (ver
        // ElementalDamageEvents#applyEarthStun).
        if (com.tudominio.elementaldamage.ElementalProcCooldowns.isStunned(player, player.level().getGameTime())) {
            event.setCanceled(true);
            return;
        }
        ItemStack weapon = player.getMainHandItem();

        boolean blockedByClass = player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(classInfo -> {
                    PlayerClass playerClass = classInfo.getPlayerClass();
                    if (playerClass == PlayerClass.GUERRERO_ANIMA && !(weapon.getItem() instanceof AnimaSwordItem)) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "Tu alma solo reconoce a tu Espada Ánima."), true);
                        return true;
                    }
                    if (playerClass == PlayerClass.RITUALISTA_ARCANO
                            && (weapon.getItem() instanceof AxeItem || weapon.getItem() instanceof TridentItem)) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "Tus brazos de erudito no pueden con armas tan pesadas."), true);
                        return true;
                    }
                    return false;
                }).orElse(false);

        if (blockedByClass) {
            event.setCanceled(true);
            return;
        }

        // Malnacido: 35% de errar el golpe por sus manos deformes.
        player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> {
            if (raceInfo.getRace() == Race.MALNACIDO && player.getRandom().nextFloat() < 0.35F) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Tu golpe falla."), true);
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Los bonos del lado atacante viven en WeakPointManager, GeneralDamageRules y RacialElemental.

        // --- Lado victima: inmunidades/reflejos de raza ---
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            // Reducciones de origen: se SUMAN entre si (Escudero 20% + Gigante Rocoso 5% = 25% menos),
            // no se multiplican (x0.8 * x0.95 = 24%).
            float reduction = 0.0F;
            if (player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                    .map(info -> info.getPlayerClass() == PlayerClass.ESCUDERO).orElse(false)) {
                reduction += 0.20F;
            }
            if (player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                    .map(info -> info.getRace() == Race.STONE_GIANT).orElse(false)) {
                reduction += 0.05F;
            }
            if (reduction > 0.0F) {
                event.setAmount(event.getAmount() * (1.0F - reduction));
            }
            player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> {
                Race playerRace = raceInfo.getRace();
                DamageSource source = event.getSource();

                if (playerRace == Race.DEMONIO) {
                    if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                            || source.is(DamageTypes.LAVA) || source.is(DamageTypes.HOT_FLOOR)
                            || source.is(TOUGH_AS_NAILS_HYPERTHERMIA)) {
                        event.setCanceled(true);
                    }
                } else if (playerRace == Race.ENDER_WARRIOR) {
                    if (player.hasEffect(com.tcorigenes.tcorigenes.effect.ModEffects.AQUATIC_ENZYME.get())
                            && (source.is(DamageTypes.DROWN) || source.is(ModDamageTypes.WATER_ELEMENTAL))) {
                        event.setCanceled(true);
                    } else if (source.is(DamageTypes.DROWN)) {
                        event.setAmount(event.getAmount() * 1.2F);
                    } else if (source.is(TOUGH_AS_NAILS_THIRST)) {
                        // "No debe tomar agua": inmune a morir de sed. La barra en si sigue
                        // bajando (no tenemos la API de Tough As Nails para tocar eso directo),
                        // pero nunca le hace daño ni le bloquea la regeneracion por estar seco.
                        event.setCanceled(true);
                    }
                } else if (playerRace == Race.MALNACIDO) {
                    // Inmune a veneno/daño instantaneo (magic) y wither: en vez de dañarlo, lo cura.
                    if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.WITHER)) {
                        float damageAmount = event.getAmount();
                        event.setCanceled(true);
                        player.heal(damageAmount);
                    } else if (source.is(DamageTypes.STARVE)) {
                        event.setCanceled(true);
                    }
                } else if (playerRace == Race.DEVOTO && source.getEntity() instanceof LivingEntity attackerEntity
                        && attackerEntity != player) {
                    // Refleja un 10% del daño recibido al atacante.
                    attackerEntity.hurt(player.damageSources().thorns(player), event.getAmount() * 0.10F);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> {
                if (raceInfo.getRace() == Race.ANGEL) {
                    event.setCanceled(true);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }
        if (!event.getObject().getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).isPresent()) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath("tcorigenes", "race"), new PlayerRaceProvider());
        }
        if (!event.getObject().getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY).isPresent()) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath("tcorigenes", "ability_loadout"), new PlayerAbilityLoadoutProvider());
        }
        if (!event.getObject().getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).isPresent()) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath("tcorigenes", "player_class"), new PlayerClassProvider());
        }
        if (!event.getObject().getCapability(com.tcorigenes.tcorigenes.favor.PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY).isPresent()) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath("tcorigenes", "favor"), new com.tcorigenes.tcorigenes.favor.PlayerFavorProvider());
        }
    }

    private static final String DEATH_CARRY_TAG = "TcDeathCarry";

    /**
     * Al morir, Forge invalida las Capabilities de la entidad vieja (Entity.remove ->
     * invalidateCaps()) ANTES de que PlayerList.respawn() cree el jugador nuevo y dispare
     * PlayerEvent.Clone. Eso significa que en onPlayerCloned, event.getOriginal().getCapability(...)
     * ya viene vacio para el caso de muerte real (por eso raza/clase/favor se "perdian" al
     * morir) -- para cambio de dimension SI sigue andando porque ese camino no invalida caps.
     * Como red de seguridad: ademas de intentar copiar via capability (que cubre el caso de
     * cambio de dimension), guardamos un snapshot en persistentData ANTES de que eso pase (aca,
     * en LivingDeathEvent, que corre mucho antes de que el jugador se respawnee de verdad).
     * persistentData no es parte del sistema de Capabilities asi que no se invalida con el resto.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onPlayerDeathSnapshot(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        CompoundTag carry = new CompoundTag();
        player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .ifPresent(raceInfo -> carry.putString("Race", raceInfo.getRace().name()));
        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .ifPresent(classInfo -> carry.putString("Class", classInfo.getPlayerClass().name()));
        player.getCapability(com.tcorigenes.tcorigenes.favor.PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY).ifPresent(favorData -> {
            CompoundTag favorTag = new CompoundTag();
            favorData.getAll().forEach((deity, value) -> favorTag.putInt(deity.name(), value));
            carry.put("Favor", favorTag);
        });
        player.getCapability(com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                .ifPresent(loadout -> {
                    ListTag unlockedTag = new ListTag();
                    loadout.getUnlockedAbilityIds().forEach(id -> unlockedTag.add(StringTag.valueOf(id)));
                    carry.put("Unlocked", unlockedTag);
                    if (loadout.getEquippedAbilityId() != null) {
                        carry.putString("Equipped", loadout.getEquippedAbilityId());
                    }
                    carry.putInt("SkillPoints", loadout.getSkillPoints());
                });

        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(DEATH_CARRY_TAG, carry);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        // Al morir (o cambiar de dimension) Forge reconstruye el Player entero: la entidad
        // vieja se descarta y la nueva arranca con capabilities frescas. Hay que copiar el
        // estado real de la entidad vieja a mano. Para cambio de dimension alcanza con leer la
        // Capability vieja (sigue valida); para muerte real esa Capability ya esta invalidada,
        // asi que ahi hace falta el respaldo de persistentData (ver onPlayerDeathSnapshot).
        CompoundTag carry = event.getEntity().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(DEATH_CARRY_TAG);

        Race liveRace = event.getOriginal().getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(raceInfo -> raceInfo.getRace()).orElse(null);
        Race savedRace = liveRace != null ? liveRace : (carry.contains("Race") ? parseEnum(Race.class, carry.getString("Race")) : null);
        if (savedRace != null) {
            event.getEntity().getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                    .ifPresent(newRace -> newRace.setRace(savedRace));
        }

        PlayerClass liveClass = event.getOriginal().getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                .map(classInfo -> classInfo.getPlayerClass()).orElse(null);
        PlayerClass savedClass = liveClass != null ? liveClass : (carry.contains("Class") ? parseEnum(PlayerClass.class, carry.getString("Class")) : null);
        if (savedClass != null) {
            event.getEntity().getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY)
                    .ifPresent(newClassData -> newClassData.setPlayerClass(savedClass));
        }

        boolean favorFromCapability = event.getOriginal().getCapability(com.tcorigenes.tcorigenes.favor.PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY)
                .map(oldFavor -> {
                    event.getEntity().getCapability(com.tcorigenes.tcorigenes.favor.PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY)
                            .ifPresent(newFavor -> oldFavor.getAll().forEach(newFavor::setFavor));
                    return true;
                }).orElse(false);
        if (!favorFromCapability && carry.contains("Favor")) {
            CompoundTag favorTag = carry.getCompound("Favor");
            event.getEntity().getCapability(com.tcorigenes.tcorigenes.favor.PlayerFavorProvider.PLAYER_FAVOR_CAPABILITY).ifPresent(newFavor -> {
                for (com.tcorigenes.tcorigenes.favor.Deity deity : com.tcorigenes.tcorigenes.favor.Deity.values()) {
                    if (favorTag.contains(deity.name())) {
                        newFavor.setFavor(deity, favorTag.getInt(deity.name()));
                    }
                }
            });
        }

        boolean loadoutFromCapability = event.getOriginal().getCapability(com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                .map(oldLoadout -> {
                    String equippedAbilityId = oldLoadout.getEquippedAbilityId();
                    java.util.Set<String> unlocked = new java.util.HashSet<>(oldLoadout.getUnlockedAbilityIds());
                    int points = oldLoadout.getSkillPoints();
                    event.getEntity().getCapability(com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                            .ifPresent(newLoadout -> {
                                unlocked.forEach(newLoadout::unlockAbility);
                                newLoadout.setEquippedAbilityId(equippedAbilityId);
                                newLoadout.setSkillPoints(points);
                            });
                    return true;
                }).orElse(false);
        if (!loadoutFromCapability && carry.contains("Unlocked")) {
            ListTag unlockedTag = carry.getList("Unlocked", Tag.TAG_STRING);
            event.getEntity().getCapability(com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                    .ifPresent(newLoadout -> {
                        for (int i = 0; i < unlockedTag.size(); i++) {
                            newLoadout.unlockAbility(unlockedTag.getString(i));
                        }
                        if (carry.contains("Equipped")) {
                            newLoadout.setEquippedAbilityId(carry.getString("Equipped"));
                        }
                        newLoadout.setSkillPoints(carry.getInt("SkillPoints"));
                    });
        }

        if (event.getOriginal().getPersistentData().getBoolean("malnacido_purificado")) {
            event.getEntity().getPersistentData().putBoolean("malnacido_purificado", true);
        }
        if (event.getOriginal().getPersistentData().getBoolean("matrimonio_consagrado")) {
            event.getEntity().getPersistentData().putBoolean("matrimonio_consagrado", true);
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String name) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> RaceAttributeManager.updateAttributes(player, raceInfo.getRace()));
        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).ifPresent(classInfo -> ClassAttributeManager.updateAttributes(player, classInfo.getPlayerClass()));
        if (player.getPersistentData().getBoolean("malnacido_purificado")) {
            RaceAttributeManager.applyPurifiedEffects(player);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.tcorigenes.tcorigenes.favor.FavorManager.syncToClient(serverPlayer);
            // Re-manda la raza en cada respawn/cambio de dimension: cubre el caso de que la
            // sincronizacion de login sola no alcance a llegar a tiempo para todos los clientes.
            player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                    .ifPresent(raceInfo -> com.tcorigenes.tcorigenes.core.capability.RaceSync.broadcast(serverPlayer, raceInfo.getRace()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY).ifPresent(raceInfo -> RaceAttributeManager.updateAttributes(player, raceInfo.getRace()));
        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).ifPresent(classInfo -> ClassAttributeManager.updateAttributes(player, classInfo.getPlayerClass()));
        if (player.getPersistentData().getBoolean("malnacido_purificado")) {
            RaceAttributeManager.applyPurifiedEffects(player);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.tcorigenes.tcorigenes.favor.FavorManager.syncToClient(serverPlayer);
            // Le manda a este jugador la raza de todos los demas ya conectados, y avisa la suya
            // propia a todos (incluido el mismo) para que cuernos/alas/altura/piel se vean bien.
            com.tcorigenes.tcorigenes.core.capability.RaceSync.syncAllTo(serverPlayer);
            player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                    .ifPresent(raceInfo -> com.tcorigenes.tcorigenes.core.capability.RaceSync.broadcast(serverPlayer, raceInfo.getRace()));
        }
    }
}
