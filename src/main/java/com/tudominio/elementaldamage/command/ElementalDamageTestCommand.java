package com.tudominio.elementaldamage.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tudominio.elementaldamage.ElementalDamageSource;
import com.tudominio.elementaldamage.ModDamageTypes;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * /elementaldamage <elemento> <cantidad> [objetivo] — para probar los 9 tipos de daño sin
 * depender de items/hechizos todavia no implementados. 1.20.1 no tiene el comando vanilla
 * /damage (llego recien en 1.20.4), por eso este comando propio.
 */
@EventBusSubscriber(modid = "elementaldamage")
public final class ElementalDamageTestCommand {
    private static final Map<String, ResourceKey<DamageType>> ELEMENTS = Map.ofEntries(
            Map.entry("fuego", ModDamageTypes.FIRE_ELEMENTAL),
            Map.entry("hielo", ModDamageTypes.ICE),
            Map.entry("agua", ModDamageTypes.WATER_ELEMENTAL),
            Map.entry("luz", ModDamageTypes.LIGHT),
            Map.entry("ender", ModDamageTypes.ENDER_ELEMENTAL),
            Map.entry("lunar", ModDamageTypes.LUNAR),
            Map.entry("tierra", ModDamageTypes.EARTH),
            Map.entry("aire", ModDamageTypes.AIR),
            Map.entry("natural", ModDamageTypes.NATURAL)
    );

    private ElementalDamageTestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("elementaldamage")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("elemento", StringArgumentType.word())
                        .then(Commands.argument("cantidad", FloatArgumentType.floatArg(0.0F))
                                .executes(ElementalDamageTestCommand::runOnSelf)
                                .then(Commands.argument("objetivo", EntityArgument.entity())
                                        .executes(ElementalDamageTestCommand::runOnTarget)))));
    }

    private static int runOnSelf(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Solo un jugador puede usar este comando sin especificar objetivo."));
            return 0;
        }
        return apply(ctx, player, player);
    }

    private static int runOnTarget(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Entity targetEntity = EntityArgument.getEntity(ctx, "objetivo");
        if (!(targetEntity instanceof LivingEntity target)) {
            source.sendFailure(Component.literal("El objetivo tiene que ser una entidad viva."));
            return 0;
        }
        Entity attacker = source.getEntity() != null ? source.getEntity() : target;
        return apply(ctx, target, attacker);
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, LivingEntity target, Entity attacker) {
        CommandSourceStack source = ctx.getSource();
        String elementName = StringArgumentType.getString(ctx, "elemento").toLowerCase(Locale.ROOT);
        ResourceKey<DamageType> element = ELEMENTS.get(elementName);
        if (element == null) {
            source.sendFailure(Component.literal("Elemento invalido. Usa: " + String.join(", ", ELEMENTS.keySet())));
            return 0;
        }
        float amount = FloatArgumentType.getFloat(ctx, "cantidad");
        boolean hurt = ElementalDamageSource.hurt(target, element, attacker, amount);
        if (hurt) {
            source.sendSuccess(() -> Component.literal(
                    "Aplicado " + amount + " de daño " + elementName + " a " + target.getName().getString() + "."), true);
            return 1;
        }
        source.sendFailure(Component.literal("No se pudo aplicar el daño (¿objetivo invulnerable?)."));
        return 0;
    }
}
