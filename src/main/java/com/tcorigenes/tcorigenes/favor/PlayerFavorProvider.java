package com.tcorigenes.tcorigenes.favor;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerFavorProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<PlayerFavor.IPlayerFavor> PLAYER_FAVOR_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<PlayerFavor.IPlayerFavor>() {});

    private final PlayerFavor.IPlayerFavor favor = new PlayerFavor.Implementation();
    private final LazyOptional<PlayerFavor.IPlayerFavor> optional = LazyOptional.of(() -> this.favor);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_FAVOR_CAPABILITY ? this.optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.favor.saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.favor.loadNBTData(nbt);
    }
}
