// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.compat;

import com.tcorigenes.tcorigenes.core.ProgressionTier;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Balance de la magia respecto del equipo (pedido de alejandr0). El daño de un hechizo de Iron's Spellbooks / Ars Nouveau es de escala
 * vanilla (~una espada de diamante, 7); el equipo del pack escala por nivel de progresion: dark metal ~10, netherite 18, fiery / wintry 26,
 * arcane / black steel 34. Por eso el daño de los hechizos de un jugador se multiplica por (espada del nivel / 7), y el costo de maná, el maná
 * maximo y la regeneracion suben juntos segun el nivel, de modo que la cantidad de lanzamientos por barra de maná no cambia pero cada uno
 * pega al nivel del arma de su progresion. El nivel es el de ProgressionTier.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class MagicBalance {
    /** Indice = nivel de progresion (1..4): multiplicador de daño de hechizo = daño de la espada de ese nivel / 7. */
    private static final double[] DAMAGE = {1.0, 10.0 / 7.0, 18.0 / 7.0, 26.0 / 7.0, 34.0 / 7.0};
    /** Multiplicador de costo de maná, de maná maximo y de regeneracion por nivel. */
    private static final double[] MANA = {1.0, 1.0, 1.5, 2.0, 2.5};
    private static final UUID MAX_MANA_UUID = UUID.fromString("5ADE7C4A-2F60-4F33-9C5B-1E5A3D7B0A11");
    private static final UUID REGEN_UUID = UUID.fromString("7B1F2C0E-9A41-4B6D-8E1C-4C2D6F5A3B22");

    private MagicBalance() {
    }

    private static int tier(Player player) {
        return Math.min(4, ProgressionTier.get(player));
    }

    public static double damageMultiplier(Player player) {
        return DAMAGE[tier(player)];
    }

    public static double manaMultiplier(Player player) {
        return MANA[tier(player)];
    }

    public static void register() {
        if (ModList.get().isLoaded("irons_spellbooks")) {
            hook("io.redspace.ironsspellbooks.api.events.SpellDamageEvent", event -> {
                Player caster = casterOfIrons(event);
                if (caster != null) {
                    call(event, "setAmount", new Class<?>[] {float.class},
                            (float) (get(event, "getAmount", Float.class) * damageMultiplier(caster)));
                }
            });
            hook("io.redspace.ironsspellbooks.api.events.SpellOnCastEvent", event -> {
                if (event instanceof net.minecraftforge.event.entity.player.PlayerEvent playerEvent) {
                    int cost = get(event, "getManaCost", Integer.class);
                    call(event, "setManaCost", new Class<?>[] {int.class}, (int) Math.round(cost * manaMultiplier(playerEvent.getEntity())));
                }
            });
        }
        if (ModList.get().isLoaded("ars_nouveau")) {
            hookField("com.hollingsworth.arsnouveau.api.event.SpellDamageEvent$Pre", event -> {
                try {
                    Object caster = event.getClass().getField("caster").get(event);
                    if (caster instanceof Player player) {
                        var field = event.getClass().getField("damage");
                        field.setFloat(event, (float) (field.getFloat(event) * damageMultiplier(player)));
                    }
                } catch (ReflectiveOperationException e) {
                    // sin balance de daño si cambia la API
                }
            });
            hookField("com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent", event -> {
                try {
                    Object context = event.getClass().getField("context").get(event);
                    Object caster = context.getClass().getMethod("getUnwrappedCaster").invoke(context);
                    if (caster instanceof Player player) {
                        var field = event.getClass().getField("currentCost");
                        field.setInt(event, (int) Math.round(field.getInt(event) * manaMultiplier(player)));
                    }
                } catch (ReflectiveOperationException e) {
                    // sin balance de costo si cambia la API
                }
            });
            hook("com.hollingsworth.arsnouveau.api.event.MaxManaCalcEvent", event -> {
                if (event instanceof net.minecraftforge.event.entity.living.LivingEvent living && living.getEntity() instanceof Player player) {
                    call(event, "setMax", new Class<?>[] {int.class}, (int) Math.round(get(event, "getMax", Integer.class) * manaMultiplier(player)));
                }
            });
            hook("com.hollingsworth.arsnouveau.api.event.ManaRegenCalcEvent", event -> {
                if (event instanceof net.minecraftforge.event.entity.living.LivingEvent living && living.getEntity() instanceof Player player) {
                    call(event, "setRegen", new Class<?>[] {double.class}, get(event, "getRegen", Double.class) * manaMultiplier(player));
                }
            });
        }
    }

    /** Maná maximo y regeneracion de Iron's Spellbooks: modificadores por atributo segun el nivel (cada 5 segundos). */
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || event.player.tickCount % 100 != 0
                || !ModList.get().isLoaded("irons_spellbooks")) {
            return;
        }
        double bonus = manaMultiplier(event.player) - 1.0;
        applyModifier(event.player, "max_mana", MAX_MANA_UUID, bonus);
        applyModifier(event.player, "mana_regen", REGEN_UUID, bonus);
    }

    private static void applyModifier(LivingEntity entity, String attributeName, UUID uuid, double amount) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", attributeName));
        AttributeInstance instance = attribute == null ? null : entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(uuid);
        if (current != null && current.getAmount() == amount) {
            return;
        }
        if (current != null) {
            instance.removeModifier(uuid);
        }
        if (amount > 0.0) {
            instance.addPermanentModifier(new AttributeModifier(uuid, "pa_magic_balance", amount, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static Player casterOfIrons(Event event) {
        try {
            Object source = event.getClass().getMethod("getSpellDamageSource").invoke(event);
            Object attacker = source.getClass().getMethod("getEntity").invoke(source);
            return attacker instanceof Player player ? player : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Event event, String method, Class<T> type) {
        try {
            return (T) event.getClass().getMethod(method).invoke(event);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void call(Event event, String method, Class<?>[] types, Object value) {
        try {
            Method m = event.getClass().getMethod(method, types);
            m.invoke(event, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void hookField(String className, Consumer<Event> handler) {
        hook(className, handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hook(String className, Consumer<Event> handler) {
        try {
            Class<? extends Event> type = (Class<? extends Event>) Class.forName(className);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class) type, (Consumer) handler);
        } catch (ReflectiveOperationException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger(MagicBalance.class).warn("No se pudo enganchar {}: {}", className, e.toString());
        }
    }
}
