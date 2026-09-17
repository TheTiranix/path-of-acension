package com.tudominio.testamentodelacarne;

import com.tudominio.testamentodelacarne.client.gui.PactSelectionScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * "Tiza banada en sangre": al usarse por primera vez abre la GUI de eleccion de Pacto
 * (Sangre vs Acero). Una vez elegido el pacto (tag persistente "pacto_elegido"), el item
 * solo devuelve un mensaje de que el destino ya fue sellado.
 */
public class ReliquiaDelApostataItem extends Item {
    public ReliquiaDelApostataItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pPlayer.getPersistentData().getBoolean("pacto_elegido")) {
            pPlayer.displayClientMessage(Component.literal("Tu destino ya ha sido sellado."), false);
            return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
        }

        if (pLevel.isClientSide()) {
            Minecraft.getInstance().setScreen(new PactSelectionScreen());
        }

        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        pTooltip.add(Component.literal("Un fragmento de poder neutro, esperando un propósito."));
    }
}
