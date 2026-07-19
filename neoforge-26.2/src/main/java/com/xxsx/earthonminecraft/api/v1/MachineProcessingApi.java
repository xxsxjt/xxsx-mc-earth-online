package com.xxsx.earthonminecraft.api.v1;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public interface MachineProcessingApi {
    List<MachineSpec> machines();

    Optional<MachineSpec> machine(Identifier machineId);

    List<ProcessingRoute> routes();

    List<ProcessingRoute> routesFor(Identifier machineId);

    record MachineSpec(Identifier id, String displayNameKey, String family, int processTicks,
                       int energyPerTick, String powerMode, String multiblockPattern, int routeCount) {
    }

    record ProcessingRoute(Identifier machineId, Identifier input, List<RouteOutput> outputs,
                           int processTicks, int energyPerTick, String note) {
        public ProcessingRoute {
            outputs = List.copyOf(outputs);
        }
    }

    record RouteOutput(Identifier item, int count) {
    }
}
