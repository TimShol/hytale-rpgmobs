package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Event fired when an elite mob dies.
 *
 * <p>Contains information about who killed the mob (if applicable) and the
 * position of death, which can be used for drop placement or visual effects.</p>
 *
 * @since 1.1.0
 */
public final class EliteMobDeathEvent extends EliteMobEvent {

    private final @Nullable Ref<EntityStore> killerRef;
    private final Vector3d position;

    /**
     * Constructs a new death event.
     *
     * @param entityRef the entity reference of the elite mob that died
     * @param tier      the tier index of the elite mob
     * @param roleName  the role name of the elite mob
     * @param killerRef the entity reference of the killer, or {@code null} if the death
     *                  was not caused by another entity (e.g., environmental damage)
     * @param position  the world position where the elite mob died
     */
    public EliteMobDeathEvent(Ref<EntityStore> entityRef, int tier, String roleName,
                              @Nullable Ref<EntityStore> killerRef, Vector3d position) {
        super(entityRef, tier, roleName);
        this.killerRef = killerRef;
        this.position = position;
    }

    /**
     * Returns the entity reference of the killer, if any.
     *
     * @return the killer's entity reference, or {@code null} if the death was not
     *         caused by another entity
     */
    public @Nullable Ref<EntityStore> getKillerRef() {
        return killerRef;
    }

    /**
     * Returns the world position where the elite mob died.
     *
     * @return the death position, never {@code null}
     */
    public Vector3d getPosition() {
        return position;
    }
}
