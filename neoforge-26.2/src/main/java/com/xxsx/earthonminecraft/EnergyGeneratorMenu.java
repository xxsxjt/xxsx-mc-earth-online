package com.xxsx.earthonminecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnergyGeneratorMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_START = EnergyGeneratorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public EnergyGeneratorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos());
    }

    private EnergyGeneratorMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(EnergyGeneratorBlockEntity.SLOT_COUNT),
                new SimpleContainerData(EnergyGeneratorBlockEntity.DATA_COUNT), pos);
    }

    public EnergyGeneratorMenu(int containerId, Inventory inventory, EnergyGeneratorBlockEntity generator, ContainerData data) {
        this(containerId, inventory, generator, data, generator.getBlockPos());
    }

    private EnergyGeneratorMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(EarthOnMinecraft.ENERGY_GENERATOR_MENU.get(), containerId);
        checkContainerSize(container, EnergyGeneratorBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, EnergyGeneratorBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.pos = pos;

        this.container.startOpen(inventory.player);
        addSlot(new Slot(container, EnergyGeneratorBlockEntity.SLOT_FUEL, 38, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ProcessingMachineBlockEntity.getFuelTicks(stack) > 0;
            }
        });

        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();

        if (index < EnergyGeneratorBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ProcessingMachineBlockEntity.getFuelTicks(stack) > 0) {
            if (!moveItemStackTo(stack, EnergyGeneratorBlockEntity.SLOT_FUEL, EnergyGeneratorBlockEntity.SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INV_END) {
            if (!moveItemStackTo(stack, PLAYER_INV_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public BlockPos pos() {
        return pos;
    }

    public int energy() {
        return data.get(0);
    }

    public int capacity() {
        return Math.max(1, data.get(1));
    }

    public int burnTime() {
        return data.get(2);
    }

    public int burnTimeTotal() {
        return Math.max(1, data.get(3));
    }

    public boolean active() {
        return data.get(4) != 0;
    }

    public int generationPerTick() {
        return EnergyGeneratorBlockEntity.GENERATION_PER_TICK;
    }

    public int transferPerTick() {
        return EnergyGeneratorBlockEntity.TRANSFER_PER_TICK;
    }
}
