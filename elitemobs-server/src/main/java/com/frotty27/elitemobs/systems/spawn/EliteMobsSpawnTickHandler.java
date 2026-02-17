package com.frotty27.elitemobs.systems.spawn;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

final class EliteMobsSpawnTickHandler {

    private final EliteMobsSpawnSystem system;

    EliteMobsSpawnTickHandler(EliteMobsSpawnSystem system) {
        this.system = system;
    }

    void handle(
            float deltaTimeSeconds,
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        system.processTick(entityIndex, archetypeChunk, entityStore, commandBuffer);
    }
}
