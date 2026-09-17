package com.tcorigenes.tcorigenes.favor;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

public class PlayerFavor {
    public interface IPlayerFavor {
        int getFavor(Deity deity);

        void setFavor(Deity deity, int value);

        Map<Deity, Integer> getAll();

        void saveNBTData(CompoundTag nbt);

        void loadNBTData(CompoundTag nbt);
    }

    public static class Implementation implements IPlayerFavor {
        private final Map<Deity, Integer> favor = new EnumMap<>(Deity.class);

        @Override
        public int getFavor(Deity deity) {
            return favor.getOrDefault(deity, 0);
        }

        @Override
        public void setFavor(Deity deity, int value) {
            favor.put(deity, value);
        }

        @Override
        public Map<Deity, Integer> getAll() {
            return favor;
        }

        @Override
        public void saveNBTData(CompoundTag nbt) {
            CompoundTag favorTag = new CompoundTag();
            favor.forEach((deity, value) -> favorTag.putInt(deity.name(), value));
            nbt.put("favor", favorTag);
        }

        @Override
        public void loadNBTData(CompoundTag nbt) {
            favor.clear();
            if (!nbt.contains("favor")) {
                return;
            }
            CompoundTag favorTag = nbt.getCompound("favor");
            for (Deity deity : Deity.values()) {
                if (favorTag.contains(deity.name())) {
                    favor.put(deity, favorTag.getInt(deity.name()));
                }
            }
        }
    }
}
