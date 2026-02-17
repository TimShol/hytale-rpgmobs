package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

/**
 * Event fired when an elite mob's drops are about to be spawned into the world.
 *
 * <p>Listeners may modify the {@linkplain #getDrops() drop list} (add, remove, or
 * replace items) or {@linkplain #setCancelled(boolean) cancel} the event to suppress
 * all drops entirely.</p>
 *
 * @since 1.1.0
 */
public final class EliteMobDropsEvent extends EliteMobEvent implements ICancellable {

    private final List<ItemStack> drops;
    private final Vector3d position;
    private boolean cancelled;

    /**
     * Constructs a new drops event.
     *
     * @param entityRef the entity reference of the elite mob whose drops are being spawned
     * @param tier      the tier index of the elite mob
     * @param roleName  the role name of the elite mob
     * @param drops     the mutable list of item stacks to drop; listeners may modify this list
     * @param position  the world position where the drops will be spawned
     */
    public EliteMobDropsEvent(Ref<EntityStore> entityRef, int tier, String roleName,
                              List<ItemStack> drops, Vector3d position) {
        super(entityRef, tier, roleName);
        this.drops = drops;
        this.position = position;
    }

    /**
     * Returns the mutable list of items to be dropped.
     *
     * <p>Listeners may add, remove, or replace entries in this list to customize
     * the drops. Changes are reflected in the final drop output.</p>
     *
     * @return the mutable drop list, never {@code null}
     */
    public List<ItemStack> getDrops() {
        return drops;
    }

    /**
     * Returns the world position where the drops will be spawned.
     *
     * @return the drop position, never {@code null}
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
