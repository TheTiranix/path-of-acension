package com.tcorigenes.tcorigenes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.ability.PlayerAbility;
import com.tcorigenes.tcorigenes.ability.capability.PlayerAbilityLoadoutProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de admin (nivel de permiso 2): fuerza equipar cualquier habilidad, IGNORANDO si
 * el jugador la desbloqueo o no en el arbol. Los jugadores normales desbloquean y equipan
 * habilidades organicamente al gastar puntos en los nodos "clave" de su rama del arbol de habilidades
 * (ver SkillTreeAbilityBridge). Esto es solo para testear.
 */
public class SetAbilityCommand {
    public SetAbilityCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setability").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("ability", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("grito_de_guerra");
                                    builder.suggest("furia_berserker");
                                    builder.suggest("guardia_total");
                                    builder.suggest("ojo_de_halcon");
                                    builder.suggest("espiritu_anima");
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String abilityName = StringArgumentType.getString(context, "ability");
                                    String abilityId = abilityName.contains(":") ? abilityName : "tcorigenes:" + abilityName;

                                    PlayerAbility ability = AbilityRegistry.get(abilityId);
                                    if (ability == null) {
                                        context.getSource().sendFailure(Component.literal("La habilidad '" + abilityName + "' no existe."));
                                        return 0;
                                    }

                                    player.getCapability(PlayerAbilityLoadoutProvider.ABILITY_LOADOUT_CAPABILITY)
                                            .ifPresent(loadout -> loadout.setEquippedAbilityId(abilityId));

                                    context.getSource().sendSuccess(() -> Component.literal("Habilidad equipada: " + abilityId), true);
                                    return 1;
                                })
                        )
        );
    }
}
