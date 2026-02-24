package me.mapacheee.revenge.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@Configurate("messages")
public record Messages(
                String prefix,
                String tpaRequestSent,
                String tpaRequestReceived,
                String tpaRequestAcceptedSender,
                String tpaRequestAcceptedTarget,
                String tpaRequestDeniedSender,
                String tpaRequestDeniedTarget,
                String tpaRequestExpired,
                String tpaCooldown,
                String tpaNoPending,
                String tpaHereRequestSent,
                String tpaHereRequestReceived,
                String tpTeleported,
                String tpPlayerNotFound,
                String homeSet,
                String homeDeleted,
                String homeTeleported,
                String homeNotFound,
                String homeMaxReached,
                String homeList,
                String homeListEntry,
                String homeGuiTitle,
                String homeGuiNoHomes,
                String spawnSet,
                String spawnTeleported,
                String spawnNotSet,
                String backTeleported,
                String backNoLocation,
                String backCrossServer,
                String flyEnabled,
                String flyDisabled,
                String flyEnabledOther,
                String flyDisabledOther,
                String speedSet,
                String speedSetWalk,
                String speedSetFly,
                String speedInvalidValue,
                String noPermission,
                String reloaded,
                String playerNotOnline,
                String crossServerTeleporting,
                String crossServerJoin,
                String crossServerQuit) {
}
