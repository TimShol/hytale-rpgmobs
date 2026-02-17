package com.frotty27.elitemobs.components.ability;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsAbilityLockComponent implements Component<EntityStore> {

    public String activeAbilityId;

    public boolean chainStartPending;

    public long chainStartedAtTick = Long.MIN_VALUE;

    public static final int CHAIN_START_GRACE_TICKS = 5;

    public static final BuilderCodec<EliteMobsAbilityLockComponent> CODEC =
            BuilderCodec.builder(EliteMobsAbilityLockComponent.class, EliteMobsAbilityLockComponent::new)
                    .build();

    public EliteMobsAbilityLockComponent() {
        this.activeAbilityId = null;
        this.chainStartPending = false;
    }

    @Override
    public Component<EntityStore> clone() {
        return new EliteMobsAbilityLockComponent();
    }

    public boolean isLocked() {
        return activeAbilityId != null;
    }

    public boolean isChainStartPending() {
        return chainStartPending;
    }

    public boolean isWithinStartGracePeriod(long currentTick) {
        if (chainStartedAtTick == Long.MIN_VALUE) return false;
        return (currentTick - chainStartedAtTick) < CHAIN_START_GRACE_TICKS;
    }

    public void lock(String abilityId) {
        this.activeAbilityId = abilityId;
        this.chainStartPending = true;
        this.chainStartedAtTick = Long.MIN_VALUE;
    }

    public void unlock() {
        this.activeAbilityId = null;
        this.chainStartPending = false;
        this.chainStartedAtTick = Long.MIN_VALUE;
    }

    public void markChainStarted(long currentTick) {
        this.chainStartPending = false;
        this.chainStartedAtTick = currentTick;
    }
}
