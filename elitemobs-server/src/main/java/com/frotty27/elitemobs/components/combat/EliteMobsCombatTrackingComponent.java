package com.frotty27.elitemobs.components.combat;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsCombatTrackingComponent implements Component<EntityStore> {

    public enum CombatState {
        IDLE, IN_COMBAT
    }

    public CombatState state;
    public long stateChangedTick;

    @Nullable
    public Ref<EntityStore> damageBasedTarget;

    @Nullable
    public Ref<EntityStore> aiTarget;

    public long lastTargetUpdateTick;

    public static final BuilderCodec<EliteMobsCombatTrackingComponent> CODEC =
            BuilderCodec.builder(EliteMobsCombatTrackingComponent.class, EliteMobsCombatTrackingComponent::new)
                    .build();

    public EliteMobsCombatTrackingComponent() {
        this.state = CombatState.IDLE;
        this.stateChangedTick = 0;
        this.damageBasedTarget = null;
        this.aiTarget = null;
        this.lastTargetUpdateTick = 0;
    }

    @Override
    public Component<EntityStore> clone() {
        EliteMobsCombatTrackingComponent c = new EliteMobsCombatTrackingComponent();
        c.state = this.state;
        c.stateChangedTick = this.stateChangedTick;
        c.damageBasedTarget = this.damageBasedTarget;
        c.aiTarget = this.aiTarget;
        c.lastTargetUpdateTick = this.lastTargetUpdateTick;
        return c;
    }

    public @Nullable Ref<EntityStore> getBestTarget() {
        if (damageBasedTarget != null && damageBasedTarget.isValid()) {
            return damageBasedTarget;
        }
        if (aiTarget != null && aiTarget.isValid()) {
            return aiTarget;
        }
        return null;
    }

    public boolean isInCombat() {
        return state == CombatState.IN_COMBAT;
    }

    public void transitionToInCombat(@Nullable Ref<EntityStore> targetRef, long currentTick) {
        this.state = CombatState.IN_COMBAT;
        this.stateChangedTick = currentTick;
        if (targetRef != null) {
            this.damageBasedTarget = targetRef;
        }
    }

    public void transitionToIdle(long currentTick) {
        this.state = CombatState.IDLE;
        this.stateChangedTick = currentTick;
        this.damageBasedTarget = null;
        this.aiTarget = null;
    }

    public void updateDamageTarget(@Nullable Ref<EntityStore> attackerRef, long currentTick) {
        this.damageBasedTarget = attackerRef;
        this.stateChangedTick = currentTick;
    }

    public void updateAITarget(@Nullable Ref<EntityStore> targetRef) {
        this.aiTarget = targetRef;
    }

}
