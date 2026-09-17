package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tcorigenes.tcorigenes.attributes.RaceAttributeManager;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.RaceSkillGrant;
import com.tcorigenes.tcorigenes.core.capability.PlayerRace;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;

public class SetRaceCommand {
    public SetRaceCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setrace").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("race", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (Race race : Race.values()) {
                                        builder.suggest(race.name().toLowerCase());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String raceName = StringArgumentType.getString(context, "race").toUpperCase();

                                    try {
                                        Race selectedRace = Race.valueOf(raceName);
                                        LazyOptional<PlayerRace.IPlayerRace> raceCapability = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY);
                                        if (raceCapability.isPresent()) {
                                            PlayerRace.IPlayerRace race = raceCapability.resolve().get();
                                            race.setRace(selectedRace);
                                            RaceAttributeManager.updateAttributes(player, selectedRace);
                                            com.tcorigenes.tcorigenes.core.capability.RaceSync.broadcast(player, selectedRace);
                                            RaceSkillGrant.grant(player, selectedRace);

                                            if (selectedRace == Race.DEVOTO) {
                                                com.tcorigenes.tcorigenes.favor.FavorManager.grantDevotoStartingFavor(player);
                                            } else if (selectedRace == Race.HEREJE) {
                                                com.tcorigenes.tcorigenes.favor.FavorManager.clampHerejeFavor(player);
                                            }

                                            if (selectedRace == Race.ENDER_WARRIOR) {
                                                String giveSwordCommand = "give @s testamentodelacarne:espada_anima_1{Enchantments:[{id:\"minecraft:vanishing_curse\",lvl:1s}]}";
                                                String fullCommand = "execute unless entity @s[nbt={Inventory:[{id:\"testamentodelacarne:espada_anima_1\"}]}] run " + giveSwordCommand;
                                                context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), fullCommand);
                                            }
                                        } else {
                                            System.out.println("[TCOrigenes Command] ¡FALLO! La capacidad de raza NO está presente en el jugador: " + player.getName().getString());
                                        }

                                        context.getSource().sendSuccess(() -> Component.literal("Tu raza ha sido establecida a: " + selectedRace.getDisplayName()), true);
                                        return 1;
                                    } catch (IllegalArgumentException e) {
                                        context.getSource().sendFailure(Component.literal("La raza '" + raceName + "' no existe."));
                                        return 0;
                                    }
                                })
                        )
        );
    }
}
