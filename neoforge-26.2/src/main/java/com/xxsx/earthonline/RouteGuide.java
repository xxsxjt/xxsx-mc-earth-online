package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class RouteGuide {
    private static final Map<Item, RouteInfo> ROUTES_BY_ITEM = createRoutesByItem();

    private RouteGuide() {
    }

    public static Map<Item, RouteInfo> buildRoutesByItem() {
        return ROUTES_BY_ITEM;
    }

    private static Map<Item, RouteInfo> createRoutesByItem() {
        Map<Item, RouteInfo> routes = new LinkedHashMap<>();
        for (ProcessingMachineBlock.Recipe recipe : ProcessingMachineBlock.recipes()) {
            routes.computeIfAbsent(recipe.input().get().asItem(), ignored -> new RouteInfo()).next.add(recipe);
            for (ProcessingMachineBlock.Output output : recipe.outputs()) {
                routes.computeIfAbsent(output.item().get().asItem(), ignored -> new RouteInfo()).sources.add(recipe);
            }
        }
        return routes;
    }

    public static RouteInfo routeFor(Item item) {
        return ROUTES_BY_ITEM.getOrDefault(item, RouteInfo.EMPTY);
    }

    public static void addRouteTips(ItemStack stack, Consumer<Component> lines) {
        RouteInfo route = routeFor(stack.getItem());

        addBeginnerShortcutTips(stack.getItem(), lines);
        addDirectUseTips(stack.getItem(), lines);

        if (!route.next().isEmpty()) {
            lines.accept(Component.translatable("tooltip.earth_online.route.next", joinMachines(route.next(), 4)).withStyle(ChatFormatting.AQUA));
            lines.accept(Component.translatable("tooltip.earth_online.route.outputs", describeOutputs(route.next().get(0))).withStyle(ChatFormatting.GRAY));
            if (uniqueMachineCount(route.next()) > 1) {
                lines.accept(Component.translatable("tooltip.earth_online.route.multiple").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (!route.sources().isEmpty()) {
            lines.accept(Component.translatable("tooltip.earth_online.route.sources", joinSources(route.sources(), 3)).withStyle(ChatFormatting.DARK_GREEN));
        }

        if (route.isEmpty()) {
            lines.accept(Component.translatable("tooltip.earth_online.route.compat").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void addBeginnerShortcutTips(Item item, Consumer<Component> lines) {
        if (item == EarthOnline.MAGNETITE_CHUNK.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.beginner.first_iron_chunk").withStyle(ChatFormatting.GOLD));
        } else if (item == EarthOnline.MAGNETITE_DUST.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.beginner.first_iron_dust").withStyle(ChatFormatting.GOLD));
        } else if (item == EarthOnline.CHALCOPYRITE_CHUNK.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.beginner.first_copper_chunk").withStyle(ChatFormatting.GOLD));
        } else if (item == EarthOnline.CHALCOPYRITE_DUST.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.beginner.first_copper_dust").withStyle(ChatFormatting.GOLD));
        } else if (item == EarthOnline.JAW_CRUSHER.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.beginner.jaw_crusher").withStyle(ChatFormatting.GOLD));
        } else if (item == EarthOnline.BALL_MILL.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.beginner.ball_mill").withStyle(ChatFormatting.GOLD));
        } else if (item == EarthOnline.BITUMINOUS_COAL_SEAM.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.deposit.bituminous").withStyle(ChatFormatting.YELLOW));
        } else if (item == EarthOnline.ANTHRACITE_COAL_SEAM.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.deposit.anthracite").withStyle(ChatFormatting.YELLOW));
        } else if (item == EarthOnline.COAL_DUST.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.fuel.coal_dust").withStyle(ChatFormatting.YELLOW));
        } else if (item == EarthOnline.COKE.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.fuel.coke").withStyle(ChatFormatting.YELLOW));
        } else if (item == EarthOnline.PETROLEUM_COKE.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.fuel.petroleum_coke").withStyle(ChatFormatting.YELLOW));
        } else if (item == EarthOnline.COAL_GAS_CELL.get().asItem() || item == EarthOnline.NATURAL_GAS_CELL.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.fuel.gas_cell").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void addDirectUseTips(Item item, Consumer<Component> lines) {
        if (item == EarthOnline.STEEL_BLOOM.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.steel_bloom").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.PRINTED_CIRCUIT_BOARD.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.printed_circuit_board").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.SIMPLE_BATTERY_CELL.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.simple_battery_cell").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.VULCANIZED_RUBBER.get().asItem() || item == EarthOnline.RUBBER_GASKET.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.rubber").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.CEMENT_POWDER.get().asItem() || item == EarthOnline.ASPHALT.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.construction").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.FERTILIZER_BLEND.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.fertilizer").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.HUMUS_SAMPLE.get().asItem() || item == EarthOnline.SANDY_LOAM_SAMPLE.get().asItem()
                || item == EarthOnline.ALLUVIAL_LOAM_SAMPLE.get().asItem() || item == EarthOnline.SALINE_SOIL_SAMPLE.get().asItem()
                || item == EarthOnline.SOIL_MINERAL_MIX.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.soil").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.IRRIGATION_MINERAL_DEPOSIT.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.irrigation_deposit").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.CELLULOSE_PULP.get().asItem() || item == EarthOnline.BLEACHED_PULP.get().asItem()
                || item == EarthOnline.CELLULOSE_FIBER.get().asItem() || item == EarthOnline.NYLON_FIBER.get().asItem()
                || item == EarthOnline.MINERAL_WOOL.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.fiber").withStyle(ChatFormatting.GREEN));
        } else if (item == EarthOnline.CARBON_BLACK.get().asItem() || item == EarthOnline.TITANIUM_DIOXIDE.get().asItem()
                || item == EarthOnline.IRON_OXIDE_PIGMENT.get().asItem()) {
            lines.accept(Component.translatable("tooltip.earth_online.use.pigment").withStyle(ChatFormatting.GREEN));
        }
    }

    public static String joinMachines(List<ProcessingMachineBlock.Recipe> recipes, int limit) {
        List<String> names = new ArrayList<>();
        for (ProcessingMachineBlock.Recipe recipe : recipes) {
            String name = recipe.kind().localizedDisplayName();
            if (!names.contains(name)) {
                names.add(name);
            }
            if (names.size() >= limit) {
                break;
            }
        }
        return String.join(" / ", names);
    }

    public static String joinSources(List<ProcessingMachineBlock.Recipe> recipes, int limit) {
        List<String> names = new ArrayList<>();
        for (ProcessingMachineBlock.Recipe recipe : recipes) {
            String input = recipe.inputStack().getItemName().getString();
            String name = recipe.kind().localizedDisplayName() + ": " + input;
            if (!names.contains(name)) {
                names.add(name);
            }
            if (names.size() >= limit) {
                break;
            }
        }
        return String.join("；", names);
    }

    public static String describeOutputs(ProcessingMachineBlock.Recipe recipe) {
        List<String> outputs = new ArrayList<>();
        for (ProcessingMachineBlock.Output output : recipe.outputs()) {
            ItemStack out = output.stack();
            outputs.add(out.getCount() + "x " + out.getItemName().getString());
        }
        return String.join(" + ", outputs);
    }

    public static int uniqueMachineCount(List<ProcessingMachineBlock.Recipe> recipes) {
        List<ProcessingMachineBlock.Kind> kinds = new ArrayList<>();
        for (ProcessingMachineBlock.Recipe recipe : recipes) {
            if (!kinds.contains(recipe.kind())) {
                kinds.add(recipe.kind());
            }
        }
        return kinds.size();
    }

    public static final class RouteInfo {
        private static final RouteInfo EMPTY = new RouteInfo();

        private final List<ProcessingMachineBlock.Recipe> next = new ArrayList<>();
        private final List<ProcessingMachineBlock.Recipe> sources = new ArrayList<>();

        public List<ProcessingMachineBlock.Recipe> next() {
            return next;
        }

        public List<ProcessingMachineBlock.Recipe> sources() {
            return sources;
        }

        public boolean isEmpty() {
            return next.isEmpty() && sources.isEmpty();
        }
    }
}
