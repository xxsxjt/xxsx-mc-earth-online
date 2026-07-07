package com.xxsx.earthonminecraft;

public interface ElectricEnergyStorage {
    int getEnergyStored();

    int getEnergyCapacity();

    int receiveEnergy(int amount, boolean simulate);

    int extractEnergy(int amount, boolean simulate);
}
