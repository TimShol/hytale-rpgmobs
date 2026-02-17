package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Abstract base class for all EliteMobs events.
 *
 * <p>Every elite mob event carries the entity reference, tier index, and role name
 * of the elite mob involved. Subclasses add event-specific data such as damage
 * amounts, positions, or target references.</p>
 *
 * @since 1.1.0
 */
public abstract class EliteMobEvent {

    private final Ref<EntityStore> entityRef;
    private final int tier;
    private final String roleName;

    /**
     * Constructs a new elite mob event.
     *
     * @param entityRef the entity reference of the elite mob involved in this event
     * @param tier      the tier index of the elite mob
     * @param roleName  the role name (NPC type identifier) of the elite mob
     */
    protected EliteMobEvent(Ref<EntityStore> entityRef, int tier, String roleName) {
        this.entityRef = entityRef;
        this.tier = tier;
        this.roleName = roleName;
    }

    /**
     * Returns the entity reference of the elite mob involved in this event.
     *
     * @return the entity reference, never {@code null}
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Returns the tier index of the elite mob.
     *
     * <p>Tier determines the mob's difficulty level, affecting health, damage,
     * model scale, and available abilities.</p>
     *
     * @return the zero-based tier index
     */
    public int getTier() {
        return tier;
    }

    /**
     * Returns the role name of the elite mob.
     *
     * <p>The role name identifies the NPC type (e.g., {@code "zombie"}, {@code "skeleton"})
     * and is used for rule matching and asset resolution.</p>
     *
     * @return the role name, never {@code null}
     */
    public String getRoleName() {
        return roleName;
    }
}
