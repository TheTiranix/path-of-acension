package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.command.AbilityDebugCommand;
import com.tcorigenes.tcorigenes.command.FavorCommand;
import com.tcorigenes.tcorigenes.command.GrantSkillPointsCommand;
import com.tcorigenes.tcorigenes.command.SetAbilityCommand;
import com.tcorigenes.tcorigenes.command.SetClassCommand;
import com.tcorigenes.tcorigenes.command.SetRaceCommand;
import com.tcorigenes.tcorigenes.faction.FactionNpcCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = "tcorigenes")
public class ModCommands {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new SetRaceCommand(event.getDispatcher());
        new SetClassCommand(event.getDispatcher());
        new SetAbilityCommand(event.getDispatcher());
        new GrantSkillPointsCommand(event.getDispatcher());
        new AbilityDebugCommand(event.getDispatcher());
        new FavorCommand(event.getDispatcher());
        FactionNpcCommand.register(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }
}
