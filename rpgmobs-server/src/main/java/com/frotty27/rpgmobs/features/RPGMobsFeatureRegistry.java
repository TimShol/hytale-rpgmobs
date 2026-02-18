package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.exceptions.FeatureRegistrationException;
import com.frotty27.rpgmobs.exceptions.FeatureResolutionException;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RPGMobsFeatureRegistry {

    private static RPGMobsFeatureRegistry instance;

    private final List<IRPGMobsFeature> orderedFeatures = new ArrayList<>();
    private final Map<String, IRPGMobsFeature> featuresByKey = new HashMap<>();
    private final Map<String, IRPGMobsFeature> featuresByAssetId = new HashMap<>();

    public RPGMobsFeatureRegistry(RPGMobsPlugin plugin) {
        instance = this;
        register(new RPGMobsSpawningFeature());
        register(new RPGMobsDamageFeature());
        register(new RPGMobsAbilityCoreFeature());
        register(new RPGMobsDropsFeature());
        register(new RPGMobsNameplateFeature());
        register(new RPGMobsEffectsFeature());
        register(new RPGMobsProjectileResistanceEffectFeature(plugin));
        register(new RPGMobsChargeLeapAbilityFeature());
        register(new RPGMobsHealLeapAbilityFeature());
        register(new RPGMobsUndeadSummonAbilityFeature());
        register(new RPGMobsHealthScalingFeature());
        register(new RPGMobsModelScalingFeature());
    }

    public static RPGMobsFeatureRegistry getInstance() {
        return instance;
    }

    public void register(IRPGMobsFeature feature) {
        if (feature == null) return;

        if (featuresByKey.containsKey(feature.getFeatureKey())) {
            throw new FeatureRegistrationException("Duplicate feature key: " + feature.getFeatureKey());
        }

        orderedFeatures.add(feature);
        featuresByKey.put(feature.getFeatureKey(), feature);

        String assetId = feature.getAssetId();
        if (assetId != null) {
            if (featuresByAssetId.containsKey(assetId)) {
                throw new FeatureRegistrationException("Duplicate feature asset ID: " + assetId);
            }
            featuresByAssetId.put(assetId, feature);
        }
    }

    public IRPGMobsFeature getFeature(String key) {
        IRPGMobsFeature feature = featuresByKey.get(key);
        if (feature == null) {
            throw new FeatureResolutionException("Key: " + key);
        }
        return feature;
    }

    public IRPGMobsFeature getFeatureByAssetId(String assetId) {
        IRPGMobsFeature feature = featuresByAssetId.get(assetId);
        if (feature == null) {
            throw new FeatureResolutionException("AssetID: " + assetId);
        }
        return feature;
    }

    public Map<String, IRPGMobsFeature> getFeaturesByKey() {
        return featuresByKey;
    }

    public void applyAll(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                         Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                         RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        for (IRPGMobsFeature feature : orderedFeatures) {
            feature.apply(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
        }
    }

    public void reconcileAll(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                             Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                             RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        for (IRPGMobsFeature feature : orderedFeatures) {
            feature.reconcile(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
        }
    }

    public void registerSystems(RPGMobsPlugin plugin) {
        for (IRPGMobsFeature feature : orderedFeatures) {
            feature.registerSystems(plugin);
        }
    }

    public void onDamageAll(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> victimRef,
                            Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                            RPGMobsTierComponent tierComponent, @Nullable NPCEntity npcEntity, int tierIndex,
                            long currentTick, Damage damage) {
        for (IRPGMobsFeature feature : orderedFeatures) {
            feature.onDamage(plugin,
                             config,
                             victimRef,
                             entityStore,
                             commandBuffer,
                             tierComponent,
                             npcEntity,
                             tierIndex,
                             currentTick,
                             damage
            );
        }
    }
}
