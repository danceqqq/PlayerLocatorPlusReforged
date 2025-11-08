package com.myangel.playerlocatorplus.config;

public enum ColorMode {
    UUID,
    TEAM_COLOR,
    CUSTOM,
    CONSTANT;

    public String translationKey() {
        return "text.autoconfig.player-locator-plus.option.colorMode." + switch (this) {
            case UUID -> "uuid";
            case TEAM_COLOR -> "team_color";
            case CUSTOM -> "custom";
            case CONSTANT -> "constant";
        };
    }
}
