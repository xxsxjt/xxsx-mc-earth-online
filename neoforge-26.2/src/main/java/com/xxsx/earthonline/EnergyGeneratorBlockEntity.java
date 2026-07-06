package com.xxsx.earthonline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EnergyGeneratorBlockEntity extends BaseContainerBlockEntity implements ElectricEnergyStorage {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_COUNT = 1;
    public static final int DATA_COUNT = 5;
    public static final int CAPACITY = 64_000;
    public static final int GENERATION_PER_TICK = 80;
    public static final int TRANSFER_PER_TICK = 320;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy;
                case 1 -> CAPACITY;
                case 2 -> burnTime;
                case 3 -> Math.max(1, burnTimeTotal);
                case 4 -> active ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0, Math.min(CAPACITY, value));
                case 2 -> burnTime = Math.max(0, value);
                case 3 -> burnTimeTotal = Math.max(0, value);
                case 4 -> active = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int energy;
    private int burnTime;
    private int burnTimeTotal;
    private boolean active;

    public EnergyGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(EarthOnline.ENERGY_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyGeneratorBlockEntity generator) {
        boolean changed = false;
        if (generator.active) {
            changed = true;
        }
        generator.active = false;

        if (generator.energy < CAPACITY && generator.burnTime <= 0 && generator.tryConsumeFuel()) {
            changed = true;
        }

        if (generator.energy < CAPACITY && generator.burnTime > 0) {
            generator.burnTime--;
            generator.energy = Math.min(CAPACITY, generator.energy + GENERATION_PER_TICK);
            generator.active = true;
            changed = true;
        }

        if (generator.energy > 0) {
            int moved = EnergyNetwork.distributeToBuffers(level, pos, Math.min(TRANSFER_PER_TICK, generator.energy));
            if (moved > 0) {
                generator.energy -= moved;
                changed = true;
            }
        }

        if (changed) {
            generator.setChanged();
        }
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getEnergyCapacity() {
        return CAPACITY;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        int extracted = Math.min(Math.max(0, amount), energy);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_FUEL && ProcessingMachineBlockEntity.getFuelTicks(stack) > 0;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.earth_online.combustion_generator");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new EnergyGeneratorMenu(containerId, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.energy = input.getIntOr("Energy", 0);
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.burnTimeTotal = input.getIntOr("BurnTimeTotal", burnTime);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("Energy", this.energy);
        output.putInt("BurnTime", this.burnTime);
        output.putInt("BurnTimeTotal", this.burnTimeTotal);
    }

    private boolean tryConsumeFuel() {
        ItemStack fuel = this.items.get(SLOT_FUEL);
        int ticks = ProcessingMachineBlockEntity.getFuelTicks(fuel);
        if (ticks <= 0) {
            return false;
        }
        Item item = fuel.getItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            this.items.set(SLOT_FUEL, item == Items.LAVA_BUCKET ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY);
        }
        this.burnTime = ticks;
        this.burnTimeTotal = ticks;
        return true;
    }
}
