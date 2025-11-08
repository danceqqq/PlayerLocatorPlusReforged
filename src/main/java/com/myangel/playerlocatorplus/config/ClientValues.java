package com.myangel.playerlocatorplus.config;

public record ClientValues(
        boolean visible,
        boolean visibleEmpty,
        boolean acceptServerConfig,
        boolean fadeMarkers,
        double fadeStart,
        double fadeEnd,
        double fadeEndOpacity,
        boolean showHeight,
        boolean alwaysShowHeads,
        boolean showHeadsOnTab,
        boolean showNamesOnTab
) {
    public static ClientValues defaults() {
        return new ClientValues(true, false, true, true, 100.0, 1000.0, 0.3, true, false, true, true);
    }
}
