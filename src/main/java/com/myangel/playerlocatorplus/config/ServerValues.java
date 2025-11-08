package com.myangel.playerlocatorplus.config;

public record ServerValues(
        boolean enabled,
        boolean sendServerConfig,
        boolean sendDistance,
        int maxDistance,
        double directionPrecision,
        int ticksBetweenUpdates,
        boolean sneakingHides,
        boolean pumpkinHides,
        boolean mobHeadsHide,
        boolean invisibilityHides,
        ColorMode colorMode,
        int constantColor,
        ClientValues clientOverrides
) {
    public static ServerValues defaults() {
        return new ServerValues(
                true,
                true,
                true,
                0,
                300.0,
                5,
                true,
                true,
                true,
                true,
                ColorMode.UUID,
                0xFFFFFF,
                ClientValues.defaults()
        );
    }

    public double clampDistance(double distance) {
        if (maxDistance <= 0) {
            return distance;
        }
        return Math.min(distance, maxDistance);
    }
}
