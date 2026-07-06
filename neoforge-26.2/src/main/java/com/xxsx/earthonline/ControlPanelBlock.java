package com.xxsx.earthonline;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ControlPanelBlock extends SupportPartBlock {
    public ControlPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player, BlockHitResult hitResult) {
        return openConnectedMachine(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        return openConnectedMachine(level, pos, player);
    }

    private InteractionResult openConnectedMachine(Level level, net.minecraft.core.BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return MachineMultiblock.findControllerForPanel(level, pos)
                .map(controller -> ProcessingMachineBlock.openMachineAt(level, controller, player))
                .orElseGet(() -> {
                    player.sendOverlayMessage(Component.translatable("message.earth_online.control_panel.not_connected"));
                    return InteractionResult.SUCCESS_SERVER;
                });
    }
}
