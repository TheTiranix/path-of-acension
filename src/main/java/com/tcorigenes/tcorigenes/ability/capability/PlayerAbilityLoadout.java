// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.ability.capability;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public class PlayerAbilityLoadout {
    public interface IPlayerAbilityLoadout {
        /** Puede ser null si todavia no desbloqueo/equipo ninguna habilidad. */
        String getEquippedAbilityId();

        void setEquippedAbilityId(String id);

        Set<String> getUnlockedAbilityIds();

        /** @return true si esta habilidad no estaba desbloqueada todavia. */
        boolean unlockAbility(String id);

        boolean isUnlocked(String id);

        int getSkillPoints();

        void setSkillPoints(int points);

        /** XP del arbol: solo la da un admin con /skillxp; se canjea por puntos de habilidad (ver SkillTreeManager). */
        long getSkillXp();

        void setSkillXp(long xp);

        /** Cuantos puntos compro este jugador con XP (cada uno sale mas caro que el anterior). */
        int getPointsBought();

        void setPointsBought(int bought);

        void saveNBTData(CompoundTag nbt);

        void loadNBTData(CompoundTag nbt);
    }

    public static class Implementation implements IPlayerAbilityLoadout {
        private String equippedAbilityId = null;
        private final Set<String> unlockedAbilityIds = new HashSet<>();
        private int skillPoints = 0;
        private long skillXp = 0;
        private int pointsBought = 0;

        @Override
        public long getSkillXp() {
            return this.skillXp;
        }

        @Override
        public void setSkillXp(long xp) {
            this.skillXp = Math.max(0L, xp);
        }

        @Override
        public int getPointsBought() {
            return this.pointsBought;
        }

        @Override
        public void setPointsBought(int bought) {
            this.pointsBought = Math.max(0, bought);
        }

        @Override
        public int getSkillPoints() {
            return this.skillPoints;
        }

        @Override
        public void setSkillPoints(int points) {
            this.skillPoints = points;
        }

        @Override
        public String getEquippedAbilityId() {
            return this.equippedAbilityId;
        }

        @Override
        public void setEquippedAbilityId(String id) {
            this.equippedAbilityId = id;
        }

        @Override
        public Set<String> getUnlockedAbilityIds() {
            return this.unlockedAbilityIds;
        }

        @Override
        public boolean unlockAbility(String id) {
            boolean wasNew = this.unlockedAbilityIds.add(id);
            if (wasNew && this.equippedAbilityId == null && !id.startsWith("node:")) {
                this.equippedAbilityId = id;
            }
            return wasNew;
        }

        @Override
        public boolean isUnlocked(String id) {
            return this.unlockedAbilityIds.contains(id);
        }

        @Override
        public void saveNBTData(CompoundTag nbt) {
            if (this.equippedAbilityId != null) {
                nbt.putString("equipped_ability", this.equippedAbilityId);
            }
            ListTag list = new ListTag();
            for (String id : this.unlockedAbilityIds) {
                list.add(StringTag.valueOf(id));
            }
            nbt.put("unlocked_abilities", list);
            nbt.putInt("skill_points", this.skillPoints);
            nbt.putLong("skill_xp", this.skillXp);
            nbt.putInt("points_bought", this.pointsBought);
        }

        @Override
        public void loadNBTData(CompoundTag nbt) {
            if (nbt.contains("equipped_ability")) {
                this.equippedAbilityId = nbt.getString("equipped_ability");
            }
            this.unlockedAbilityIds.clear();
            if (nbt.contains("unlocked_abilities")) {
                ListTag list = nbt.getList("unlocked_abilities", StringTag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    this.unlockedAbilityIds.add(list.getString(i));
                }
            }
            if (nbt.contains("skill_points")) {
                this.skillPoints = nbt.getInt("skill_points");
            }
            this.skillXp = nbt.getLong("skill_xp");
            this.pointsBought = nbt.getInt("points_bought");
        }
    }
}
