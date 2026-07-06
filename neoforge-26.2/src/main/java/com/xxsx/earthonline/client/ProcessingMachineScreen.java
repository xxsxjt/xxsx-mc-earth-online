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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ProcessingMachineScreen extends AbstractContainerScreen<ProcessingMachineMenu> {
    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("earth_online", "textures/gui/container/processing_machine.png");
    private static final Identifier PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier FUEL_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int VANILLA_TEXT = 0xFF404040;
    private static final int MUTED = 0xFF606060;
    private static final int WARNING = 0xFFAA3322;

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
        this.titleLabelX = Math.max(8, (this.imageWidth - this.font.width(trimmedTitle())) / 2);
        redstoneButton = addRenderableWidget(Button.builder(redstoneButtonLabel(), b -> cycleRedstoneMode())
                .bounds(this.leftPos + 8, this.topPos + 55, 20, 20)
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
        g.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        drawFuel(g);
        drawProgress(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawRedstoneIcon(g);
        drawTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, trimmedTitle(), this.titleLabelX, this.titleLabelY, VANILLA_TEXT, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, VANILLA_TEXT, false);
        g.text(this.font, statusLine(), 76, 60, statusColor(), false);
    }

    private void drawFuel(GuiGraphicsExtractor g) {
        if (this.menu.burnTimeTotal() <= 0) {
            return;
        }
        int lit = Math.min(14, 14 * this.menu.burnTime() / this.menu.burnTimeTotal());
        if (lit > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_SPRITE, 14, 14, 0, 14 - lit,
                    this.leftPos + 59, this.topPos + 59 + 14 - lit, 14, lit);
        }
    }

    private void drawProgress(GuiGraphicsExtractor g) {
        int progress = Math.min(24, 24 * this.menu.progress() / this.menu.maxProgress());
        if (progress > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_SPRITE, 24, 16, 0, 0, this.leftPos + 64, this.topPos + 38, progress, 16);
        }
    }

    private String statusLine() {
        if (!this.menu.structureValid()) {
            MachineMultiblock.Pattern pattern = MachineMultiblock.patternFor(this.menu.kind());
            return fit(localized("screen.earth_online.machine.structure_missing") + " " + localized(pattern.screenKey()), 132);
        }

        ItemStack input = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (input.isEmpty()) {
            return fit(localized("screen.earth_online.machine.empty_input"), 92);
        }

        return ProcessingMachineBlock.findRecipe(this.menu.kind(), input).map(recipe -> {
            if (this.menu.gridPowered()) {
                return fit(localized("screen.earth_online.machine.grid_powered") + " " + recipeSummary(recipe), 92);
            }
            if (!this.menu.hasBurningFuel() && !fuelSlotHasFuel()) {
                return fit(localized("screen.earth_online.machine.missing_power"), 92);
            }
            String note = recipeSummary(recipe);
            return fit(note, 92);
        }).orElseGet(() -> fit(localized("screen.earth_online.machine.unsupported_input"), 92));
    }

    private int statusColor() {
        if (!this.menu.structureValid()) {
            return WARNING;
        }
        ItemStack input = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (!input.isEmpty() && ProcessingMachineBlock.findRecipe(this.menu.kind(), input).isEmpty()) {
            return WARNING;
        }
        if (!input.isEmpty() && !this.menu.gridPowered() && !this.menu.hasBurningFuel() && !fuelSlotHasFuel()) {
            return WARNING;
        }
        return this.menu.active() ? 0xFF207030 : MUTED;
    }

    private void drawRedstoneIcon(GuiGraphicsExtractor g) {
        int x = this.leftPos + 10;
        int y = this.topPos + 57;
        ProcessingMachineBlockEntity.RedstoneMode redstone = this.menu.redstoneMode();
        g.item(new ItemStack(redstoneIcon(redstone)), x, y);
        if (redstone == ProcessingMachineBlockEntity.RedstoneMode.REQUIRE_NO_SIGNAL) {
            g.fill(x, y, x + 16, y + 16, 0x99000000);
        }
    }

    private Item redstoneIcon(ProcessingMachineBlockEntity.RedstoneMode redstone) {
        return switch (redstone) {
            case ALWAYS -> Items.BARRIER;
            case REQUIRE_SIGNAL, REQUIRE_NO_SIGNAL -> Items.REDSTONE_TORCH;
        };
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(8, 55, 20, 20, mouseX, mouseY)) {
            ProcessingMachineBlockEntity.RedstoneMode redstone = this.menu.redstoneMode();
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_online.redstone.current", Component.translatable(redstone.labelKey())),
                    Component.translatable(redstone.descriptionKey()),
                    Component.translatable("screen.earth_online.redstone.tooltip")
            ), mouseX, mouseY);
        }
        if (!this.menu.structureValid() && isHovering(34, 55, 134, 20, mouseX, mouseY)) {
            MachineMultiblock.Pattern pattern = MachineMultiblock.patternFor(this.menu.kind());
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_online.machine.structure_missing"),
                    Component.translatable(pattern.screenKey())
            ), mouseX, mouseY);
        }
        if (isHovering(38, 57, 18, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_online.machine.fuel"),
                    Component.translatable("screen.earth_online.machine.fuel.tooltip"),
                    Component.translatable("screen.earth_online.machine.energy.tooltip", this.menu.energyPerTick()),
                    Component.translatable("screen.earth_online.machine.fuel.examples")
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
        return Component.empty();
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

    private boolean fuelSlotHasFuel() {
        ItemStack fuel = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_FUEL).getItem();
        return ProcessingMachineBlockEntity.getFuelTicks(fuel) > 0;
    }
}
