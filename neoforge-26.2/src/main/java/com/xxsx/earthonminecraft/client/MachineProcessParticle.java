package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.ProcessingMachineBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

final class MachineProcessParticle extends SimpleAnimatedParticle {
    MachineProcessParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
                           double zSpeed, SpriteSet sprites, ProcessingMachineBlock.Kind kind, RandomSource random) {
        super(level, x, y, z, sprites, gravity(kind));
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.friction = friction(kind);
        this.hasPhysics = false;
        this.lifetime = lifetime(kind, random);
        this.quadSize = size(kind, random);
        this.setSpriteFromAge(sprites);
    }

    private static float gravity(ProcessingMachineBlock.Kind kind) {
        return switch (kind.processFamily()) {
            case COMMINUTION, CLASSIFICATION, FORMING, MIXING -> 0.18F;
            case WET_PROCESS, ELECTROCHEMICAL -> -0.01F;
            case THERMAL, COLUMN, REACTION, CRYSTALLIZATION -> -0.045F;
        };
    }

    private static float friction(ProcessingMachineBlock.Kind kind) {
        return switch (kind.processFamily()) {
            case COMMINUTION, FORMING -> 0.84F;
            case THERMAL, COLUMN -> 0.94F;
            default -> 0.90F;
        };
    }

    private static int lifetime(ProcessingMachineBlock.Kind kind, RandomSource random) {
        int base = switch (kind.processFamily()) {
            case THERMAL, COLUMN, REACTION -> 22;
            case WET_PROCESS, ELECTROCHEMICAL, CRYSTALLIZATION -> 18;
            default -> 13;
        };
        return base + random.nextInt(7);
    }

    private static float size(ProcessingMachineBlock.Kind kind, RandomSource random) {
        float base = switch (kind.processFamily()) {
            case THERMAL, COLUMN -> 0.115F;
            case COMMINUTION, FORMING -> 0.075F;
            default -> 0.09F;
        };
        return base * (0.82F + random.nextFloat() * 0.36F);
    }
}
