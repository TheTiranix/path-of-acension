// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.favor.client;

import com.tcorigenes.tcorigenes.favor.Deity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Lista compacta arriba a la izquierda con el favor actual de cada dios. Solo se dibuja si el
 * jugador tiene favor distinto de 0 con al menos uno (para no ensuciar la pantalla con ceros
 * apenas arranca la partida). Verde para favor positivo, rojo para negativo.
 */
public class FavorHudOverlay implements IGuiOverlay {
    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!ClientFavorData.hasAny()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int x = 4;
        int y = 4;
        guiGraphics.drawString(mc.font, "Favor de los dioses", x, y, 0xC0C0C0);
        y += 10;
        for (Deity deity : Deity.values()) {
            int value = ClientFavorData.getFavor(deity);
            if (value == 0) {
                continue;
            }
            int color = value > 0 ? 0x55FF55 : 0xFF5555;
            String line = deity.getDisplayName() + ": " + (value > 0 ? "+" : "") + value;
            guiGraphics.drawString(mc.font, line, x, y, color);
            y += 10;
        }
    }
}
