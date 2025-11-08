package com.myangel.playerlocatorplus.client;

import com.myangel.playerlocatorplus.config.ClientValues;
import com.myangel.playerlocatorplus.config.PlayerLocatorConfig;
import com.myangel.playerlocatorplus.config.ServerValues;

import java.util.Optional;

public final class ClientConfigState {
    private static volatile ClientValues localValues = PlayerLocatorConfig.getClientValues();
    private static volatile ServerValues serverValues = ServerValues.defaults();
    private static volatile Optional<ClientValues> serverOverrides = Optional.empty();
    private static volatile boolean serverConfigActive = false;

    private ClientConfigState() {
    }

    public static void refreshLocal() {
        localValues = PlayerLocatorConfig.getClientValues();
    }

    public static void applyServerConfig(ServerValues values) {
        serverValues = values;
        if (values.sendServerConfig()) {
            serverOverrides = Optional.ofNullable(values.clientOverrides());
            serverConfigActive = true;
        } else {
            serverOverrides = Optional.empty();
            serverConfigActive = false;
        }
    }

    public static void clearServerConfig() {
        serverOverrides = Optional.empty();
        serverValues = ServerValues.defaults();
        serverConfigActive = false;
    }

    public static EffectiveClientConfig effective() {
        ClientValues base = localValues;
        if (serverConfigActive && localValues.acceptServerConfig()) {
            base = serverOverrides.orElse(base);
        }
        return new EffectiveClientConfig(base, serverValues);
    }
}
