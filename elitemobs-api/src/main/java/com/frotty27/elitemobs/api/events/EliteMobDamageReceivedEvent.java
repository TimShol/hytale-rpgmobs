package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Event fired when an elite mob receives damage.
 *
 * <p>This is an informational event; the damage has already been determined.
 * The attacker reference may be {@code null} if the damage source is
 * environmental (e.g., fall damage, fire).</p>
 *
 * @since 1.1.0
 */
public final class EliteMobDamageReceivedEvent extends EliteMobEvent {

    private final @Nullable Ref<EntityStore> attackerRef;
    private final float damageAmount;

    /**
     * Constructs a new damage received event.
     *
     * @param victimRef    the entity reference of the elite mob that received damage
     * @param tier         the tier index of the elite mob
     * @param roleName     the role name of the elite mob
     * @param attackerRef  the entity reference of the attacker, or {@code null} if
     *                     the damage was not caused by another entity
     * @param damageAmount the amount of damage received
     */
    public EliteMobDamageReceivedEvent(Ref<EntityStore> victimRef, int tier, String roleName,
                                       @Nullable Ref<EntityStore> attackerRef, float damageAmount) {
        super(victimRef, tier, roleName);
        this.attackerRef = attackerRef;
        this.damageAmount = damageAmount;
    }

    /**
     * Returns the entity reference of the attacker, if any.
     *
     * @return the attacker's entity reference, or {@code null} if the damage was
     *         not caused by another entity
     */
    public @Nullable Ref<EntityStore> getAttackerRef() {
        return attackerRef;
    }

    /**
     * Returns the amount of damage received by the elite mob.
     *
     * @return the damage amount
     */
    public float getDamageAmount() {
        return damageAmount;
    }
}
