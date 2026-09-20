package com.tcorigenes.tcorigenes.core.capability.event;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.core.capability.PlayerRaceProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Hereje: todo encantamiento consume 50% menos de experiencia. En vez de tocar cada mesa/yunque
 * (Apotheosis reemplaza los menus vanilla), se vigila al jugador mientras tiene abierto un menu de
 * encantamiento o de yunque: si sus niveles bajan, se le devuelve la mitad de lo que se descontó
 * (redondeando hacia abajo lo devuelto, o sea paga de mas en costos impares). No aplica en creativo.
 */
@EventBusSubscriber(modid = "tcorigenes")
public final class HerejeEnchantDiscount {
    private static final Map<UUID, Integer> LAST_LEVEL = new HashMap<>();

    private HerejeEnchantDiscount() {
    }

    private static boolean isEnchantingMenu(AbstractContainerMenu menu) {
        if (menu instanceof EnchantmentMenu || menu instanceof AnvilMenu) {
            return true;
        }
        String name = menu.getClass().getName().toLowerCase();
        return name.contains("enchant") || name.contains("anvil");
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        boolean hereje = player.getCapability(PlayerRaceProvider.PLAYER_RACE_CAPABILITY)
                .map(info -> info.getRace() == Race.HEREJE).orElse(false);
        if (!hereje || player.isCreative() || !isEnchantingMenu(player.containerMenu)) {
            LAST_LEVEL.remove(player.getUUID());
            return;
        }
        int current = player.experienceLevel;
        Integer last = LAST_LEVEL.get(player.getUUID());
        if (last != null && current < last) {
            int refund = (last - current) / 2;
            if (refund > 0) {
                player.giveExperienceLevels(refund);
                current = player.experienceLevel;
            }
        }
        LAST_LEVEL.put(player.getUUID(), current);
    }
}
