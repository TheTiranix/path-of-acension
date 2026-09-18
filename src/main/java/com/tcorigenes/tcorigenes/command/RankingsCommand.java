package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tcorigenes.tcorigenes.compat.ItemStatRanking;
import com.tcorigenes.tcorigenes.compat.jei.StatEntry;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /rankings damage|armor [pagina]: para poder ver en el juego (sin abrir JEI) las mismas listas
 * ordenadas de menor a mayor daño/armadura que ya existen como categorias de JEI (ver
 * ModJeiPlugin, comparten los datos via ItemStatRanking). Abierto a cualquier jugador.
 */
public class RankingsCommand {
    private static final int PAGE_SIZE = 10;
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    public RankingsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rankings")
                .then(Commands.argument("stat", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("damage");
                            builder.suggest("armor");
                            return builder.buildFuture();
                        })
                        .executes(context -> show(context.getSource(), StringArgumentType.getString(context, "stat"), 1))
                        .then(Commands.argument("pagina", IntegerArgumentType.integer(1))
                                .executes(context -> show(context.getSource(),
                                        StringArgumentType.getString(context, "stat"),
                                        IntegerArgumentType.getInteger(context, "pagina"))))));
    }

    private int show(CommandSourceStack source, String stat, int page) {
        List<StatEntry> entries;
        String label;
        if (stat.equalsIgnoreCase("damage") || stat.equalsIgnoreCase("dano") || stat.equalsIgnoreCase("daño")) {
            entries = ItemStatRanking.damageRanking();
            label = "Daño";
        } else if (stat.equalsIgnoreCase("armor") || stat.equalsIgnoreCase("armadura")) {
            entries = ItemStatRanking.armorRanking();
            label = "Armadura";
        } else {
            source.sendFailure(Component.literal("Usa /rankings damage o /rankings armor"));
            return 0;
        }

        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int clampedPage = Math.min(page, totalPages);
        int from = (clampedPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, entries.size());

        source.sendSuccess(() -> Component.literal(
                "== " + label + " (pagina " + clampedPage + "/" + totalPages + ", " + entries.size() + " items, menor a mayor) =="), false);
        for (int i = from; i < to; i++) {
            StatEntry entry = entries.get(i);
            int rank = i + 1;
            source.sendSuccess(() -> Component.literal(
                    rank + ". " + entry.stack().getHoverName().getString() + " - " + FORMAT.format(entry.value())), false);
        }
        if (clampedPage < totalPages) {
            int nextPage = clampedPage + 1;
            source.sendSuccess(() -> Component.literal(
                    "Usa /rankings " + stat + " " + nextPage + " para ver la siguiente pagina."), false);
        }
        return 1;
    }
}
