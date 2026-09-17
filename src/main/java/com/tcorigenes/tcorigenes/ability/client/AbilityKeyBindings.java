package com.tcorigenes.tcorigenes.ability.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tcorigenes.tcorigenes.ability.network.ActivateAbilityPacket;
import com.tcorigenes.tcorigenes.ability.network.ActivateRacialAbilityPacket;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.progression.client.AbilityTreeScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public class AbilityKeyBindings {
    public static final KeyMapping ACTIVATE_ABILITY = new KeyMapping(
            "key.tcorigenes.activate_ability", InputConstants.Type.KEYSYM, InputConstants.KEY_G, "key.categories.tcorigenes");

    public static final KeyMapping OPEN_ABILITY_TREE = new KeyMapping(
            "key.tcorigenes.open_ability_tree", InputConstants.Type.KEYSYM, InputConstants.KEY_H, "key.categories.tcorigenes");

    public static final KeyMapping ACTIVATE_RACIAL = new KeyMapping(
            "key.tcorigenes.activate_racial", InputConstants.Type.KEYSYM, InputConstants.KEY_J, "key.categories.tcorigenes");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_ABILITY);
        event.register(OPEN_ABILITY_TREE);
        event.register(ACTIVATE_RACIAL);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (ACTIVATE_ABILITY.consumeClick()) {
            Networking.sendToServer(new ActivateAbilityPacket());
        }
        while (OPEN_ABILITY_TREE.consumeClick()) {
            AbilityTreeScreen.open();
        }
        while (ACTIVATE_RACIAL.consumeClick()) {
            Networking.sendToServer(new ActivateRacialAbilityPacket());
        }
    }
}
