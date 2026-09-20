// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.playerclass.capability;

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

public class PlayerClassProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<PlayerClassData.IPlayerClassData> PLAYER_CLASS_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<PlayerClassData.IPlayerClassData>() {});

    private final PlayerClassData.IPlayerClassData data = new PlayerClassData.Implementation();
    private final LazyOptional<PlayerClassData.IPlayerClassData> optional = LazyOptional.of(() -> this.data);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_CLASS_CAPABILITY ? this.optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.data.saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.data.loadNBTData(nbt);
    }
}
