package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsModelScalingComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.visual.ModelScalingSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsModelScalingFeature implements IEliteMobsFeature {

    @Override
    public String getFeatureKey() {
        return "ModelScaling";
    }

    @Override
    public Object getConfig(EliteMobsConfig config) {
        return config.modelConfig;
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
        if (config.modelConfig.enableMobModelScaling) {
            EliteMobsModelScalingComponent modelScaling = new EliteMobsModelScalingComponent();
            commandBuffer.putComponent(npcRef, plugin.getModelScalingComponentType(), modelScaling);
        }
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        ModelScalingSystem system = new ModelScalingSystem(plugin);
        plugin.registerSystem(system);
        plugin.getEventBus().registerListener(system);
    }
}
