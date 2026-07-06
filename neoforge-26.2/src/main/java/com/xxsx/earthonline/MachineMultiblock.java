package com.xxsx.earthonline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MachineMultiblock {
    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private MachineMultiblock() {
    }

    public static Pattern patternFor(ProcessingMachineBlock.Kind kind) {
        return switch (kind) {
            case GAS_SEPARATOR -> Pattern.HEAVY_FRAME;
            case FLOTATION_CELL, LEACHING_TANK, ELECTROLYTIC_CELL, CHEMICAL_REACTOR, POLYMERIZER -> Pattern.WET_VESSEL;
            case ROASTER, REDUCTION_FURNACE, INDUSTRIAL_KILN, STEAM_CRACKER -> Pattern.HEATED_LINE;
            case DISTILLATION_COLUMN, SYNTHESIS_LOOP, ABSORPTION_TOWER -> Pattern.TALL_COLUMN;
            default -> Pattern.NONE;
        };
    }

    public static boolean isComplete(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind) {
        return findMatch(level, controller, kind).isPresent();
    }

    public static Optional<Match> findMatch(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind) {
        return patternFor(kind).findMatch(level, controller);
    }

    public static Optional<BlockPos> findControllerForPanel(Level level, BlockPos panel) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    cursor.set(panel.getX() + dx, panel.getY() + dy, panel.getZ() + dz);
                    if (level.getBlockEntity(cursor) instanceof ProcessingMachineBlockEntity machine) {
                        Optional<Match> match = findMatch(level, cursor, machine.kind());
                        if (match.isPresent() && match.get().parts().contains(panel)) {
                            return Optional.of(cursor.immutable());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void syncAssembly(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind, boolean assembled) {
        clearAssemblyNear(level, controller);
        if (!assembled) {
            return;
        }
        findMatch(level, controller, kind).ifPresent(match -> {
            for (BlockPos part : match.parts()) {
                setAssembled(level, part, true);
            }
        });
    }

    private static void clearAssemblyNear(Level level, BlockPos controller) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 4; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    cursor.set(controller.getX() + dx, controller.getY() + dy, controller.getZ() + dz);
                    setAssembled(level, cursor, false);
                }
            }
        }
    }

    private static void setAssembled(Level level, BlockPos pos, boolean assembled) {
        var state = level.getBlockState(pos);
        if (state.hasProperty(SupportPartBlock.ASSEMBLED) && state.getValue(SupportPartBlock.ASSEMBLED) != assembled) {
            level.setBlock(pos, state.setValue(SupportPartBlock.ASSEMBLED, assembled), Block.UPDATE_CLIENTS);
        }
    }

    public enum Pattern {
        NONE("tooltip.earth_online.multiblock.none", "screen.earth_online.machine.structure.none") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return Optional.of(new Match(Direction.NORTH, List.of()));
            }
        },
        HEAVY_FRAME("tooltip.earth_online.multiblock.heavy_frame", "screen.earth_online.machine.structure.heavy_frame") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                for (Direction front : HORIZONTALS) {
                    Optional<Match> match = matchFootprint(level, controller, front,
                            new Requirement(front, EarthOnline.CONTROL_PANEL.get()),
                            new Requirement(front.getCounterClockWise(), EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(front.getClockWise(), EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(front.getOpposite(), EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(front.getOpposite(), front.getCounterClockWise(), EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(front.getOpposite(), front.getClockWise(), EarthOnline.INDUSTRIAL_MACHINE_CASING.get()));
                    if (match.isPresent()) {
                        return match;
                    }
                }
                return Optional.empty();
            }
        },
        WET_VESSEL("tooltip.earth_online.multiblock.wet_vessel", "screen.earth_online.machine.structure.wet_vessel") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                for (Direction front : HORIZONTALS) {
                    Direction left = front.getCounterClockWise();
                    Direction right = front.getClockWise();
                    Direction back = front.getOpposite();
                    Optional<Match> match = matchFootprint(level, controller, front,
                            new Requirement(front, EarthOnline.CONTROL_PANEL.get()),
                            new Requirement(left, EarthOnline.STEEL_PROCESS_PIPE.get()),
                            new Requirement(right, EarthOnline.STEEL_PROCESS_PIPE.get()),
                            new Requirement(back, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(back, left, EarthOnline.STEEL_PROCESS_PIPE.get()),
                            new Requirement(back, right, EarthOnline.STEEL_PROCESS_PIPE.get()));
                    if (match.isPresent()) {
                        return match;
                    }
                }
                return Optional.empty();
            }
        },
        HEATED_LINE("tooltip.earth_online.multiblock.heated_line", "screen.earth_online.machine.structure.heated_line") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                for (Direction front : HORIZONTALS) {
                    Direction left = front.getCounterClockWise();
                    Direction right = front.getClockWise();
                    Direction back = front.getOpposite();
                    Optional<Match> match = matchFootprint(level, controller, front,
                            new Requirement(front, EarthOnline.CONTROL_PANEL.get()),
                            new Requirement(left, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(right, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(back, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(1, EarthOnline.STEEL_PROCESS_PIPE.get()),
                            new Requirement(left, 1, EarthOnline.STEEL_PROCESS_PIPE.get()),
                            new Requirement(right, 1, EarthOnline.STEEL_PROCESS_PIPE.get()));
                    if (match.isPresent()) {
                        return match;
                    }
                }
                return Optional.empty();
            }
        },
        TALL_COLUMN("tooltip.earth_online.multiblock.tall_column", "screen.earth_online.machine.structure.tall_column") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                for (Direction front : HORIZONTALS) {
                    Direction left = front.getCounterClockWise();
                    Direction right = front.getClockWise();
                    Direction back = front.getOpposite();
                    Optional<Match> match = matchFootprint(level, controller, front,
                            new Requirement(front, EarthOnline.CONTROL_PANEL.get()),
                            new Requirement(left, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(right, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(back, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(back, left, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(back, right, EarthOnline.INDUSTRIAL_MACHINE_CASING.get()),
                            new Requirement(1, EarthOnline.STEEL_PROCESS_PIPE.get()),
                            new Requirement(2, EarthOnline.STEEL_PROCESS_PIPE.get()),
                            new Requirement(3, EarthOnline.STEEL_PROCESS_PIPE.get()));
                    if (match.isPresent()) {
                        return match;
                    }
                }
                return Optional.empty();
            }
        };

        private final String descriptionKey;
        private final String screenKey;

        Pattern(String descriptionKey, String screenKey) {
            this.descriptionKey = descriptionKey;
            this.screenKey = screenKey;
        }

        abstract Optional<Match> findMatch(Level level, BlockPos controller);

        public String descriptionKey() {
            return descriptionKey;
        }

        public String screenKey() {
            return screenKey;
        }
    }

    private static boolean has(Level level, BlockPos pos, Block block) {
        return level.getBlockState(pos).getBlock() == block;
    }

    private static Optional<Match> matchFootprint(Level level, BlockPos controller, Direction front, Requirement... requirements) {
        List<BlockPos> parts = new ArrayList<>();
        for (Requirement requirement : requirements) {
            BlockPos pos = requirement.resolve(controller);
            if (!has(level, pos, requirement.block())) {
                return Optional.empty();
            }
            parts.add(pos);
        }
        return Optional.of(new Match(front, List.copyOf(parts)));
    }

    public record Match(Direction front, List<BlockPos> parts) {
    }

    private record Requirement(int left, int back, int up, Block block) {
        Requirement(Direction direction, Block block) {
            this(0, direction == Direction.NORTH ? 1 : direction == Direction.SOUTH ? -1 : 0,
                    0, block, direction);
        }

        Requirement(Direction first, Direction second, Block block) {
            this(0, 0, 0, block, first, second);
        }

        Requirement(Direction direction, int up, Block block) {
            this(0, direction == Direction.NORTH ? 1 : direction == Direction.SOUTH ? -1 : 0,
                    up, block, direction);
        }

        Requirement(int up, Block block) {
            this(0, 0, up, block);
        }

        private Requirement(int left, int back, int up, Block block, Direction... directions) {
            this(leftOffset(directions), backOffset(directions), up, block);
        }

        BlockPos resolve(BlockPos controller) {
            return controller.offset(left, up, back);
        }

        private static int leftOffset(Direction... directions) {
            int x = 0;
            for (Direction direction : directions) {
                x += direction.getStepX();
            }
            return x;
        }

        private static int backOffset(Direction... directions) {
            int z = 0;
            for (Direction direction : directions) {
                z += direction.getStepZ();
            }
            return z;
        }
    }
}
