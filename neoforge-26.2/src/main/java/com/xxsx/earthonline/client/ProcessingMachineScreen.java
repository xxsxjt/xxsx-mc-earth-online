package com.xxsx.earthonline.client;

import com.xxsx.earthonline.ProcessingMachineBlock;
import com.xxsx.earthonline.ProcessingMachineBlockEntity;
import com.xxsx.earthonline.ProcessingMachineMenu;
import com.xxsx.earthonline.MachineMultiblock;
import com.xxsx.earthonline.RouteGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ProcessingMachineScreen extends AbstractContainerScreen<ProcessingMachineMenu> {
    private static final int VANILLA_PANEL = 0xFFC6C6C6;
    private static final int VANILLA_LIGHT = 0xFFFFFFFF;
    private static final int VANILLA_DARK = 0xFF555555;
    private static final int VANILLA_SLOT = 0xFF8B8B8B;
    private static final int VANILLA_TEXT = 0xFF404040;
    private static final int MUTED = 0xFF606060;
    private static final int WARNING = 0xFFAA3322;
    private static final int PROGRESS_BG = 0xFF8B8B8B;
    private static final int PROGRESS = 0xFFFFD36A;

    private Button redstoneButton;

    public ProcessingMachineScreen(ProcessingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        redstoneButton = addRenderableWidget(Button.builder(redstoneButtonLabel(), b -> cycleRedstoneMode())
                .bounds(this.leftPos + 8, this.topPos + 55, 64, 17)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.earth_online.button.notebook"), button -> {
                    this.onClose();
                    EarthOnlineClient.openNotebook();
                })
                .bounds(this.leftPos + this.imageWidth - 42, this.topPos + 4, 34, 14)
                .build());
        syncRedstoneButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncRedstoneButtons();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        drawVanillaPanel(g);
        drawSlot(g, 38, 35);
        drawOutputSlots(g);
        drawProgress(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, trimmedTitle(), this.titleLabelX, this.titleLabelY, VANILLA_TEXT, false);
        g.text(this.font, Component.translatable("screen.earth_online.machine.input"), 31, 24, MUTED, false);
        g.text(this.font, Component.translatable("screen.earth_online.machine.outputs"), 112, 11, MUTED, false);
        g.text(this.font, Component.translatable("screen.earth_online.machine.redstone"), 8, 45, MUTED, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, VANILLA_TEXT, false);
        g.text(this.font, statusLine(), 78, 59, statusColor(), false);
    }

    private void drawOutputSlots(GuiGraphicsExtractor g) {
        int[][] outputSlots = {
                {102, 20}, {120, 20}, {138, 20}, {156, 20},
                {111, 42}, {129, 42}, {147, 42}
        };
        for (int[] slot : outputSlots) {
            drawSlot(g, slot[0], slot[1]);
        }
    }

    private void drawVanillaPanel(GuiGraphicsExtractor g) {
        int x = this.leftPos;
        int y = this.topPos;
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, VANILLA_PANEL);
        g.fill(x, y, x + this.imageWidth, y + 1, VANILLA_LIGHT);
        g.fill(x, y, x + 1, y + this.imageHeight, VANILLA_LIGHT);
        g.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, VANILLA_DARK);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, VANILLA_DARK);
        g.fill(x + 7, y + 82, x + 169, y + 83, VANILLA_DARK);
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y) {
        int sx = this.leftPos + x - 1;
        int sy = this.topPos + y - 1;
        g.fill(sx, sy, sx + 18, sy + 18, VANILLA_DARK);
        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, VANILLA_SLOT);
        g.fill(sx + 1, sy + 1, sx + 16, sy + 2, 0xFF373737);
        g.fill(sx + 1, sy + 1, sx + 2, sy + 16, 0xFF373737);
        g.fill(sx + 2, sy + 2, sx + 17, sy + 17, 0xFFE0E0E0);
        g.fill(sx + 2, sy + 2, sx + 16, sy + 16, VANILLA_SLOT);
    }

    private void drawProgress(GuiGraphicsExtractor g) {
        int x = this.leftPos + 64;
        int y = this.topPos + 38;
        drawArrow(g, x, y, PROGRESS_BG);
        int progress = Math.min(24, 24 * this.menu.progress() / this.menu.maxProgress());
        if (progress > 0) {
            g.fill(x, y + 5, x + progress, y + 12, this.menu.active() ? PROGRESS : 0xFFFFB347);
        }
    }

    private void drawArrow(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x, y + 5, x + 18, y + 12, color);
        g.fill(x + 18, y + 2, x + 21, y + 15, color);
        g.fill(x + 21, y + 5, x + 24, y + 12, color);
    }

    private String statusLine() {
        ProcessingMachineBlockEntity.RedstoneMode redstone = this.menu.redstoneMode();
        if (!this.menu.structureValid()) {
            MachineMultiblock.Pattern pattern = MachineMultiblock.patternFor(this.menu.kind());
            return fit(localized("screen.earth_online.machine.structure_missing") + " " + localized(pattern.screenKey()), 90);
        }

        ItemStack input = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (input.isEmpty()) {
            return fit(localized("screen.earth_online.machine.empty_input"), 90);
        }

        return ProcessingMachineBlock.findRecipe(this.menu.kind(), input).map(recipe -> {
            String note = recipeSummary(recipe);
            return fit(note, 90);
        }).orElseGet(() -> fit(localized("screen.earth_online.machine.unsupported_input"), 90));
    }

    private int statusColor() {
        if (!this.menu.structureValid()) {
            return WARNING;
        }
        ItemStack input = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (!input.isEmpty() && ProcessingMachineBlock.findRecipe(this.menu.kind(), input).isEmpty()) {
            return WARNING;
        }
        return this.menu.active() ? 0xFF207030 : MUTED;
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(8, 55, 64, 17, mouseX, mouseY)) {
            ProcessingMachineBlockEntity.RedstoneMode redstone = this.menu.redstoneMode();
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable(redstone.labelKey()),
                    Component.translatable(redstone.descriptionKey()),
                    Component.translatable("screen.earth_online.redstone.tooltip")
            ), mouseX, mouseY);
        }
        if (!this.menu.structureValid() && isHovering(78, 55, 90, 17, mouseX, mouseY)) {
            MachineMultiblock.Pattern pattern = MachineMultiblock.patternFor(this.menu.kind());
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_online.machine.structure_missing"),
                    Component.translatable(pattern.screenKey())
            ), mouseX, mouseY);
        }
    }

    private void cycleRedstoneMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) {
            return;
        }
        int id = switch (this.menu.redstoneMode()) {
            case ALWAYS -> ProcessingMachineMenu.BUTTON_REDSTONE_REQUIRE_SIGNAL;
            case REQUIRE_SIGNAL -> ProcessingMachineMenu.BUTTON_REDSTONE_REQUIRE_NO_SIGNAL;
            case REQUIRE_NO_SIGNAL -> ProcessingMachineMenu.BUTTON_REDSTONE_ALWAYS;
        };
        mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private void syncRedstoneButtons() {
        if (redstoneButton != null) {
            redstoneButton.setMessage(redstoneButtonLabel());
        }
    }

    private Component redstoneButtonLabel() {
        return Component.translatable(this.menu.redstoneMode().labelKey());
    }

    private Component trimmedTitle() {
        String text = this.title.getString();
        if (this.font.width(text) > 118) {
            return Component.literal(this.font.plainSubstrByWidth(text, 115) + "...");
        }
        return this.title;
    }

    private String recipeSummary(ProcessingMachineBlock.Recipe recipe) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(java.util.Locale.ROOT).startsWith("zh")) {
            return recipe.note();
        }
        return localized("screen.earth_online.machine.recipe_ready") + ": " + RouteGuide.describeOutputs(recipe);
    }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("..."))) + "...";
    }

    private static String localized(String key) {
        return Language.getInstance().getOrDefault(key);
    }
}
