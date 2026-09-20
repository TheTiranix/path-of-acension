package com.tcorigenes.tcorigenes.ability.client;

import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.ability.PlayerAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Icono 16x16 abajo al centro con el tiempo de cooldown restante encima (en segundos).
 * Muestra la ultima habilidad que el jugador intento usar (alcanza para la habilidad de
 * prueba; cuando haya mas de una habilidad equipable simultanea, esto hay que expandirlo
 * a una fila de iconos en vez de uno solo).
 */
public class AbilityHudOverlay implements IGuiOverlay {
    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        String abilityId = ClientAbilityCooldowns.getLastAbilityId();
        if (abilityId == null) {
            return;
        }
        PlayerAbility ability = AbilityRegistry.get(abilityId);
        if (ability == null) {
            return;
        }

        int remainingMillis = ClientAbilityCooldowns.getRemainingMillis(abilityId);
        // A la derecha de la hotbar (el indicador de temperatura de Tough As Nails ocupa el centro, encima de ella).
        int x = screenWidth / 2 + 122;
        int y = screenHeight - 20;

        guiGraphics.blit(ability.icon(), x, y, 0, 0, 16, 16, 16, 16);

        if (remainingMillis > 0) {
            guiGraphics.fill(x, y, x + 16, y + 16, 0x90000000);
            String seconds = String.valueOf((remainingMillis / 1000) + 1);
            Minecraft mc = Minecraft.getInstance();
            guiGraphics.drawCenteredString(mc.font, seconds, x + 8, y + 4, 0xFFFFFF);
        }
    }
}
