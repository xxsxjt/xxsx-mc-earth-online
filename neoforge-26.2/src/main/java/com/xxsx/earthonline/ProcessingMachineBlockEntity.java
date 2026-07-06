package com.xxsx.earthonline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Optional;

public class ProcessingMachineBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_START = 1;
    public static final int OUTPUT_SLOT_COUNT = 7;
    public static final int SLOT_COUNT = SLOT_OUTPUT_START + OUTPUT_SLOT_COUNT;
    public static final int DATA_COUNT = 5;
    private static final int PROCESS_TIME = 60;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PROCESS_TIME;
                case 2 -> redstoneMode.id;
                case 3 -> active ? 1 : 0;
                case 4 -> kind().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = Math.max(0, value);
                case 2 -> redstoneMode = RedstoneMode.byId(value);
                case 3 -> active = value != 0;
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
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS;
    private boolean active;

    public ProcessingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(EarthOnline.PROCESSING_MACHINE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProcessingMachineBlockEntity machine) {
        boolean changed = false;
        machine.active = false;

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
        return slot == SLOT_INPUT && ProcessingMachineBlock.findRecipe(kind(), stack).isPresent();
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal(kind().displayName());
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
        this.items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("Progress", 0);
        this.redstoneMode = RedstoneMode.byId(input.getIntOr("RedstoneMode", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("Progress", this.progress);
        output.putInt("RedstoneMode", this.redstoneMode.id);
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
        ALWAYS(0, "无信号持续激活", "不需要红石信号，有材料就持续工作。"),
        REQUIRE_SIGNAL(1, "有红石信号才工作", "只有收到红石信号时工作。"),
        REQUIRE_NO_SIGNAL(2, "无红石信号才工作", "收到红石信号时暂停。");

        private final int id;
        private final String label;
        private final String description;

        RedstoneMode(int id, String label, String description) {
            this.id = id;
            this.label = label;
            this.description = description;
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

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }
    }
}
