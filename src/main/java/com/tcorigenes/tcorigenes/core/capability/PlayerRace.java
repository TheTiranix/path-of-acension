package com.tcorigenes.tcorigenes.core.capability;

import com.tcorigenes.tcorigenes.core.Race;
import net.minecraft.nbt.CompoundTag;

public class PlayerRace {
    public interface IPlayerRace {
        Race getRace();

        void setRace(Race race);

        void saveNBTData(CompoundTag nbt);

        void loadNBTData(CompoundTag nbt);
    }

    public static class Implementation implements IPlayerRace {
        private Race playerRace = Race.HUMANO;

        @Override
        public Race getRace() {
            return this.playerRace;
        }

        @Override
        public void setRace(Race race) {
            this.playerRace = race;
        }

        @Override
        public void saveNBTData(CompoundTag nbt) {
            nbt.putString("race", this.playerRace.name());
        }

        @Override
        public void loadNBTData(CompoundTag nbt) {
            if (nbt.contains("race")) {
                try {
                    this.playerRace = Race.valueOf(nbt.getString("race"));
                } catch (IllegalArgumentException e) {
                    this.playerRace = Race.HUMANO;
                }
            }
        }
    }
}
