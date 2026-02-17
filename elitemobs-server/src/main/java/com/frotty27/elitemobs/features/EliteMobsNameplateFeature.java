package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsNameplateFeature implements IEliteMobsFeature {

    @Override
    public String getFeatureKey() {
        return "Nameplate";
    }

    @Override
    public Object getConfig(EliteMobsConfig config) {
        return config.nameplatesConfig;
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
        applyNameplate(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
    }

    @Override
    public void reconcile(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        applyNameplate(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
    }

    private void applyNameplate(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        if (roleName == null || roleName.isBlank()) {
            if (config != null && config.nameplatesConfig.enableMobNameplates) {
                plugin.getNameplateService().applyOrUpdateNameplate(
                        config, npcRef, entityStore, commandBuffer, "", tierComponent.tierIndex);
            }
            return;
        }
        plugin.getNameplateService().applyOrUpdateNameplate(
                config, npcRef, entityStore, commandBuffer, roleName, tierComponent.tierIndex);
    }
}
