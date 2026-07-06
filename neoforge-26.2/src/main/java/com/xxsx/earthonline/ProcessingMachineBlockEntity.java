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

import java.util.List;
import java.util.Optional;

public class ProcessingMachineBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT_START = 2;
    public static final int OUTPUT_SLOT_COUNT = 7;
    public static final int SLOT_COUNT = SLOT_OUTPUT_START + OUTPUT_SLOT_COUNT;
    public static final int DATA_COUNT = 8;
    private static final int PROCESS_TIME = 60;
    private static final int ENERGY_PER_TICK = 40;
    private static final String HAS_FUEL_SLOT_KEY = "HasFuelSlotV1";

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PROCESS_TIME;
                case 2 -> redstoneMode.id;
                case 3 -> active ? 1 : 0;
                case 4 -> structureValid ? 1 : 0;
                case 5 -> burnTime;
                case 6 -> Math.max(1, burnTimeTotal);
                case 7 -> gridPowered ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = Math.max(0, value);
                case 2 -> redstoneMode = RedstoneMode.byId(value);
                case 3 -> active = value != 0;
                case 5 -> burnTime = Math.max(0, value);
                case 6 -> burnTimeTotal = Math.max(0, value);
                case 7 -> gridPowered = value != 0;
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
    private int progress;
    private int burnTime;
    private int burnTimeTotal;
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS;
    private boolean active;
    private boolean gridPowered;
    private boolean structureValid = true;
    private boolean assemblySynced;
    private boolean lastAssemblyComplete;

    public ProcessingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(EarthOnline.PROCESSING_MACHINE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProcessingMachineBlockEntity machine) {
        boolean changed = false;
        if (machine.active || machine.gridPowered) {
            changed = true;
        }
        machine.active = false;
        machine.gridPowered = false;

        boolean completeStructure = MachineMultiblock.isComplete(level, pos, machine.kind());
        if (!machine.assemblySynced || machine.lastAssemblyComplete != completeStructure) {
            MachineMultiblock.syncAssembly(level, pos, machine.kind(), completeStructure);
            machine.assemblySynced = true;
            machine.lastAssemblyComplete = completeStructure;
        }
        if (machine.structureValid != completeStructure) {
            machine.structureValid = completeStructure;
            changed = true;
        }
        if (!completeStructure) {
            if (machine.progress != 0) {
                machine.progress = 0;
                changed = true;
            }
            machine.setChangedIfNeeded(changed);
            return;
        }

        if (!machine.redstoneMode.allows(level.hasNeighborSignal(pos))) {
            machine.setChangedIfNeeded(changed);
            return;
        }

        ItemStack input = machine.items.get(SLOT_INPUT);
        Optional<ProcessingMachineBlock.Recipe> match = ProcessingMachineBlock.findRecipe(machine.kind(), input);
        if (match.isEmpty()) {
            if (machine.progress != 0) {
                machine.progress = 0;
                changed = true;
            }
            machine.setChangedIfNeeded(changed);
            return;
        }

        ProcessingMachineBlock.Recipe recipe = match.get();
        if (!machine.canFitOutputs(recipe.outputStacks())) {
            machine.setChangedIfNeeded(changed);
            return;
        }

        if (EnergyNetwork.consume(level, pos, ENERGY_PER_TICK)) {
            machine.gridPowered = true;
        } else {
            if (machine.burnTime <= 0 && machine.tryConsumeFuel()) {
                changed = true;
            }
            if (machine.burnTime <= 0) {
                machine.burnTimeTotal = 0;
                machine.setChangedIfNeeded(changed);
                return;
            }
            machine.burnTime--;
        }

        machine.active = true;
        machine.progress++;
        changed = true;

        if (machine.progress >= PROCESS_TIME) {
            input.shrink(1);
            if (input.isEmpty()) {
                machine.items.set(SLOT_INPUT, ItemStack.EMPTY);
            }
            machine.insertOutputs(recipe.outputStacks());
            machine.progress = 0;
        }

        machine.setChangedIfNeeded(changed);
    }

    public ProcessingMachineBlock.Kind kind() {
        if (getBlockState().getBlock() instanceof ProcessingMachineBlock block) {
            return block.kind();
        }
        return ProcessingMachineBlock.Kind.CRUSHER;
    }

    public ContainerData data() {
        return data;
    }

    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    public boolean structureValid() {
        return structureValid;
    }

    public boolean gridPowered() {
        return gridPowered;
    }

    public static int energyPerTick() {
        return ENERGY_PER_TICK;
    }

    public void cycleRedstoneMode() {
        this.redstoneMode = this.redstoneMode.next();
        setChanged();
    }

    public void setRedstoneMode(RedstoneMode mode) {
        if (this.redstoneMode != mode) {
            this.redstoneMode = mode;
            setChanged();
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            return ProcessingMachineBlock.findRecipe(kind(), stack).isPresent();
        }
        return slot == SLOT_FUEL && getFuelTicks(stack) > 0;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(kind().displayNameKey());
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
        return new ProcessingMachineMenu(containerId, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        boolean hasFuelSlot = input.getBooleanOr(HAS_FUEL_SLOT_KEY, false);
        this.items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        if (!hasFuelSlot) {
            migrateLegacyOutputSlots();
        }
        this.progress = input.getIntOr("Progress", 0);
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.burnTimeTotal = input.getIntOr("BurnTimeTotal", burnTime);
        this.redstoneMode = RedstoneMode.byId(input.getIntOr("RedstoneMode", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putBoolean(HAS_FUEL_SLOT_KEY, true);
        output.putInt("Progress", this.progress);
        output.putInt("BurnTime", this.burnTime);
        output.putInt("BurnTimeTotal", this.burnTimeTotal);
        output.putInt("RedstoneMode", this.redstoneMode.id);
    }

    private void migrateLegacyOutputSlots() {
        for (int i = SLOT_COUNT - 1; i >= SLOT_OUTPUT_START; i--) {
            this.items.set(i, this.items.get(i - 1));
        }
        this.items.set(SLOT_FUEL, ItemStack.EMPTY);
    }

    public static int getFuelTicks(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Item item = stack.getItem();
        if (item == EarthOnline.COAL_DUST.get().asItem()) {
            return 1200;
        }
        if (item == EarthOnline.WOOD_CHIPS.get().asItem()) {
            return 300;
        }
        if (item == EarthOnline.COKE.get().asItem()) {
            return 2400;
        }
        if (item == EarthOnline.PETROLEUM_COKE.get().asItem()) {
            return 3200;
        }
        if (item == EarthOnline.COAL_GAS_CELL.get().asItem() || item == EarthOnline.NATURAL_GAS_CELL.get().asItem()) {
            return 1800;
        }
        if (item == Items.COAL || item == Items.CHARCOAL) {
            return 1600;
        }
        if (item == Items.COAL_BLOCK) {
            return 16000;
        }
        if (item == Items.DRIED_KELP_BLOCK) {
            return 4000;
        }
        if (item == Items.BLAZE_ROD) {
            return 2400;
        }
        if (item == Items.LAVA_BUCKET) {
            return 20000;
        }
        return 0;
    }

    private boolean tryConsumeFuel() {
        ItemStack fuel = this.items.get(SLOT_FUEL);
        int ticks = getFuelTicks(fuel);
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

    private boolean canFitOutputs(List<ItemStack> outputs) {
        NonNullList<ItemStack> simulated = NonNullList.withSize(OUTPUT_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUT_SLOT_COUNT; i++) {
            simulated.set(i, this.items.get(SLOT_OUTPUT_START + i).copy());
        }

        for (ItemStack output : outputs) {
            if (!fitInto(simulated, output.copy())) {
                return false;
            }
        }
        return true;
    }

    private void insertOutputs(List<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            fitInto(this.items, output.copy(), SLOT_OUTPUT_START, SLOT_COUNT);
        }
    }

    private static boolean fitInto(NonNullList<ItemStack> stacks, ItemStack output) {
        return fitInto(stacks, output, 0, stacks.size());
    }

    private static boolean fitInto(NonNullList<ItemStack> stacks, ItemStack output, int start, int end) {
        for (int i = start; i < end && !output.isEmpty(); i++) {
            ItemStack existing = stacks.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, output)) {
                int move = Math.min(output.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (move > 0) {
                    existing.grow(move);
                    output.shrink(move);
                }
            }
        }
        for (int i = start; i < end && !output.isEmpty(); i++) {
            if (stacks.get(i).isEmpty()) {
                int move = Math.min(output.getCount(), output.getMaxStackSize());
                stacks.set(i, output.copyWithCount(move));
                output.shrink(move);
            }
        }
        return output.isEmpty();
    }

    private void setChangedIfNeeded(boolean changed) {
        if (changed) {
            setChanged();
        }
    }

    public enum RedstoneMode {
        ALWAYS(0, "screen.earth_online.redstone.always", "screen.earth_online.redstone.always.desc"),
        REQUIRE_SIGNAL(1, "screen.earth_online.redstone.require_signal", "screen.earth_online.redstone.require_signal.desc"),
        REQUIRE_NO_SIGNAL(2, "screen.earth_online.redstone.require_no_signal", "screen.earth_online.redstone.require_no_signal.desc");

        private final int id;
        private final String labelKey;
        private final String descriptionKey;

        RedstoneMode(int id, String labelKey, String descriptionKey) {
            this.id = id;
            this.labelKey = labelKey;
            this.descriptionKey = descriptionKey;
        }

        public static RedstoneMode byId(int id) {
            for (RedstoneMode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return ALWAYS;
        }

        public RedstoneMode next() {
            return byId((id + 1) % values().length);
        }

        public boolean allows(boolean powered) {
            return switch (this) {
                case ALWAYS -> true;
                case REQUIRE_SIGNAL -> powered;
                case REQUIRE_NO_SIGNAL -> !powered;
            };
        }

        public String labelKey() {
            return labelKey;
        }

        public String descriptionKey() {
            return descriptionKey;
        }
    }
}
