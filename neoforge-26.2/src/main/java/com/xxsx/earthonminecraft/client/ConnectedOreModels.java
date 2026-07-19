package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.EarthOnMinecraft;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.registries.DeferredBlock;

final class ConnectedOreModels {
    private static final List<ModelSet> MODEL_SETS = createModelSets();

    private ConnectedOreModels() {
    }

    static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        for (ModelSet set : MODEL_SETS) {
            event.register(set.centerKey, SimpleUnbakedStandaloneModel.blockStateModel(set.centerModel));
            for (Direction direction : Direction.values()) {
                event.register(set.edgeKeys.get(direction),
                        SimpleUnbakedStandaloneModel.blockStateModel(set.edgeModels.get(direction)));
            }
        }
    }

    static void replaceBakedModels(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> stateModels = event.getBakingResult().blockStateModels();
        var standaloneModels = event.getBakingResult().standaloneModels();

        for (ModelSet set : MODEL_SETS) {
            BlockStateModel center = standaloneModels.get(set.centerKey);
            EnumMap<Direction, BlockStateModel> edges = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                BlockStateModel edge = standaloneModels.get(set.edgeKeys.get(direction));
                if (edge != null) {
                    edges.put(direction, edge);
                }
            }

            if (center == null || edges.size() != Direction.values().length) {
                EarthOnMinecraft.LOGGER.error("Connected ore models are incomplete for {}", set.block.getId());
                continue;
            }

            ConnectedOreBlockStateModel connectedModel = new ConnectedOreBlockStateModel(center, edges);
            for (BlockState state : set.block.get().getStateDefinition().getPossibleStates()) {
                if (stateModels.containsKey(state)) {
                    stateModels.put(state, connectedModel);
                }
            }
        }
    }

    private static List<ModelSet> createModelSets() {
        List<ModelSet> sets = new ArrayList<>();
        for (DeferredBlock<Block> block : EarthOnMinecraft.connectedOreBlocks()) {
            sets.add(new ModelSet(block));
        }
        return List.copyOf(sets);
    }

    private static final class ModelSet {
        private final DeferredBlock<Block> block;
        private final Identifier centerModel;
        private final StandaloneModelKey<BlockStateModel> centerKey;
        private final EnumMap<Direction, Identifier> edgeModels = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, StandaloneModelKey<BlockStateModel>> edgeKeys = new EnumMap<>(Direction.class);

        private ModelSet(DeferredBlock<Block> block) {
            this.block = block;
            String id = block.getId().getPath();
            this.centerModel = modelId(id + "_center");
            this.centerKey = key("connected_ore/" + id + "/center");
            for (Direction direction : Direction.values()) {
                String suffix = direction.getSerializedName();
                this.edgeModels.put(direction, modelId(id + "_edge_" + suffix));
                this.edgeKeys.put(direction, key("connected_ore/" + id + "/edge_" + suffix));
            }
        }

        private static Identifier modelId(String name) {
            return Identifier.fromNamespaceAndPath(EarthOnMinecraft.MODID, "block/connected/" + name);
        }

        private static StandaloneModelKey<BlockStateModel> key(String name) {
            ModelDebugName debugName = () -> EarthOnMinecraft.MODID + ":" + name;
            return new StandaloneModelKey<>(debugName);
        }
    }

    private static final class ConnectedOreBlockStateModel extends DelegateBlockStateModel {
        private final BlockStateModel center;
        private final EnumMap<Direction, BlockStateModel> edges;

        private ConnectedOreBlockStateModel(BlockStateModel center, EnumMap<Direction, BlockStateModel> edges) {
            super(center);
            this.center = center;
            this.edges = new EnumMap<>(edges);
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                                 List<BlockStateModelPart> parts) {
            center.collectParts(level, pos, state, random, parts);
            int exposedEdges = exposedEdges(level, pos, state);
            for (Direction direction : Direction.values()) {
                if ((exposedEdges & bit(direction)) != 0) {
                    edges.get(direction).collectParts(level, pos, state, random, parts);
                }
            }
        }

        @Override
        public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
            return new GeometryKey(this, exposedEdges(level, pos, state));
        }

        private static int exposedEdges(BlockAndTintGetter level, BlockPos pos, BlockState state) {
            int mask = 0;
            for (Direction direction : Direction.values()) {
                BlockState neighbor = level.getBlockState(pos.relative(direction));
                if (neighbor.getBlock() != state.getBlock()) {
                    mask |= bit(direction);
                }
            }
            return mask;
        }

        private static int bit(Direction direction) {
            return 1 << direction.ordinal();
        }

        private record GeometryKey(ConnectedOreBlockStateModel model, int exposedEdges) {
        }
    }
}
