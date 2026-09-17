package com.tcorigenes.tcorigenes.progression.client;

import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.ability.PlayerAbility;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.progression.network.UnlockAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Arbol de habilidades PROPIO (no depende de PassiveSkillTree — ese mod ignora nuestros
 * cambios porque "skilltree:main_tree" ya existe embebido en su propio jar y gana por
 * prioridad de carga). Mismo resultado para el jugador: gastar puntos, desbloquear
 * habilidades. Los puntos y el desbloqueo ya viven en PlayerAbilityLoadout.
 */
public class AbilityTreeScreen extends Screen {
    private static final String[] ABILITY_IDS = {
            "tcorigenes:furia_berserker", "tcorigenes:guardia_total",
            "tcorigenes:ojo_de_halcon", "tcorigenes:espiritu_anima"
    };

    public AbilityTreeScreen() {
        super(Component.literal("Árbol de Habilidades"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        for (int i = 0; i < ABILITY_IDS.length; i++) {
            String abilityId = ABILITY_IDS[i];
            PlayerAbility ability = AbilityRegistry.get(abilityId);
            String label = ability != null ? ability.id() : abilityId;
            int y = startY + i * 30;

            this.addRenderableWidget(Button.builder(Component.literal("Desbloquear: " + label + " (2 pts)"), button -> {
                Networking.sendToServer(new UnlockAbilityPacket(abilityId));
            }).bounds(centerX - 120, y, 240, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> this.onClose())
                .bounds(centerX - 50, startY + ABILITY_IDS.length * 30 + 20, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new AbilityTreeScreen());
    }
}
