package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.api.events.EliteMobAbilityCompletedEvent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.ability.EliteMobsAbilityLockComponent;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class EliteMobsAbilityCompletionSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final InteractionType ABILITY_INTERACTION_TYPE = InteractionType.Ability2;

    private final EliteMobsPlugin plugin;

    public EliteMobsAbilityCompletionSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(plugin.getEliteMobsComponentType(), plugin.getAbilityLockComponentType()
        );
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        EliteMobsAbilityLockComponent lock = chunk.getComponent(entityIndex, plugin.getAbilityLockComponentType());
        if (lock == null || !lock.isLocked()) return;

        long currentTick = plugin.getTickClock().getTick();

        if (lock.isChainStartPending()) return;

        if (lock.isWithinStartGracePeriod(currentTick)) {
            EliteMobsLogger.debug(LOGGER,
                                  "[AbilityCompletion] GRACE skip: ability=%s startedAt=%d currentTick=%d",
                                  EliteMobsLogLevel.INFO,
                                  lock.activeAbilityId,
                                  lock.chainStartedAtTick,
                                  currentTick
            );
            return;
        }

        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);

        boolean chainRunning = AbilityHelpers.isInteractionTypeRunning(store, entityRef, ABILITY_INTERACTION_TYPE);
        if (chainRunning) return;

        String completedAbilityId = lock.activeAbilityId;

        EliteMobsTierComponent tier = store.getComponent(entityRef, plugin.getEliteMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        EliteMobsLogger.debug(LOGGER,
                              "[AbilityCompletion] DETECTED: ability=%s tier=%d pending=%b startedAt=%d currentTick=%d chainRunning=%b",
                              EliteMobsLogLevel.INFO,
                              completedAbilityId,
                              tierIndex,
                              lock.chainStartPending,
                              lock.chainStartedAtTick,
                              currentTick,
                              chainRunning
        );

        EliteMobAbilityCompletedEvent completedEvent = new EliteMobAbilityCompletedEvent(
                entityRef, completedAbilityId, tierIndex
        );
        plugin.getEventBus().fire(completedEvent);

        EliteMobsLogger.debug(LOGGER,
                "[AbilityCompletion] %s completed tier=%d",
                EliteMobsLogLevel.INFO, completedAbilityId, tierIndex);
    }
}
