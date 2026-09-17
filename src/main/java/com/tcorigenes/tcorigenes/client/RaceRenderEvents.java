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
 * Registra RaceFeaturesLayer en los dos renderers de skin de jugador (default/slim), y escala
 * visualmente al Ender Warrior (+0.5 bloques, mas alto que ancho) en Pre/Post. La escala del
 * Ender Warrior es SOLO visual: en 1.20.1 no existe el atributo generic.scale de jugador (llego
 * en 1.20.5+), asi que el hitbox real sigue siendo el tamaño vanilla. Es un compromiso consciente
 * (mismo que se hablo antes de riesgo/beneficio).
 *
 * La deformidad del Malnacido NO se hace aca: mods de animacion (NotEnoughAnimations,
 * player-animation-lib, ParCool) tocan los ModelPart del jugador durante el render y pisaban la
 * escala si se aplicaba en Pre (quedaba resuelta ANTES del pase base, dejando tiempo para que
 * otro mod la reseteara). Por eso esa parte se hace toda de una vez dentro de RaceFeaturesLayer:
 * se oculta la cabeza/brazo real en Pre (para que el pase base no los dibuje) y se vuelve a
 * mostrar en Post; el reemplazo mas grande (con la piel real del jugador) se dibuja en el layer,
 * en el mismo metodo que fija y resetea la escala, sin dejar hueco para que otro mod interfiera.
 */
public final class RaceRenderEvents {
    // No uniforme a proposito: "mas alto, mas alargado que ancho" (no solo "mas grande en general").
    private static final float ENDER_WARRIOR_HEIGHT_SCALE = 1.32F;
    private static final float ENDER_WARRIOR_WIDTH_SCALE = 1.05F;

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
            model.head.visible = false;
            model.rightArm.visible = false;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Race race = ClientRaceData.get(player.getUUID());
        if (race == Race.ENDER_WARRIOR) {
            event.getPoseStack().popPose();
        } else if (race == Race.MALNACIDO && !ClientRaceData.isPurified(player.getUUID())) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.head.visible = true;
            model.rightArm.visible = true;
        }
    }
}
