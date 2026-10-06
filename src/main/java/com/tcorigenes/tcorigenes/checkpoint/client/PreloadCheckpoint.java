// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.checkpoint.client;

import com.tcorigenes.tcorigenes.checkpoint.CheckpointSnapshotter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Elegir el punto de guardado ANTES de cargar el mundo (desde "Seleccionar mundo"), para no cargar el mundo dos veces: antes el save se
 * elegia dentro del mundo, que despues se cerraba para restaurar y habia que entrar de nuevo. Aca el mundo esta cerrado: se copia el snapshot
 * elegido sobre el mundo y se entra a jugar en una sola carga (se marca para que el juego no vuelva a ofrecer la lista al entrar).
 */
@EventBusSubscriber(modid = "tcorigenes", value = Dist.CLIENT)
public final class PreloadCheckpoint {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SKIP_FILE = "skip_load_screen.txt";

    private PreloadCheckpoint() {
    }

    record Entry(UUID id, int number, long day, boolean initial) {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof SelectWorldScreen screen)) {
            return;
        }
        int width = screen.width;
        event.addListener(Button.builder(Component.translatable("tcorigenes.preload.button"), button -> {
            String levelId = selectedLevelId(screen);
            if (levelId == null) {
                return;
            }
            Minecraft.getInstance().setScreen(new PickScreen(screen, levelId));
        }).bounds(Math.max(4, width - 136), 4, 132, 18).build());
    }

    /** Carpeta (id) del mundo marcado en la lista, o null si no hay ninguno. Se busca por tipo para no depender de los nombres internos. */
    private static String selectedLevelId(SelectWorldScreen screen) {
        try {
            for (Field field : SelectWorldScreen.class.getDeclaredFields()) {
                if (WorldSelectionList.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    WorldSelectionList list = (WorldSelectionList) field.get(screen);
                    var selected = list.getSelected();
                    if (selected instanceof WorldSelectionList.WorldListEntry entry) {
                        for (Field f : WorldSelectionList.WorldListEntry.class.getDeclaredFields()) {
                            if (f.getType() == LevelSummary.class) {
                                f.setAccessible(true);
                                return ((LevelSummary) f.get(entry)).getLevelId();
                            }
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[tcorigenes] No se pudo leer el mundo seleccionado", e);
        }
        return null;
    }

    static List<Entry> readEntries(Path gameDir, String levelId) {
        List<Entry> result = new ArrayList<>();
        Path snapshots = gameDir.resolve("checkpoint_snapshots");
        Path dat = gameDir.resolve("saves").resolve(levelId).resolve("data").resolve("tcorigenes_checkpoints.dat");
        if (Files.isRegularFile(dat)) {
            try {
                CompoundTag root = NbtIo.readCompressed(dat.toFile());
                ListTag list = root.getCompound("data").getList("checkpoints", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag tag = list.getCompound(i);
                    UUID id = tag.getUUID("id");
                    if (Files.isDirectory(snapshots.resolve(id.toString()))) {
                        result.add(new Entry(id, tag.getInt("number"), tag.getLong("placedAt") / 24000L + 1L, false));
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("[tcorigenes] No se pudieron leer los puntos de guardado de {}", levelId, e);
            }
        }
        result.sort(Comparator.comparingInt(Entry::number));
        if (Files.isDirectory(snapshots.resolve(CheckpointSnapshotter.INITIAL_ID.toString()))) {
            result.add(0, new Entry(CheckpointSnapshotter.INITIAL_ID, 0, 1, true));
        }
        return result;
    }

    /** Pantalla con la lista de puntos de guardado del mundo marcado. */
    static final class PickScreen extends Screen {
        private static final int PER_PAGE = 6;

        private final Screen parent;
        private final String levelId;
        private final List<Entry> entries;
        private int page;
        private volatile String status = "";
        private volatile boolean busy;

        PickScreen(Screen parent, String levelId) {
            super(Component.translatable("tcorigenes.preload.title"));
            this.parent = parent;
            this.levelId = levelId;
            this.entries = readEntries(Minecraft.getInstance().gameDirectory.toPath(), levelId);
        }

        @Override
        protected void init() {
            int cx = this.width / 2;
            int y = this.height / 2 - 60;
            int from = page * PER_PAGE;
            for (int i = from; i < Math.min(entries.size(), from + PER_PAGE); i++) {
                Entry entry = entries.get(i);
                Component label = entry.initial()
                        ? Component.translatable("tcorigenes.preload.initial")
                        : Component.translatable("tcorigenes.preload.entry", entry.number(), entry.day());
                addRenderableWidget(Button.builder(label, button -> choose(entry)).bounds(cx - 100, y, 200, 20).build());
                y += 22;
            }
            if (entries.size() > PER_PAGE) {
                addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildWidgets();
                }).bounds(cx - 100, y + 4, 40, 18).build());
                addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    if ((page + 1) * PER_PAGE < entries.size()) {
                        page++;
                    }
                    rebuildWidgets();
                }).bounds(cx + 60, y + 4, 40, 18).build());
            }
            addRenderableWidget(Button.builder(Component.translatable("tcorigenes.preload.continue"), button -> {
                markSkip();
                minecraft.setScreen(parent);
            }).bounds(cx - 100, this.height / 2 + 82, 200, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> minecraft.setScreen(parent))
                    .bounds(cx - 100, this.height / 2 + 104, 200, 20).build());
        }

        private static void markSkip() {
            try {
                Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve(SKIP_FILE), "1", StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.warn("[tcorigenes] No se pudo escribir la marca de carga", e);
            }
        }

        private void choose(Entry entry) {
            if (busy) {
                return;
            }
            busy = true;
            status = Component.translatable("tcorigenes.preload.working").getString();
            Path gameDir = minecraft.gameDirectory.toPath();
            Path worldRoot = gameDir.resolve("saves").resolve(levelId);
            Path snapshot = gameDir.resolve("checkpoint_snapshots").resolve(entry.id().toString());
            Thread thread = new Thread(() -> {
                IOException error = null;
                for (int attempt = 0; attempt < 20; attempt++) {
                    try {
                        CheckpointSnapshotter.restoreFromSnapshot(worldRoot, snapshot);
                        error = null;
                        break;
                    } catch (IOException e) {
                        error = e;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                IOException finalError = error;
                minecraft.execute(() -> finish(finalError));
            }, "tcorigenes-preload-restore");
            thread.setDaemon(true);
            thread.start();
        }

        private void finish(IOException error) {
            busy = false;
            if (error != null) {
                LOGGER.error("[tcorigenes] No se pudo restaurar el punto de guardado", error);
                status = Component.translatable("tcorigenes.preload.failed").getString();
                return;
            }
            markSkip();
            minecraft.setScreen(parent);
            // entra al mundo de una: el boton "Jugar el mundo seleccionado" de la lista
            for (GuiEventListener child : parent.children()) {
                if (child instanceof Button button && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                        && tc.getKey().equals("selectWorld.select")) {
                    button.onPress();
                    return;
                }
            }
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
            renderBackground(g);
            g.drawCenteredString(font, title, width / 2, height / 2 - 90, 0xFFE8C84A);
            g.drawCenteredString(font, Component.translatable("tcorigenes.preload.hint"), width / 2, height / 2 - 76, 0xFFAAAAAA);
            if (entries.isEmpty()) {
                g.drawCenteredString(font, Component.translatable("tcorigenes.preload.none"), width / 2, height / 2 - 40, 0xFFFFFFFF);
            }
            if (!status.isEmpty()) {
                g.drawCenteredString(font, status, width / 2, height / 2 + 70, 0xFFFFD700);
            }
            super.render(g, mouseX, mouseY, partial);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return !busy;
        }
    }
}
