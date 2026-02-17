package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an elite mob loses its aggro target.
 *
 * <p>This occurs when the elite mob is no longer actively targeting an entity,
 * for example when the target moves out of range, dies, or disconnects.
 * Listeners can use this event to clean up combat-related state.</p>
 *
 * @since 1.1.0
 */
public final class EliteMobDeaggroEvent extends EliteMobEvent {

    /**
     * Constructs a new deaggro event.
     *
     * @param mobRef   the entity reference of the elite mob that lost its target
     * @param tier     the tier index of the elite mob
     * @param roleName the role name of the elite mob
     */
    public EliteMobDeaggroEvent(Ref<EntityStore> mobRef, int tier, String roleName) {
        super(mobRef, tier, roleName);
    }
}
