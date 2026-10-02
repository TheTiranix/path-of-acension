// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.intro.client;

import com.tcorigenes.tcorigenes.client.gui.OriginInfo;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.intro.IntroAnswersPacket;
import com.tcorigenes.tcorigenes.intro.IntroContent;
import com.tcorigenes.tcorigenes.networking.Networking;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Introduccion del mundo (estilo Fear and Hunger): cinematica con barras negras donde la camara recorre el cielo, la luna, el mundo y el
 * Aether mientras se cuenta el lore, y preguntas que deciden la raza y la clase (ver IntroContent). Solo muestra y recolecta las respuestas:
 * el resultado lo calcula y lo aplica el servidor.
 */
public class IntroScreen extends Screen {
    private enum Cam { SKY, MOON, WORLD, AETHER, GROUND }

    /** Una escena de narracion (question == -1) o una pregunta. */
    private record Step(List<String> paragraphs, Cam cam, int question) {
        static Step story(Cam cam, String... lines) {
            return new Step(List.of(lines), cam, -1);
        }

        static Step ask(int question) {
            return new Step(List.of(IntroContent.QUESTIONS.get(question).prompt()), null, question);
        }
    }

    private static final int CAM_TICKS = 420;
    private static final float TYPE_SPEED = 1.1F; // caracteres por tick

    private final List<Step> steps = new ArrayList<>();
    private final int[] answers = new int[IntroContent.QUESTIONS.size()];
    private int idx = 0;
    private int stepTicks = 0;
    private float typedChars = 0;
    private boolean typingDone = false;
    // transicion: 0 = ninguna, 1 = saliendo (a negro), 2 = entrando
    private int fadeState = 2;
    private int fadeTick = 0;
    private int fadeInLength = 50;
    private boolean waitingResult = false;
    private int waitTicks = 0;
    private boolean finishing = false;

    // camara
    private float baseYaw;
    private float basePitch;
    private float fromYaw;
    private float fromPitch;
    private float toYaw;
    private float toPitch;
    private int camTick = CAM_TICKS;
    private float camYaw;
    private float camPitch;
    private boolean prevHideGui;

    public IntroScreen() {
        super(Component.literal("Introducción"));
        buildSteps();
    }

    // ------------------------------------------------------------------ contenido
    private void buildSteps() {
        boolean moonVisible = moonElevation() > 0;
        steps.add(Step.story(Cam.SKY,
                "Hubo un tiempo en que el cielo tenía dueños.",
                "Los dioses caminaban sobre las nubes, y la carne del mundo les pertenecía."));
        steps.add(Step.ask(0));
        steps.add(Step.story(Cam.MOON,
                moonVisible ? "Luna sigue ahí arriba. Pálida, constante: la única que no abandonó su puesto."
                        : "Aunque ahora no la veas, Luna sigue ahí arriba. Pálida, constante: la única que no abandonó su puesto.",
                "Dicen que su luz recuerda a todos los que alguna vez la sirvieron."));
        steps.add(Step.ask(1));
        steps.add(Step.story(Cam.WORLD,
                "Entonces vino la Caída.",
                "Una guerra entre dioses desgarró los cielos, y lo que cayó, cayó sobre el mundo.",
                "Sus recuerdos se cristalizaron en la piedra. Sus almas, corruptas, habitan ahora en los monstruos."));
        steps.add(Step.ask(2));
        steps.add(Step.story(Cam.AETHER,
                "Muy arriba, más allá de las nubes, el Aether todavía flota: el paraíso que el Padre de la Carne construyó y al que ya nadie llega.",
                "Pater, Meidris, Filis, Luna. Los dioses menores aún exigen ofrendas a quien quiera escucharlos."));
        steps.add(Step.ask(3));
        steps.add(Step.ask(4));
        steps.add(Step.story(Cam.GROUND,
                "Pero los dioses importan cada vez menos. Lo que importa es cómo vas a sobrevivir.",
                "El único poder real es la sangre, el acero y las almas."));
        for (int q = IntroContent.RACE_QUESTIONS; q < IntroContent.QUESTIONS.size(); q++) {
            steps.add(Step.ask(q));
        }
    }

    // ------------------------------------------------------------------ camara
    /** Elevacion de la luna sobre el horizonte, en grados (negativa = bajo el horizonte). */
    private float moonElevation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 30.0F;
        }
        double theta = mc.level.getTimeOfDay(1.0F) * Math.PI * 2.0;
        return (float) Math.toDegrees(Math.asin(Mth.clamp(-Math.cos(theta), -1.0, 1.0)));
    }

    private float moonYaw() {
        Minecraft mc = Minecraft.getInstance();
        double theta = mc.level == null ? 0 : mc.level.getTimeOfDay(1.0F) * Math.PI * 2.0;
        return Math.sin(theta) >= 0 ? -90.0F : 90.0F; // este = -90, oeste = 90
    }

    private void setCameraTarget(Cam cam) {
        fromYaw = camYaw;
        fromPitch = camPitch;
        switch (cam) {
            case SKY -> {
                toYaw = baseYaw + 30;
                toPitch = -78;
            }
            case MOON -> {
                toYaw = moonYaw();
                toPitch = -Mth.clamp(moonElevation(), 14.0F, 70.0F);
            }
            case WORLD -> {
                toYaw = moonYaw() + 150;
                toPitch = 6;
            }
            case AETHER -> {
                toYaw = moonYaw() + 230;
                toPitch = -38;
            }
            case GROUND -> {
                toYaw = moonYaw() + 250;
                toPitch = 38;
            }
        }
        // el yaw avanza por el camino corto
        toYaw = fromYaw + Mth.wrapDegrees(toYaw - fromYaw);
        camTick = 0;
    }

    private void applyCamera(float partial) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float t = Mth.clamp((camTick + partial) / CAM_TICKS, 0.0F, 1.0F);
        float e = t * t * (3.0F - 2.0F * t);
        camYaw = Mth.lerp(e, fromYaw, toYaw);
        camPitch = Mth.lerp(e, fromPitch, toPitch);
        player.setYRot(camYaw);
        player.setXRot(camPitch);
        player.yRotO = camYaw;
        player.xRotO = camPitch;
    }

    // ------------------------------------------------------------------ ciclo de vida
    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (steps.isEmpty() || idx != 0 || stepTicks != 0) {
            return; // init se vuelve a llamar al cambiar el tamaño de la ventana
        }
        if (mc.player != null) {
            baseYaw = mc.player.getYRot();
            basePitch = mc.player.getXRot();
            camYaw = fromYaw = toYaw = baseYaw;
            camPitch = fromPitch = toPitch = basePitch;
        }
        prevHideGui = mc.options.hideGui;
        mc.options.hideGui = true;
        beginStep();
    }

    @Override
    public void removed() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = prevHideGui;
        LocalPlayer player = mc.player;
        if (player != null) {
            player.setYRot(baseYaw);
            player.setXRot(basePitch);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Step step() {
        return steps.get(Math.min(idx, steps.size() - 1));
    }

    private void beginStep() {
        stepTicks = 0;
        typedChars = 0;
        typingDone = false;
        Step step = step();
        if (step.cam() != null) {
            setCameraTarget(step.cam());
            play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F);
        } else if (step.question() >= 0) {
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F);
        }
    }

    private void play(net.minecraft.sounds.SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    @Override
    public void tick() {
        stepTicks++;
        camTick++;
        if (waitingResult) {
            waitTicks++;
            if (waitTicks > 20 * 12) {
                onClose(); // el servidor no contesto: no dejar al jugador trabado
            }
            return;
        }
        if (!typingDone) {
            typedChars += TYPE_SPEED;
        }
        if (fadeState == 1) {
            if (++fadeTick >= 12) {
                fadeState = 2;
                fadeTick = 0;
                fadeInLength = 24;
                advanceNow();
            }
        } else if (fadeState == 2 && ++fadeTick >= fadeInLength) {
            fadeState = 0;
        }
    }

    // ------------------------------------------------------------------ avance
    private void advance() {
        if (fadeState == 1 || waitingResult) {
            return;
        }
        fadeState = 1;
        fadeTick = 0;
    }

    private void advanceNow() {
        if (finishing) {
            onClose();
            return;
        }
        idx++;
        if (idx >= steps.size()) {
            idx = steps.size() - 1;
            waitingResult = true;
            Networking.sendToServer(new IntroAnswersPacket(answers.clone()));
            return;
        }
        beginStep();
    }

    /** Escena final con la raza y la clase asignadas por el servidor. */
    public static void onResult(Race race, PlayerClass playerClass) {
        if (Minecraft.getInstance().screen instanceof IntroScreen screen) {
            screen.showResult(race, playerClass);
        }
    }

    private void showResult(Race race, PlayerClass playerClass) {
        steps.clear();
        steps.add(new Step(List.of(
                "Despertás como " + race.getDisplayName() + ".",
                OriginInfo.tagline(race),
                "Tu camino es el del " + playerClass.getDisplayName() + ". " + OriginInfo.tagline(playerClass),
                "Que los dioses, o su ausencia, te acompañen."), Cam.SKY, -1));
        idx = 0;
        finishing = true;
        waitingResult = false;
        fadeState = 2;
        fadeTick = 0;
        fadeInLength = 40;
        beginStep();
    }

    // ------------------------------------------------------------------ entrada
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (fadeState == 1 || waitingResult) {
            return true;
        }
        Step step = step();
        if (!typingDone) {
            typedChars = Float.MAX_VALUE / 4; // saltea la maquina de escribir
            return true;
        }
        if (step.question() >= 0) {
            int option = optionAt(mouseX, mouseY);
            if (option >= 0) {
                choose(step.question(), option);
            }
            return true;
        }
        advance();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (fadeState == 1 || waitingResult) {
            return true;
        }
        Step step = step();
        boolean confirm = key == 257 || key == 32 || key == 335; // enter, espacio
        if (!typingDone && confirm) {
            typedChars = Float.MAX_VALUE / 4;
            return true;
        }
        if (typingDone && step.question() >= 0 && key >= 49 && key <= 53) {
            int option = key - 49;
            if (option < IntroContent.QUESTIONS.get(step.question()).options().size()) {
                choose(step.question(), option);
            }
            return true;
        }
        if (typingDone && step.question() < 0 && confirm) {
            advance();
            return true;
        }
        return true; // se traga todo el resto (ESC incluido)
    }

    private void choose(int question, int option) {
        answers[question] = option;
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
        advance();
    }

    // ------------------------------------------------------------------ dibujo
    private List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        for (var part : font.getSplitter().splitLines(text, width, Style.EMPTY)) {
            out.add(part.getString());
        }
        return out;
    }

    private int barHeight() {
        return (int) (height * 0.12F);
    }

    private int textWidth() {
        return Math.min(width - 80, 520);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        applyCamera(partial);
        int bar = barHeight();
        g.fill(0, 0, width, bar, 0xFF000000);
        g.fill(0, height - bar, width, height, 0xFF000000);

        if (waitingResult) {
            g.fill(0, 0, width, height, 0xFF000000);
            return;
        }
        Step step = step();
        float shown = typedChars + (typingDone ? 0 : partial * TYPE_SPEED);
        int tw = textWidth();
        int x = (width - tw) / 2;

        if (step.question() >= 0) {
            renderQuestion(g, step, shown, mouseX, mouseY, tw);
        } else {
            // narracion: parrafos ya envueltos, revelados de a caracteres, en la franja inferior
            List<List<String>> wrapped = new ArrayList<>();
            int lines = 0;
            for (String paragraph : step.paragraphs()) {
                List<String> w = wrap(paragraph, tw);
                wrapped.add(w);
                lines += w.size() + 1;
            }
            int total = 0;
            for (List<String> w : wrapped) {
                for (String line : w) {
                    total += line.length();
                }
            }
            typingDone = shown >= total;
            int y = height - bar - 10 - lines * (font.lineHeight + 2);
            g.fill(x - 14, y - 8, x + tw + 14, height - bar - 2, 0x99000000);
            int left = (int) shown;
            for (List<String> w : wrapped) {
                for (String line : w) {
                    int take = Math.min(left, line.length());
                    if (take > 0) {
                        g.drawString(font, line.substring(0, take), x, y, 0xFFEDE3CF, true);
                    }
                    left -= take;
                    y += font.lineHeight + 2;
                }
                y += font.lineHeight + 2;
            }
            if (typingDone && (stepTicks / 12) % 2 == 0) {
                g.drawCenteredString(font, finishing ? "[ clic para empezar ]" : "[ clic para continuar ]", width / 2, height - bar + 6, 0xFF8F8878);
            }
        }

        // fundido a negro
        float alpha = 0;
        if (fadeState == 1) {
            alpha = Mth.clamp((fadeTick + partial) / 12.0F, 0, 1);
        } else if (fadeState == 2) {
            alpha = 1.0F - Mth.clamp((fadeTick + partial) / fadeInLength, 0, 1);
        }
        if (alpha > 0) {
            g.fill(0, 0, width, height, ((int) (alpha * 255) << 24));
        }
    }

    // posiciones de las opciones (se recalculan en cada llamada para el click y el dibujo)
    private int[] optionTops;
    private int[] optionHeights;
    private List<List<String>> optionLines;

    private void layoutOptions(Step step, int tw) {
        var options = IntroContent.QUESTIONS.get(step.question()).options();
        optionLines = new ArrayList<>();
        optionTops = new int[options.size()];
        optionHeights = new int[options.size()];
        int promptLines = wrap(step.paragraphs().get(0), tw).size();
        int total = promptLines * (font.lineHeight + 2) + 18;
        for (int i = 0; i < options.size(); i++) {
            List<String> lines = wrap((i + 1) + ".  " + options.get(i).text(), tw - 16);
            optionLines.add(lines);
            optionHeights[i] = lines.size() * (font.lineHeight + 2) + 10;
            total += optionHeights[i] + 4;
        }
        int bar = barHeight();
        int top = bar + (height - 2 * bar - total) / 2;
        int y = top + promptLines * (font.lineHeight + 2) + 18;
        for (int i = 0; i < options.size(); i++) {
            optionTops[i] = y;
            y += optionHeights[i] + 4;
        }
    }

    private int optionAt(double mx, double my) {
        Step step = step();
        if (step.question() < 0) {
            return -1;
        }
        layoutOptions(step, textWidth());
        int x = (width - textWidth()) / 2;
        for (int i = 0; i < optionTops.length; i++) {
            if (mx >= x && mx <= x + textWidth() && my >= optionTops[i] && my <= optionTops[i] + optionHeights[i]) {
                return i;
            }
        }
        return -1;
    }

    private void renderQuestion(GuiGraphics g, Step step, float shown, int mouseX, int mouseY, int tw) {
        layoutOptions(step, tw);
        int x = (width - tw) / 2;
        List<String> prompt = wrap(step.paragraphs().get(0), tw);
        int promptTotal = 0;
        for (String line : prompt) {
            promptTotal += line.length();
        }
        typingDone = shown >= promptTotal;
        int bar = barHeight();
        int py = optionTops[0] - 18 - prompt.size() * (font.lineHeight + 2);
        g.fill(x - 14, py - 8, x + tw + 14, optionTops[optionTops.length - 1] + optionHeights[optionHeights.length - 1] + 8, 0xAA000000);
        int left = (int) shown;
        for (String line : prompt) {
            int take = Math.min(left, line.length());
            if (take > 0) {
                g.drawString(font, line.substring(0, take), x, py, 0xFFF4D98A, true);
            }
            left -= take;
            py += font.lineHeight + 2;
        }
        if (!typingDone) {
            return;
        }
        int hover = optionAt(mouseX, mouseY);
        for (int i = 0; i < optionTops.length; i++) {
            boolean over = i == hover;
            g.fill(x, optionTops[i], x + tw, optionTops[i] + optionHeights[i], over ? 0xCC3A3320 : 0x88000000);
            if (over) {
                g.renderOutline(x, optionTops[i], tw, optionHeights[i], 0xFFC9A44C);
            }
            int ty = optionTops[i] + 5;
            for (String line : optionLines.get(i)) {
                g.drawString(font, line, x + 8, ty, over ? 0xFFFFFFFF : 0xFFCFC6B2, true);
                ty += font.lineHeight + 2;
            }
        }
    }
}
