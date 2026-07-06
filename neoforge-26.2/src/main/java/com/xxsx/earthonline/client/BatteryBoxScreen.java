package com.xxsx.earthonline.client;

import com.xxsx.earthonline.BatteryBoxMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BatteryBoxScreen extends AbstractContainerScreen<BatteryBoxMenu> {
    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("earth_online", "textures/gui/container/processing_machine.png");
    private static final int ENERGY_X = 28;
    private static final int ENERGY_Y = 31;
    private static final int ENERGY_W = 120;
    private static final int ENERGY_H = 12;
    private static final int VANILLA_TEXT = 0xFF404040;
    private static final int MUTED = 0xFF606060;
    private static final int POWER = 0xFF1E7C9A;

    public BatteryBoxScreen(BatteryBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_online.button.notebook"), button -> {
                    this.onClose();
                    EarthOnlineClient.openNotebook();
                })
                .bounds(this.leftPos + this.imageWidth - 42, this.topPos + 4, 34, 14)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        g.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        drawEnergy(g, this.leftPos + ENERGY_X, this.topPos + ENERGY_Y, ENERGY_W, ENERGY_H, this.menu.energy(), this.menu.capacity());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_online.energy.stored", this.menu.energy(), this.menu.capacity()),
                    Component.translatable("screen.earth_online.energy.battery_tooltip", this.menu.transferLimit())
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, VANILLA_TEXT, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, VANILLA_TEXT, false);
        g.text(this.font, Component.literal(compactEnergy(this.menu.energy(), this.menu.capacity())), ENERGY_X, 19, VANILLA_TEXT, false);
        g.text(this.font, Component.translatable("screen.earth_online.energy.battery_hint_short"), ENERGY_X, 52, MUTED, false);
        g.text(this.font, Component.translatable("screen.earth_online.energy.battery_transfer_short", this.menu.transferLimit()), ENERGY_X, 64, POWER, false);
    }

    private void drawEnergy(GuiGraphicsExtractor g, int x, int y, int width, int height, int energy, int capacity) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF3B3B3B);
        g.fill(x, y, x + width, y + height, 0xFF10151A);
        int filled = Math.min(width, width * Math.max(0, energy) / Math.max(1, capacity));
        if (filled > 0) {
            g.fill(x, y, x + filled, y + height, 0xFF31A9C9);
            g.fill(x, y, x + filled, y + 2, 0xFF82DFF0);
        }
    }

    private static String compactEnergy(int energy, int capacity) {
        return compact(energy) + "/" + compact(capacity) + " EOU";
    }

    private static String compact(int value) {
        if (value >= 1000) {
            return value / 1000 + "k";
        }
        return Integer.toString(value);
    }
}
