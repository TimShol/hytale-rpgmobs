package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.visual.HealthScalingSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsHealthScalingFeature implements IEliteMobsFeature {

    @Override
    public String getFeatureKey() {
        return "HealthScaling";
    }

    @Override
    public Object getConfig(EliteMobsConfig config) {
        return config.healthConfig;
    }

    @Override
    public void apply(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        HealthScalingSystem system = new HealthScalingSystem(plugin, this);
        plugin.registerSystem(system);
        plugin.setHealthScalingSystem(system); 
    }
}
