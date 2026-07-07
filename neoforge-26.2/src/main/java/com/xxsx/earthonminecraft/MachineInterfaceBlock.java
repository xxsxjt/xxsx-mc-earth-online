package com.xxsx.earthonminecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MachineInterfaceBlock extends SupportPartBlock implements EntityBlock {
    private final InterfaceType interfaceType;

    public MachineInterfaceBlock(Properties properties, InterfaceType interfaceType) {
        super(properties);
        this.interfaceType = interfaceType;
    }

    public InterfaceType interfaceType() {
        return interfaceType;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineInterfaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != EarthOnMinecraft.MACHINE_INTERFACE_BLOCK_ENTITY.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                MachineInterfaceBlockEntity.serverTick(tickerLevel, pos, tickerState, (MachineInterfaceBlockEntity) blockEntity);
    }

    public enum InterfaceType {
        INPUT,
        OUTPUT
    }
}
