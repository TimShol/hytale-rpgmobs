package com.frotty27.elitemobs.api;

import com.frotty27.elitemobs.api.events.*;

/**
 * Listener interface for receiving EliteMobs events.
 *
 * <p>Implement this interface and override the event handlers you are interested in.
 * All handler methods have default no-op implementations, so you only need to override
 * the ones relevant to your mod.</p>
 *
 * <p>Register your listener via {@link EliteMobsAPI#registerListener(IEliteMobsEventListener)}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * public class MyListener implements IEliteMobsEventListener {
 *     @Override
 *     public void onEliteMobDeath(EliteMobDeathEvent event) {
 *         // React to elite mob deaths
 *     }
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public interface IEliteMobsEventListener {

    /**
     * Called when an elite mob is spawned into the world.
     *
     * @param event the spawn event; may be cancelled to prevent the spawn
     */
    default void onEliteMobSpawned(EliteMobSpawnedEvent event) {}

    /**
     * Called when an elite mob dies.
     *
     * @param event the death event containing the killer reference and death position
     */
    default void onEliteMobDeath(EliteMobDeathEvent event) {}

    /**
     * Called when an elite mob's drops are about to be spawned.
     *
     * <p>Listeners may modify the drop list or cancel the event to suppress drops entirely.</p>
     *
     * @param event the drops event; may be cancelled to suppress all drops
     */
    default void onEliteMobDrops(EliteMobDropsEvent event) {}

    /**
     * Called when an elite mob deals damage to another entity.
     *
     * <p>Listeners may modify the damage multiplier or cancel the event.</p>
     *
     * @param event the damage dealt event; may be cancelled to prevent the damage
     */
    default void onEliteMobDamageDealt(EliteMobDamageDealtEvent event) {}

    /**
     * Called when an elite mob receives damage from another entity or source.
     *
     * @param event the damage received event
     */
    default void onEliteMobDamageReceived(EliteMobDamageReceivedEvent event) {}

    /**
     * Called during a reconciliation pass, allowing listeners to synchronize
     * their state with the current elite mob data.
     *
     * @param event the reconcile event
     */
    default void onReconcile(EliteMobReconcileEvent event) {}

    /**
     * Called when an elite mob begins executing an ability.
     *
     * <p>This event may be cancelled to prevent the ability from starting.</p>
     *
     * @param event the ability started event; may be cancelled
     */
    default void onEliteMobAbilityStarted(EliteMobAbilityStartedEvent event) {}

    /**
     * Called when an elite mob successfully completes an ability.
     *
     * @param event the ability completed event
     */
    default void onEliteMobAbilityCompleted(EliteMobAbilityCompletedEvent event) {}

    /**
     * Called when an elite mob's ability is interrupted before completion.
     *
     * @param event the ability interrupted event, including the interruption reason
     */
    default void onEliteMobAbilityInterrupted(EliteMobAbilityInterruptedEvent event) {}

    /**
     * Called after health, damage, and model scaling have been applied to an elite mob.
     *
     * @param event the scaling applied event containing all computed multipliers
     */
    default void onScalingApplied(EliteMobScalingAppliedEvent event) {}

    /**
     * Called when an elite mob acquires an aggro target.
     *
     * @param event the aggro event containing the target reference
     */
    default void onEliteMobAggro(EliteMobAggroEvent event) {}

    /**
     * Called when an elite mob loses its aggro target.
     *
     * @param event the deaggro event
     */
    default void onEliteMobDeaggro(EliteMobDeaggroEvent event) {}
}
