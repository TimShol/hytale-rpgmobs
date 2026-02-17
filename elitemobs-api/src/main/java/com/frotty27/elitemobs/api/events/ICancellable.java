package com.frotty27.elitemobs.api.events;

/**
 * Interface for events that can be cancelled by listeners.
 *
 * <p>When an event is cancelled, the action it represents will not be carried out.
 * For example, cancelling an {@link EliteMobSpawnedEvent} prevents the elite mob
 * from spawning, and cancelling an {@link EliteMobDamageDealtEvent} prevents the
 * damage from being applied.</p>
 *
 * @since 1.1.0
 */
public interface ICancellable {

    /**
     * Returns whether this event has been cancelled.
     *
     * @return {@code true} if the event is cancelled, {@code false} otherwise
     */
    boolean isCancelled();

    /**
     * Sets whether this event should be cancelled.
     *
     * @param cancelled {@code true} to cancel the event, {@code false} to allow it
     */
    void setCancelled(boolean cancelled);
}
