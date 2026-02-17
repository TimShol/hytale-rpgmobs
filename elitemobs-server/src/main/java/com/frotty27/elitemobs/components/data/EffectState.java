package com.frotty27.elitemobs.components.data;

public record EffectState(
    long appliedTick,
    long durationTicks,
    int stackCount, boolean applied) {
}
