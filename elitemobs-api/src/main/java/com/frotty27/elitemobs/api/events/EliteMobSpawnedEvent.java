package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an elite mob is spawned into the world.
 *
 * <p>This event implements {@link ICancellable}. Cancelling it will prevent the
 * elite mob from completing its spawn initialization.</p>
 *
 * @since 1.1.0
 */
public final class EliteMobSpawnedEvent extends EliteMobEvent implements ICancellable {

    private final Vector3d position;
    private boolean cancelled;

    /**
     * Constructs a new spawned event.
     *
     * @param entityRef the entity reference of the spawned elite mob
     * @param tier      the tier index of the elite mob
     * @param roleName  the role name of the elite mob
     * @param position  the world position where the elite mob spawned
     */
    public EliteMobSpawnedEvent(Ref<EntityStore> entityRef, int tier, String roleName, Vector3d position) {
        super(entityRef, tier, roleName);
        this.position = position;
    }

    /**
     * Returns the world position where the elite mob spawned.
     *
     * @return the spawn position, never {@code null}
     */
    public Vector3d getPosition() {
        return position;
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
