// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability.capability;

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

public class PlayerAbilityLoadoutProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<PlayerAbilityLoadout.IPlayerAbilityLoadout> ABILITY_LOADOUT_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<PlayerAbilityLoadout.IPlayerAbilityLoadout>() {});

    private final PlayerAbilityLoadout.IPlayerAbilityLoadout loadout = new PlayerAbilityLoadout.Implementation();
    private final LazyOptional<PlayerAbilityLoadout.IPlayerAbilityLoadout> optional = LazyOptional.of(() -> this.loadout);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ABILITY_LOADOUT_CAPABILITY ? this.optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.loadout.saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.loadout.loadNBTData(nbt);
    }
}
