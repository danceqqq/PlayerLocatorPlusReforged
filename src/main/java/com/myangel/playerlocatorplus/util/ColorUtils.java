package com.myangel.playerlocatorplus.util;

import java.util.Random;
import java.util.UUID;

public final class ColorUtils {
    private ColorUtils() {
    }

    public static int uuidToColor(UUID uuid) {
        Random random = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        float hue = random.nextFloat() * 360f;
        float saturation = random.nextFloat() / 4f + 0.75f;
        float lightness = random.nextFloat() / 2f + 0.5f;
        return hslToColor(hue, saturation, lightness);
    }

    private static int hslToColor(float h, float s, float l) {
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float m = l - 0.5f * c;
        float x = c * (1f - Math.abs((h / 60f % 2f) - 1f));

        int hueSegment = (int) (h / 60f);
        float r = 0f;
        float g = 0f;
        float b = 0f;

        switch (hueSegment) {
            case 0 -> {
                r = c + m;
                g = x + m;
                b = m;
            }
            case 1 -> {
                r = x + m;
                g = c + m;
                b = m;
            }
            case 2 -> {
                r = m;
                g = c + m;
                b = x + m;
            }
            case 3 -> {
                r = m;
                g = x + m;
                b = c + m;
            }
            case 4 -> {
                r = x + m;
                g = m;
                b = c + m;
            }
            case 5, 6 -> {
                r = c + m;
                g = m;
                b = x + m;
            }
        }

        int ri = Math.min(255, Math.max(0, Math.round(r * 255f)));
        int gi = Math.min(255, Math.max(0, Math.round(g * 255f)));
        int bi = Math.min(255, Math.max(0, Math.round(b * 255f)));
        return (ri << 16) | (gi << 8) | bi;
    }
}
