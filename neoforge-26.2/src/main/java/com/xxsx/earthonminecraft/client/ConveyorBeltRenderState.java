package com.xxsx.earthonminecraft.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class ConveyorBeltRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState itemState = new ItemStackRenderState();
    public Direction facing = Direction.NORTH;
    public float progress;
}
