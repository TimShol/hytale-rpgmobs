package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an elite mob deals damage to another entity.
 *
 * <p>Listeners can modify the damage {@linkplain #setMultiplier(float) multiplier}
 * to scale the outgoing damage, or {@linkplain #setCancelled(boolean) cancel} the
 * event to prevent the damage entirely.</p>
 *
 * <p>The final damage dealt is computed as {@code baseDamage * multiplier}.</p>
 *
 * @since 1.1.0
 */
public final class EliteMobDamageDealtEvent extends EliteMobEvent implements ICancellable {

    private final Ref<EntityStore> victimRef;
    private final float baseDamage;
    private float multiplier;
    private boolean cancelled;

    /**
     * Constructs a new damage dealt event.
     *
     * @param attackerRef the entity reference of the attacking elite mob
     * @param tier        the tier index of the attacking elite mob
     * @param roleName    the role name of the attacking elite mob
     * @param victimRef   the entity reference of the entity being damaged
     * @param baseDamage  the base (unscaled) damage amount
     * @param multiplier  the initial damage multiplier applied by the elite mob's tier
     */
    public EliteMobDamageDealtEvent(Ref<EntityStore> attackerRef, int tier, String roleName,
                                    Ref<EntityStore> victimRef, float baseDamage, float multiplier) {
        super(attackerRef, tier, roleName);
        this.victimRef = victimRef;
        this.baseDamage = baseDamage;
        this.multiplier = multiplier;
    }

    /**
     * Returns the entity reference of the entity being damaged.
     *
     * @return the victim's entity reference, never {@code null}
     */
    public Ref<EntityStore> getVictimRef() {
        return victimRef;
    }

    /**
     * Returns the base (unscaled) damage amount before the multiplier is applied.
     *
     * @return the base damage value
     */
    public float getBaseDamage() {
        return baseDamage;
    }

    /**
     * Returns the current damage multiplier.
     *
     * <p>The final damage is computed as {@code baseDamage * multiplier}.</p>
     *
     * @return the damage multiplier
     */
    public float getMultiplier() {
        return multiplier;
    }

    /**
     * Sets the damage multiplier.
     *
     * <p>Modify this value to scale the damage dealt by the elite mob.
     * The final damage is computed as {@code baseDamage * multiplier}.</p>
     *
     * @param multiplier the new damage multiplier
     */
    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
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
