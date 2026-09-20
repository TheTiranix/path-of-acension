// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.playerclass.capability;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import net.minecraft.nbt.CompoundTag;

public class PlayerClassData {
    public interface IPlayerClassData {
        PlayerClass getPlayerClass();

        void setPlayerClass(PlayerClass playerClass);

        void saveNBTData(CompoundTag nbt);

        void loadNBTData(CompoundTag nbt);
    }

    public static class Implementation implements IPlayerClassData {
        private PlayerClass playerClass = PlayerClass.NINGUNA;

        @Override
        public PlayerClass getPlayerClass() {
            return this.playerClass;
        }

        @Override
        public void setPlayerClass(PlayerClass playerClass) {
            this.playerClass = playerClass;
        }

        @Override
        public void saveNBTData(CompoundTag nbt) {
            nbt.putString("player_class", this.playerClass.name());
        }

        @Override
        public void loadNBTData(CompoundTag nbt) {
            if (nbt.contains("player_class")) {
                try {
                    this.playerClass = PlayerClass.valueOf(nbt.getString("player_class"));
                } catch (IllegalArgumentException e) {
                    this.playerClass = PlayerClass.NINGUNA;
                }
            }
        }
    }
}
