package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class MachineBlockItem extends BlockItem {
    private final ProcessingMachineBlock.Kind kind;

    public MachineBlockItem(Block block, Properties properties, ProcessingMachineBlock.Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getBlock().getDescriptionId());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        RouteGuide.addBeginnerShortcutTips(stack.getItem(), lines);
        lines.accept(Component.translatable(kind.descriptionKey()).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.earth_online.machine.use").withStyle(ChatFormatting.AQUA));
        lines.accept(Component.translatable("tooltip.earth_online.machine.fuel").withStyle(ChatFormatting.YELLOW));
        lines.accept(Component.translatable(MachineMultiblock.patternFor(kind).descriptionKey()).withStyle(ChatFormatting.GREEN));
        lines.accept(Component.translatable("tooltip.earth_online.machine.redstone").withStyle(ChatFormatting.GREEN));
        lines.accept(Component.translatable("tooltip.earth_online.machine.routes", ProcessingMachineBlock.recipesFor(kind).size())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
