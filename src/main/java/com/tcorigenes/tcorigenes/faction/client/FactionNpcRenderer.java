// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.faction.client;

import com.tcorigenes.tcorigenes.TCOrigenes;
import com.tcorigenes.tcorigenes.faction.entity.FactionNpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Reutiliza el modelo humanoide base de vanilla (mismo esqueleto que el jugador/zombie);
 * lo unico propio es la textura, que cambia segun la faccion del NPC (ver Faction).
 */
public class FactionNpcRenderer extends HumanoidMobRenderer<FactionNpcEntity, HumanoidModel<FactionNpcEntity>> {
    public FactionNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FactionNpcEntity entity) {
        String faction = entity.getFaction().name().toLowerCase(java.util.Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath(TCOrigenes.MOD_ID, "textures/entity/faction/" + faction + ".png");
    }
}
