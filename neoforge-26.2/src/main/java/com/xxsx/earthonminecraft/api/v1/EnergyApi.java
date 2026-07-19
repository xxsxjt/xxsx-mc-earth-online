package com.xxsx.earthonminecraft.api.v1;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface EnergyApi {
    Optional<EnergyPort> findPort(Level level, BlockPos pos, Direction side);

    default int transfer(EnergyPort source, EnergyPort target, int amount, boolean simulate) {
        int requested = Math.max(0, amount);
        int available = source.extractEnergy(requested, true);
        int accepted = target.receiveEnergy(available, true);
        if (simulate || accepted <= 0) {
            return accepted;
        }
        int extracted = source.extractEnergy(accepted, false);
        return target.receiveEnergy(extracted, false);
    }

    interface EnergyPort {
        int getEnergyStored();

        int getEnergyCapacity();

        int receiveEnergy(int amount, boolean simulate);

        int extractEnergy(int amount, boolean simulate);

        default EnergySnapshot snapshot() {
            return new EnergySnapshot(getEnergyStored(), getEnergyCapacity());
        }
    }

    record EnergySnapshot(int stored, int capacity) {
        public float fillRatio() {
            return capacity <= 0 ? 0.0F : Math.min(1.0F, stored / (float) capacity);
        }
    }
}
