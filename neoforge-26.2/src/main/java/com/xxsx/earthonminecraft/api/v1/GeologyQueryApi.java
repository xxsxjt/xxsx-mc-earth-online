package com.xxsx.earthonminecraft.api.v1;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelReader;

public interface GeologyQueryApi {
    Optional<GeologySample> inspect(LevelReader level, BlockPos pos);

    Optional<GeologySample> describe(Identifier blockId);

    enum DepositRole {
        ORE,
        VEIN,
        SEAM,
        BED,
        PLACER,
        LATERITE,
        CARBONATITE,
        ROCK,
        SOIL,
        OTHER
    }

    record GeologySample(Identifier blockId, DepositRole role, String formula, String categoryKey,
                         String sourceKey, String processKey, boolean earthNaturalSource) {
    }
}
