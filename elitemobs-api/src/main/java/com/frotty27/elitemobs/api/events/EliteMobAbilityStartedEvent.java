package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Event fired when an elite mob begins executing an ability.
 *
 * <p>This event implements {@link ICancellable}. Cancelling it will prevent the
 * ability from starting, and the elite mob will remain in its idle state.</p>
 *
 * <p>The {@linkplain #getAbilityId() ability ID} is a string identifier that
 * corresponds to the enum names in {@link com.frotty27.elitemobs.api.query.AbilityType}
 * (e.g., {@code "CHARGE_LEAP"}, {@code "HEAL_LEAP"}, {@code "SUMMON_UNDEAD"}).</p>
 *
 * @since 1.1.0
 */
public final class EliteMobAbilityStartedEvent implements ICancellable {

    private final Ref<EntityStore> entityRef;
    private final String abilityId;
    private final int tierIndex;
    private final @Nullable Ref<EntityStore> targetRef;
    private boolean cancelled;

    /**
     * Constructs a new ability started event.
     *
     * @param entityRef the entity reference of the elite mob starting the ability
     * @param abilityId the string identifier of the ability being started
     * @param tierIndex the zero-based tier index of the elite mob
     * @param targetRef the entity reference of the ability target, or {@code null}
     *                  if the ability has no specific target
     */
    public EliteMobAbilityStartedEvent(Ref<EntityStore> entityRef, String abilityId, int tierIndex,
                                       @Nullable Ref<EntityStore> targetRef) {
        this.entityRef = entityRef;
        this.abilityId = abilityId;
        this.tierIndex = tierIndex;
        this.targetRef = targetRef;
    }

    /**
     * Returns the entity reference of the elite mob starting the ability.
     *
     * @return the entity reference, never {@code null}
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Returns the string identifier of the ability being started.
     *
     * <p>This corresponds to the {@link com.frotty27.elitemobs.api.query.AbilityType}
     * enum names (e.g., {@code "CHARGE_LEAP"}).</p>
     *
     * @return the ability identifier, never {@code null}
     */
    public String getAbilityId() {
        return abilityId;
    }

    /**
     * Returns the zero-based tier index of the elite mob.
     *
     * @return the tier index
     */
    public int getTierIndex() {
        return tierIndex;
    }

    /**
     * Returns the entity reference of the ability target, if any.
     *
     * @return the target's entity reference, or {@code null} if the ability has no
     *         specific target
     */
    public @Nullable Ref<EntityStore> getTargetRef() {
        return targetRef;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
