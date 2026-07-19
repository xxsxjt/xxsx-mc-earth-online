package com.xxsx.earthonminecraft.api.v1;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface LogisticsApi {
    Optional<ItemPort> findPort(Level level, BlockPos pos, Direction side);

    interface ItemPort {
        ItemStack insert(ItemStack stack, boolean simulate);

        ItemStack extract(int maxAmount, boolean simulate);

        int slotCount();
    }
}
