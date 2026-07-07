package com.xxsx.earthonminecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ControlPanelBlock extends SupportPartBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH_SHAPE = Block.box(1.0, 1.0, 14.0, 15.0, 15.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(1.0, 1.0, 0.0, 15.0, 15.0, 2.0);
    private static final VoxelShape WEST_SHAPE = Block.box(14.0, 1.0, 1.0, 16.0, 15.0, 15.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 1.0, 1.0, 2.0, 15.0, 15.0);

    public ControlPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        for (Direction front : Direction.Plane.HORIZONTAL) {
            if (context.getLevel().getBlockEntity(pos.relative(front.getOpposite())) instanceof ProcessingMachineBlockEntity) {
                return defaultBlockState().setValue(FACING, front);
            }
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        MachineMultiblock.refreshProjectionForPanel(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        MachineMultiblock.refreshProjectionForPanel(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return openConnectedMachine(level, pos, player, true);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        return openConnectedMachine(level, pos, player, true);
    }

    @Override
    protected InteractionResult openConnectedMachine(Level level, BlockPos pos, Player player, boolean showMissingMessage) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return MachineMultiblock.findControllerForPart(level, pos)
                .map(controller -> ProcessingMachineBlock.openMachineAt(level, controller, player))
                .orElseGet(() -> {
                    player.sendOverlayMessage(Component.translatable("message.earth_on_minecraft.control_panel.not_connected"));
                    return InteractionResult.SUCCESS_SERVER;
                });
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }
}
