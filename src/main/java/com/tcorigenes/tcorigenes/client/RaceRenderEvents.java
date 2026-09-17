package com.tcorigenes.tcorigenes.client;

import com.tcorigenes.tcorigenes.core.Race;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Registra RaceFeaturesLayer en los dos renderers de skin de jugador (default/slim); escala
 * visualmente al Ender Warrior (+0.5 bloques, mas alto que ancho); deforma la cabeza/brazo del
 * Malnacido. Todo esto en Pre/Post (no en un RenderLayer normal) porque tiene que aplicarse
 * ANTES del pase base del modelo -- asi el Malnacido se ve deforme pero CON SU PIEL REAL, no una
 * textura nueva encima. La escala del Ender Warrior es SOLO visual: en 1.20.1 no existe el
 * atributo generic.scale de jugador (llego en 1.20.5+), asi que el hitbox real sigue siendo el
 * tamaño vanilla. Es un compromiso consciente (mismo que se hablo antes de riesgo/beneficio).
 */
public final class RaceRenderEvents {
    // No uniforme a proposito: "mas alto, mas alargado que ancho" (no solo "mas grande en general").
    private static final float ENDER_WARRIOR_HEIGHT_SCALE = 1.32F;
    private static final float ENDER_WARRIOR_WIDTH_SCALE = 1.05F;
    private static final float MALNACIDO_HEAD_SCALE = 1.35F;
    private static final float MALNACIDO_ARM_SCALE = 1.45F;

    private RaceRenderEvents() {
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skinName : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skinName);
            if (renderer != null) {
                renderer.addLayer(new RaceFeaturesLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        Race race = ClientRaceData.get(player.getUUID());
        if (race == Race.ENDER_WARRIOR) {
            event.getPoseStack().pushPose();
            event.getPoseStack().scale(ENDER_WARRIOR_WIDTH_SCALE, ENDER_WARRIOR_HEIGHT_SCALE, ENDER_WARRIOR_WIDTH_SCALE);
        } else if (race == Race.MALNACIDO && !ClientRaceData.isPurified(player.getUUID())) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            setScale(model.head, MALNACIDO_HEAD_SCALE);
            setScale(model.rightArm, MALNACIDO_ARM_SCALE);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Race race = ClientRaceData.get(player.getUUID());
        if (race == Race.ENDER_WARRIOR) {
            event.getPoseStack().popPose();
        } else if (race == Race.MALNACIDO && !ClientRaceData.isPurified(player.getUUID())) {
            // Son los mismos ModelPart reusados cada frame: resetear o la deformacion se cuela
            // en otros lugares que usan el mismo modelo (inventario, otras entidades, etc).
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            setScale(model.head, 1.0F);
            setScale(model.rightArm, 1.0F);
        }
    }

    private static void setScale(net.minecraft.client.model.geom.ModelPart part, float scale) {
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }
}
