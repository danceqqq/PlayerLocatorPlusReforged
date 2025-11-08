package com.myangel.playerlocatorplus.client;

public final class Animatable {
    private float naturalFreq = 120f;
    private float targetValue;
    private float currentValue;
    private float lastDisplacement;
    private float lastVelocity;

    public Animatable(float initialValue) {
        this.targetValue = initialValue;
        this.currentValue = initialValue;
        this.lastDisplacement = initialValue;
    }

    public void setNaturalFreq(float naturalFreq) {
        this.naturalFreq = naturalFreq;
    }

    public float getCurrentValue() {
        return currentValue;
    }

    public void setTargetValue(float targetValue) {
        this.targetValue = targetValue;
    }

    public void updateValues(float timeElapsedMs) {
        float adjustedDisplacement = lastDisplacement - targetValue;
        double deltaT = timeElapsedMs / 1000.0;

        double coeffA = adjustedDisplacement;
        double coeffB = lastVelocity + naturalFreq * adjustedDisplacement;
        double nFdT = -naturalFreq * deltaT;

        double displacement = (coeffA + coeffB * deltaT) * Math.exp(nFdT);
        double currentVelocity = ((coeffA + coeffB * deltaT) * Math.exp(nFdT) * (-naturalFreq)) + coeffB * Math.exp(nFdT);

        float newValue = (float) (displacement + targetValue);
        float newVelocity = (float) currentVelocity;

        lastDisplacement = newValue;
        lastVelocity = newVelocity;
        currentValue = newValue;
    }

    public float getTargetValue() {
        return targetValue;
    }
}
