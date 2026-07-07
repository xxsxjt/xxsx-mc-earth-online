package com.xxsx.earthonminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xxsx.earthonminecraft.ConveyorBeltBlock;
import com.xxsx.earthonminecraft.ConveyorBeltBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ConveyorBeltRenderer implements BlockEntityRenderer<ConveyorBeltBlockEntity, ConveyorBeltRenderState> {
    private final ItemModelResolver itemModelResolver;

    public ConveyorBeltRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public ConveyorBeltRenderState createRenderState() {
        return new ConveyorBeltRenderState();
    }

    @Override
    public void extractRenderState(ConveyorBeltBlockEntity blockEntity, ConveyorBeltRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().hasProperty(ConveyorBeltBlock.FACING)
                ? blockEntity.getBlockState().getValue(ConveyorBeltBlock.FACING)
                : Direction.NORTH;
        state.progress = blockEntity.transportProgress(partialTicks);
        this.itemModelResolver.updateForTopItem(state.itemState, blockEntity.cargo(), ItemDisplayContext.GROUND,
                blockEntity.getLevel(), null, 0);
    }

    @Override
    public void submit(ConveyorBeltRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.itemState.isEmpty()) {
            return;
        }
        float travel = Mth.clamp(state.progress, 0.0F, 1.0F) - 0.5F;
        float x = 0.5F + state.facing.getStepX() * travel;
        float z = 0.5F + state.facing.getStepZ() * travel;
        poseStack.pushPose();
        poseStack.translate(x, 0.22F, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        state.itemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ConveyorBeltBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 0.75D, pos.getZ() + 1.0D);
    }
}
