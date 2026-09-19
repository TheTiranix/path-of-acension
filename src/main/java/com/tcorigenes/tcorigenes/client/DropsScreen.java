package com.tcorigenes.tcorigenes.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

/** Grilla de mobs (un spawn egg por mob) que sueltan el item buscado con /drops. Scroll con la rueda. */
public class DropsScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 6;
    private static final int CELL = 18;

    private record Cell(ItemStack icon, List<Component> tooltip) {
    }

    private final ItemStack searched;
    private final List<Cell> cells = new ArrayList<>();
    private int scrollRow;

    public DropsScreen(ResourceLocation itemId, List<ResourceLocation> entityIds) {
        super(Component.literal("Drops"));
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        this.searched = new ItemStack(item == null ? Items.BARRIER : item);
        for (ResourceLocation id : entityIds) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
            ItemStack icon = new ItemStack(egg != null ? egg : Items.NAME_TAG);
            Component name = type == null ? Component.literal(id.toString()) : type.getDescription();
            cells.add(new Cell(icon, List.of(name, Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY))));
        }
    }

    private int left() {
        return this.width / 2 - COLUMNS * CELL / 2;
    }

    private int top() {
        return this.height / 2 - VISIBLE_ROWS * CELL / 2 + 10;
    }

    private int totalRows() {
        return (cells.size() + COLUMNS - 1) / COLUMNS;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, totalRows() - VISIBLE_ROWS);
        scrollRow = Math.max(0, Math.min(maxScroll, scrollRow - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int left = left();
        int top = top();

        g.fill(left - 6, top - 26, left + COLUMNS * CELL + 6, top + VISIBLE_ROWS * CELL + 6, 0xE0101010);
        g.renderItem(searched, left, top - 22);
        g.drawString(this.font, Component.literal("Lo sueltan (" + cells.size() + " mobs): ").append(searched.getHoverName()),
                left + 22, top - 18, 0xFFFFFF);

        List<Component> hovered = null;
        for (int i = 0; i < VISIBLE_ROWS * COLUMNS; i++) {
            int index = scrollRow * COLUMNS + i;
            int x = left + (i % COLUMNS) * CELL;
            int y = top + (i / COLUMNS) * CELL;
            g.fill(x, y, x + CELL - 1, y + CELL - 1, 0xFF373737);
            if (index >= cells.size()) {
                continue;
            }
            Cell cell = cells.get(index);
            g.renderItem(cell.icon(), x + 1, y + 1);
            if (mouseX >= x && mouseX < x + CELL - 1 && mouseY >= y && mouseY < y + CELL - 1) {
                hovered = cell.tooltip();
            }
        }
        if (cells.isEmpty()) {
            g.drawCenteredString(this.font, "Ningun mob suelta este item.", this.width / 2, top + 20, 0xAAAAAA);
        }
        if (totalRows() > VISIBLE_ROWS) {
            g.drawCenteredString(this.font, "Rueda del mouse para desplazar (" + (scrollRow + 1) + "/" + (totalRows() - VISIBLE_ROWS + 1) + ")",
                    this.width / 2, top + VISIBLE_ROWS * CELL + 10, 0x888888);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (hovered != null) {
            g.renderComponentTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
