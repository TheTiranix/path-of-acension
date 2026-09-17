package com.tcorigenes.tcorigenes.core.capability;

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

public class PlayerRaceProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<PlayerRace.IPlayerRace> PLAYER_RACE_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<PlayerRace.IPlayerRace>() {});

    private final PlayerRace.IPlayerRace race = new PlayerRace.Implementation();
    private final LazyOptional<PlayerRace.IPlayerRace> optional = LazyOptional.of(() -> this.race);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_RACE_CAPABILITY ? this.optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.race.saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.race.loadNBTData(nbt);
    }
}
