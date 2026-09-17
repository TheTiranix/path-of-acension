package com.tcorigenes.tcorigenes.faction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tcorigenes.tcorigenes.faction.entity.FactionNpcEntity;
import com.tcorigenes.tcorigenes.faction.structure.FactionCampSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /factionnpc spawn <faccion> — invoca un NPC de prueba en la posicion del jugador.
 * /factionnpc locate <faccion> — busca el campamento mas cercano ya generado en esta dimension
 * (ver FactionCampSavedData; solo encuentra los generados despues de agregar el sistema de
 * campamentos, no hay forma de indexar retroactivamente chunks viejos).
 */
public final class FactionNpcCommand {
    private FactionNpcCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("factionnpc")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("faccion", StringArgumentType.word())
                                .executes(FactionNpcCommand::spawn)))
                .then(Commands.literal("locate")
                        .then(Commands.argument("faccion", StringArgumentType.word())
                                .executes(FactionNpcCommand::locate))));
    }

    private static Faction parseFaction(CommandSourceStack source, com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return Faction.valueOf(StringArgumentType.getString(ctx, "faccion").toUpperCase(java.util.Locale.ROOT));
    }

    private static int spawn(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        Faction faction;
        try {
            faction = parseFaction(source, ctx);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Faccion invalida. Usa: pater, filis, meidris, luna, deiros, tempo"));
            return 0;
        }
        FactionNpcEntity npc = ModEntityTypes.FACTION_NPC.get().create(player.serverLevel());
        if (npc != null) {
            npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
            npc.setFaction(faction);
            player.serverLevel().addFreshEntity(npc);
            source.sendSuccess(() -> Component.literal("NPC de " + faction.getDisplayName() + " invocado."), true);
        }
        return 1;
    }

    private static int locate(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        Faction faction;
        try {
            faction = parseFaction(source, ctx);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Faccion invalida. Usa: pater, filis, meidris, luna, deiros, tempo"));
            return 0;
        }
        FactionCampSavedData data = FactionCampSavedData.get(player.serverLevel());
        BlockPos nearest = data.findNearest(faction, player.blockPosition());
        if (nearest == null) {
            source.sendFailure(Component.literal("No se conoce ningun campamento de " + faction.getDisplayName()
                    + " en esta dimension todavia (solo se registran los generados en chunks nuevos)."));
            return 0;
        }
        double distance = Math.sqrt(player.blockPosition().distSqr(nearest));
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                "Campamento de %s mas cercano: %d, %d, %d (a %.0f bloques)",
                faction.getDisplayName(), nearest.getX(), nearest.getY(), nearest.getZ(), distance)), false);
        return 1;
    }
}
