package com.myangel.playerlocatorplus.client;

import com.myangel.playerlocatorplus.config.ClientValues;
import com.myangel.playerlocatorplus.config.ServerValues;

public record EffectiveClientConfig(ClientValues client, ServerValues server) {
    public boolean isEnabled() {
        return server.enabled();
    }
}
