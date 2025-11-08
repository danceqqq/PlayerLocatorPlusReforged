package com.myangel.playerlocatorplus.util;

public class PlayerData {
    private int customColor = 0xFFFFFF;

    public int customColor() {
        return customColor;
    }

    public void setCustomColor(int customColor) {
        this.customColor = customColor & 0xFFFFFF;
    }
}
