package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an elite mob acquires an aggro target.
 *
 * <p>This event is triggered when the elite mob begins targeting a specific entity,
 * typically a player. It can be used to react to combat initiation or to trigger
 * custom behaviors when an elite mob locks onto a target.</p>
 *
 * @since 1.1.0
 */
public final class EliteMobAggroEvent extends EliteMobEvent {

    private final Ref<EntityStore> targetRef;

    /**
     * Constructs a new aggro event.
     *
     * @param mobRef    the entity reference of the elite mob that acquired a target
     * @param targetRef the entity reference of the entity being targeted
     * @param tier      the tier index of the elite mob
     * @param roleName  the role name of the elite mob
     */
    public EliteMobAggroEvent(Ref<EntityStore> mobRef, Ref<EntityStore> targetRef, int tier, String roleName) {
        super(mobRef, tier, roleName);
        this.targetRef = targetRef;
    }

    /**
     * Returns the entity reference of the entity being targeted by the elite mob.
     *
     * @return the target's entity reference, never {@code null}
     */
    public Ref<EntityStore> targetRef() {
        return targetRef;
    }
}
