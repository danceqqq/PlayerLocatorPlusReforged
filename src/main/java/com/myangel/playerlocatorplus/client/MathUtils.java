package com.myangel.playerlocatorplus.client;

public final class MathUtils {
    private MathUtils() {
    }

    public static double calculateHorizontalFov(double verticalFovDegrees, int width, int height) {
        double fovRad = Math.toRadians(verticalFovDegrees / 2.0);
        double d = height / 2.0 / Math.tan(fovRad);
        double horizontal = Math.atan(width / 2.0 / d) * 2.0;
        return Math.toDegrees(horizontal);
    }
}
