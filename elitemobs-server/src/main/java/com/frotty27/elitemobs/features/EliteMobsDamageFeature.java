package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.combat.EliteMobsDamageDealtSystem;
import com.frotty27.elitemobs.systems.combat.EliteMobsFriendlyFireSystem;
import com.frotty27.elitemobs.systems.death.EliteMobsDeathSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsDamageFeature implements IEliteMobsFeature {

    @Override
    public String getFeatureKey() {
        return "Damage";
    }

    @Override
    public Object getConfig(EliteMobsConfig config) {
        return config.damageConfig;
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
        plugin.registerSystem(new EliteMobsDeathSystem(plugin));
        plugin.registerSystem(new EliteMobsDamageDealtSystem(plugin));
        plugin.registerSystem(new EliteMobsFriendlyFireSystem(plugin));
    }
}
