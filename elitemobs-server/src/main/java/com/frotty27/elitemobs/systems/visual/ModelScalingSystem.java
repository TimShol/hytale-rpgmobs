package com.frotty27.elitemobs.systems.visual;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsModelScalingComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.Random;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampFloat;

public class ModelScalingSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float MODEL_SCALE_MIN = 0.5f;
    private static final float MODEL_SCALE_MAX = 2.0f;
    private final Random random = new Random();
    private final EliteMobsPlugin plugin;

    public ModelScalingSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType(), plugin.getEliteMobsComponentType());
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        EliteMobsConfig config = plugin.getConfig();
        if (config == null || !config.modelConfig.enableModelScaling) return;

        if (!plugin.shouldReconcileThisTick()) return;

        Ref<EntityStore> npcRef = chunk.getReferenceTo(entityIndex);
        EliteMobsModelScalingComponent modelComp = store.getComponent(npcRef, plugin.getModelScalingComponentType());
        if (modelComp == null || !modelComp.scaledApplied || modelComp.appliedScale <= 0.001f) return;

        if (modelComp.resyncVerified) return;

        tryScaleModelComponent(npcRef, store, commandBuffer, modelComp.appliedScale, false);
        modelComp.resyncVerified = true;
        commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelComp);
    }


    private float computeModelScaleMultiplier(EliteMobsConfig config, int tierIndex) {
        float baseMultiplier =
                (config.modelConfig.modelScaleMultiplierPerTier != null && config.modelConfig.modelScaleMultiplierPerTier.length > tierIndex)
                        ? config.modelConfig.modelScaleMultiplierPerTier[tierIndex]
                        : 1.0f;

        float variance = Math.max(0f, config.modelConfig.modelScaleRandomVariance);
        float randomizedMultiplier = baseMultiplier + ((random.nextFloat() * 2f - 1f) * variance);

        return clampFloat(randomizedMultiplier, MODEL_SCALE_MIN, MODEL_SCALE_MAX);
    }

    private boolean tryScaleModelComponent(
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            float scaleMultiplier,
            boolean log
    ) {
        ModelComponent modelComponent = entityStore.getComponent(npcRef, ModelComponent.getComponentType());
        if (modelComponent == null) return false;

        Model currentModel = modelComponent.getModel();
        if (currentModel == null) return false;

        String modelAssetId = currentModel.getModelAssetId();
        if (modelAssetId == null || modelAssetId.isBlank()) return false;

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        if (modelAsset == null) {
            if (log) {
                EliteMobsLogger.debug(
                        LOGGER,
                        "ModelAsset not found id=%s (cannot scale)",
                        EliteMobsLogLevel.WARNING,
                        modelAssetId
                );
            }
            return false;
        }

        Model scaledModel;
        try {
            scaledModel = Model.createScaledModel(
                    modelAsset,
                    scaleMultiplier,
                    currentModel.getRandomAttachmentIds(),
                    currentModel.getBoundingBox()
            );
        } catch (Throwable ignored) {
            scaledModel = Model.createStaticScaledModel(modelAsset, scaleMultiplier);
        }

        commandBuffer.replaceComponent(npcRef, ModelComponent.getComponentType(), new ModelComponent(scaledModel));

        PersistentModel persistentModel = entityStore.getComponent(npcRef, PersistentModel.getComponentType());
        if (persistentModel != null) {
            persistentModel.setModelReference(scaledModel.toReference());
            commandBuffer.replaceComponent(npcRef, PersistentModel.getComponentType(), persistentModel);
        }

        if (log) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "Scaled model asset=%s scale=%.3f",
                    EliteMobsLogLevel.INFO,
                    modelAssetId,
                    scaleMultiplier
            );
        }

        return true;
    }


    public void applyModelScalingOnSpawn(Ref<EntityStore> npcRef, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        EliteMobsConfig config = plugin.getConfig();
        if (config == null || !config.modelConfig.enableModelScaling) return;

        EliteMobsTierComponent tierComponent = store.getComponent(npcRef, plugin.getEliteMobsComponentType());
        EliteMobsModelScalingComponent modelScalingComponent = store.getComponent(npcRef,
                                                                                  plugin.getModelScalingComponentType()
        );

        if (tierComponent == null) return;

        if (modelScalingComponent != null && modelScalingComponent.scaledApplied) return;

        int tierIndex = tierComponent.tierIndex;


        float scaleMultiplier = computeModelScaleMultiplier(config, tierIndex);


        boolean scaled = tryScaleModelComponent(
                npcRef,
                store,
                commandBuffer,
                scaleMultiplier,
                config.debugConfig.isDebugModeEnabled
        );

        if (!scaled) return;


        if (modelScalingComponent != null) {
            modelScalingComponent.scaledApplied = true;
            modelScalingComponent.appliedScale = scaleMultiplier;
            commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelScalingComponent);
        }
    }
}
