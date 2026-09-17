package com.tudominio.testamentodelacarne.networking.packet;

import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Paquete cliente->servidor que confirma el Pacto elegido en PactSelectionScreen.
 * "sangre": cuesta 4 corazones de vida maxima (requiere >20 HP), otorga el advancement de sangre.
 * "acero": cuesta 32 niveles de experiencia, otorga el advancement de acero.
 * Ambos marcan "pacto_elegido"=true en la persistent data del jugador (un solo pacto por partida).
 */
public class ChoosePactPacket {
    private final String pactId;

    public ChoosePactPacket(String pactId) {
        this.pactId = pactId;
    }

    public ChoosePactPacket(FriendlyByteBuf buf) {
        this.pactId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.pactId);
    }

    public boolean handle(Supplier<Context> supplier) {
        Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.getPersistentData().getBoolean("pacto_elegido")) {
                return;
            }

            CommandSourceStack source = player.getServer().createCommandSourceStack().withPermission(4);
            String playerName = player.getGameProfile().getName();

            if (this.pactId.equals("sangre")) {
                if (player.getMaxHealth() > 20.0F) {
                    player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getMaxHealth() - 4.0);
                    player.getServer().getCommands().performPrefixedCommand(source,
                            "advancement grant " + playerName + " only testamentodelacarne:otorgar_pacto_sangre");
                    player.displayClientMessage(Component.literal("§cHas sellado el pacto de sangre. Un nuevo poder fluye a través de ti."), false);
                    player.getPersistentData().putBoolean("pacto_elegido", true);
                } else {
                    player.displayClientMessage(Component.literal("§4No tienes suficiente vitalidad para soportar este pacto."), false);
                }
            } else if (this.pactId.equals("acero")) {
                if (player.experienceLevel >= 32) {
                    player.giveExperienceLevels(-32);
                    player.getServer().getCommands().performPrefixedCommand(source,
                            "advancement grant " + playerName + " only testamentodelacarne:otorgar_pacto_acero");
                    player.displayClientMessage(Component.literal("§bHas aceptado el dogma de acero. Has alcanzado una nueva fuerza."), false);
                    player.getPersistentData().putBoolean("pacto_elegido", true);
                } else {
                    player.displayClientMessage(Component.literal("§3No tienes suficiente experiencia para comprender este dogma."), false);
                }
            }
        });
        return true;
    }
}
