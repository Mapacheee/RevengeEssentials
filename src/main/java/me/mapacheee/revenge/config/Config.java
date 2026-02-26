package me.mapacheee.revenge.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@Configurate("config")
public record Config(
        int defaultMaxHomes,
        boolean forceSpawnOnJoin,
        int tpaTimeoutSeconds,
        int tpaCooldownSeconds,
        float defaultWalkSpeed,
        float defaultFlySpeed,
        String rtpTargetServer,
        String rtpTargetWorld,
        int rtpRadius,
        int rtpCooldownSeconds) {}
