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
 * visualmente al Ender Warrior (+0.5 bloques, mas alto que ancho) y al Malnacido (cabeza/brazo
 * mas grandes) en Pre/Post. Todo esto es SOLO visual: en 1.20.1 no existe el atributo
 * generic.scale de jugador (llego en 1.20.5+), asi que el hitbox real sigue siendo el tamaño
 * vanilla. Es un compromiso consciente (mismo que se hablo antes de riesgo/beneficio).
 *
 * El Malnacido tiene una vuelta de rosca: PlayerModel tiene capas EXTERIORES separadas del skin
 * (hat para la cabeza, rightSleeve para el brazo, ver PlayerModel#setupAnim) que NO son hijas de
 * head/rightArm sino ModelPart propias. El primer intento (esconder head/rightArm y redibujar
 * aparte) se rompio justamente por esto: la capa exterior seguia dibujandose en su posicion vieja
 * mientras el reemplazo se dibujaba en la nueva, superpuestos. La forma correcta y mucho mas
 * simple es escalar head+hat y rightArm+rightSleeve juntos aca en Pre (ModelPart.render() ya
 * aplica xScale/yScale/zScale solo con setearlos, ver ModelPart#translateAndRotate) y resetear en
 * Post -- sin esconder ni redibujar nada.
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
            setScale(model.hat, MALNACIDO_HEAD_SCALE);
            setScale(model.rightArm, MALNACIDO_ARM_SCALE);
            setScale(model.rightSleeve, MALNACIDO_ARM_SCALE);
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
            setScale(model.head, 1.0F);
            setScale(model.hat, 1.0F);
            setScale(model.rightArm, 1.0F);
            setScale(model.rightSleeve, 1.0F);
        }
    }

    private static void setScale(net.minecraft.client.model.geom.ModelPart part, float scale) {
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }
}
