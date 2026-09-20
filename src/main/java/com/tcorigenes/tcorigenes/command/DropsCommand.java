// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * /drops <item>: abre una grilla (DropsScreen) con los mobs (loot tables "entities/*") que pueden soltar ese item. Lee las
 * loot tables del servidor, asi incluye las de los mods; no ve modificaciones hechas por
 * scripts (LootJS) ni tablas referenciadas desde otras tablas.
 */
public class DropsCommand {
    public DropsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("drops")
                .then(Commands.argument("item", StringArgumentType.greedyString())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                ForgeRegistries.ITEMS.getKeys(), builder))
                        .executes(context -> run(context.getSource(), StringArgumentType.getString(context, "item").trim()))));
    }

    private int run(CommandSourceStack source, String itemArg) {
        ResourceLocation itemId = ResourceLocation.tryParse(itemArg);
        if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId)) {
            source.sendFailure(Component.literal("Item desconocido: " + itemArg));
            return 0;
        }
        var gson = Deserializers.createLootTableSerializer().create();
        var lootData = source.getServer().getLootData();
        String needle = itemId.toString();
        List<ResourceLocation> entityIds = new ArrayList<>();
        for (ResourceLocation tableId : lootData.getKeys(LootDataType.TABLE)) {
            if (!tableId.getPath().startsWith("entities/")) {
                continue;
            }
            LootTable table = lootData.getLootTable(tableId);
            try {
                if (containsItem(gson.toJsonTree(table), needle)) {
                    ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath(tableId.getNamespace(),
                            tableId.getPath().substring("entities/".length()));
                    if (ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
                        entityIds.add(entityId);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            com.tcorigenes.tcorigenes.networking.Networking.sendToPlayer(player,
                    new com.tcorigenes.tcorigenes.networking.packet.DropsResultPacket(itemId, entityIds));
        } else {
            source.sendSuccess(() -> Component.literal("Sueltan " + needle + ": " + entityIds), false);
        }
        return entityIds.size();
    }

    private static boolean containsItem(JsonElement element, String itemId) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("type") && obj.has("name") && itemId.equals(obj.get("name").getAsString())
                    && obj.get("type").getAsString().endsWith("item")) {
                return true;
            }
            for (var entry : obj.entrySet()) {
                if (containsItem(entry.getValue(), itemId)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (containsItem(child, itemId)) {
                    return true;
                }
            }
        }
        return false;
    }
}
