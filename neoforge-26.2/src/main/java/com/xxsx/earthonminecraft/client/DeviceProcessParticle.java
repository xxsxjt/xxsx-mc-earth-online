package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.MachineFeedback;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

final class DeviceProcessParticle extends SimpleAnimatedParticle {
    DeviceProcessParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
                          double zSpeed, SpriteSet sprites, MachineFeedback.DeviceKind kind, RandomSource random) {
        super(level, x, y, z, sprites, -0.04F);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.friction = kind == MachineFeedback.DeviceKind.STEAM_TURBINE_GENERATOR ? 0.95F : 0.91F;
        this.hasPhysics = false;
        this.lifetime = (kind == MachineFeedback.DeviceKind.STEAM_TURBINE_GENERATOR ? 22 : 18) + random.nextInt(7);
        this.quadSize = (kind == MachineFeedback.DeviceKind.STEAM_TURBINE_GENERATOR ? 0.11F : 0.09F)
                * (0.84F + random.nextFloat() * 0.32F);
        this.setSpriteFromAge(sprites);
    }
}
