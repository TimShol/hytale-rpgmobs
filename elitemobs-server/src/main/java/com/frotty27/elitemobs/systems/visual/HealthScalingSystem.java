package com.frotty27.elitemobs.systems.visual;

import com.frotty27.elitemobs.api.IEliteMobsEventListener;
import com.frotty27.elitemobs.api.events.EliteMobScalingAppliedEvent;
import com.frotty27.elitemobs.api.events.EliteMobSpawnedEvent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsHealthScalingComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsModelScalingComponent;
import com.frotty27.elitemobs.components.progression.EliteMobsProgressionComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.features.EliteMobsHealthScalingFeature;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.utils.StoreHelpers;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.Random;

import static com.frotty27.elitemobs.utils.Constants.NPC_COMPONENT_TYPE;

public class HealthScalingSystem extends EntityTickingSystem<EntityStore> implements IEliteMobsEventListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float HEALTH_MAX_EPSILON = 0.05f;
    private static final int HEALTH_FINALIZE_MAX_TRIES = 5;

    private final EliteMobsPlugin plugin;
    private final EliteMobsHealthScalingFeature feature;
    private final Random random = new Random();

    public HealthScalingSystem(EliteMobsPlugin plugin, EliteMobsHealthScalingFeature feature) {
        this.plugin = plugin;
        this.feature = feature;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPC_COMPONENT_TYPE, plugin.getEliteMobsComponentType());
    }


    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        EliteMobsConfig config = plugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> npcRef = chunk.getReferenceTo(entityIndex);

        EliteMobsHealthScalingComponent healthComp = store.getComponent(npcRef, plugin.getHealthScalingComponentType());
        if (healthComp == null || !config.healthConfig.enableMobHealthScaling || !healthComp.healthApplied) return;

        if (!healthComp.healthFinalized && healthComp.shouldRetryHealthFinalization()) {
            verifyHealthScalingFreshSpawn(npcRef, store, commandBuffer, healthComp);
            return;
        }

        if (!plugin.shouldReconcileThisTick()) return;

        if (healthComp.healthFinalized && !healthComp.resyncDone) {
            resyncAfterRestart(npcRef, store, commandBuffer, healthComp);
            return;
        }

        if (healthComp.healthFinalized) {
            reconcileConfigChange(npcRef, store, commandBuffer, healthComp, config);
        }
    }

    private void resyncAfterRestart(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                     CommandBuffer<EntityStore> commandBuffer,
                                     EliteMobsHealthScalingComponent healthComp) {
        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return;

        float distanceHealthBonus = 0f;
        EliteMobsProgressionComponent prog = store.getComponent(npcRef, plugin.getProgressionComponentType());
        if (prog != null) {
            distanceHealthBonus = prog.distanceHealthBonus();
        }

        float totalMultiplier = healthComp.appliedHealthMult + distanceHealthBonus;
        float actualMax = healthStatValue.getMax();

        EliteMobsLogger.debug(LOGGER,
                              "[HealthScaling] Post-restart resync: actualMax=%.1f baseMax=%.1f totalMult=%.2f",
                              EliteMobsLogLevel.INFO,
                              actualMax,
                              healthComp.baseHealthMax,
                              totalMultiplier
        );

        if (actualMax <= healthComp.baseHealthMax + HEALTH_MAX_EPSILON) {
            entityStats.putModifier(
                    healthStatId,
                    feature.getFeatureKey(),
                    new StaticModifier(
                            Modifier.ModifierTarget.MAX,
                            StaticModifier.CalculationType.MULTIPLICATIVE,
                            Math.max(0.01f, totalMultiplier)
                    )
            );

            EliteMobsLogger.debug(LOGGER,
                                  "[HealthScaling] Resync: registered modifier (mult=%.2f), engine will apply on flush",
                                  EliteMobsLogLevel.INFO,
                                  totalMultiplier
            );

            commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);
        }

        healthComp.resyncDone = true;
        healthComp.healthFinalized = false;
        healthComp.healthFinalizeTries = 0;
        commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);
    }

    private void reconcileConfigChange(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                        CommandBuffer<EntityStore> commandBuffer,
                                        EliteMobsHealthScalingComponent healthComp,
                                        EliteMobsConfig config) {
        EliteMobsTierComponent tierComp = store.getComponent(npcRef, plugin.getEliteMobsComponentType());
        if (tierComp == null) return;

        int tierIndex = tierComp.tierIndex;
        float configHealthMult = 1.0f;
        if (config.healthConfig.mobHealthMultiplierPerTier != null && config.healthConfig.mobHealthMultiplierPerTier.length > tierIndex) {
            configHealthMult = config.healthConfig.mobHealthMultiplierPerTier[tierIndex];
        }

        if (Math.abs(configHealthMult - healthComp.appliedHealthMult) <= 0.001f) {
            return;
        }

        EliteMobsLogger.debug(LOGGER,
                              "[HealthScaling] Config reconcile: tier=%d oldMult=%.2f newMult=%.2f",
                              EliteMobsLogLevel.INFO,
                              tierIndex,
                              healthComp.appliedHealthMult,
                              configHealthMult
        );

        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();

        float distanceHealthBonus = 0f;
        EliteMobsProgressionComponent prog = store.getComponent(npcRef, plugin.getProgressionComponentType());
        if (prog != null) {
            distanceHealthBonus = prog.distanceHealthBonus();
        }

        float totalMultiplier = configHealthMult + distanceHealthBonus;

        entityStats.putModifier(
                healthStatId,
                feature.getFeatureKey(),
                new StaticModifier(
                        Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        Math.max(0.01f, totalMultiplier)
                )
        );

        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

        healthComp.appliedHealthMult = configHealthMult;
        healthComp.healthFinalized = false;
        healthComp.healthFinalizeTries = 0;
        commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);

        EliteMobsLogger.debug(LOGGER,
                              "[HealthScaling] Config reconcile: registered new modifier (mult=%.2f), entering VERIFY",
                              EliteMobsLogLevel.INFO,
                              totalMultiplier
        );
    }



    private void applyHealthModifier(
            Ref<EntityStore> npcRef,
            CommandBuffer<EntityStore> commandBuffer,
            EntityStatMap entityStats,
            int healthStatId,
            float totalMultiplier
    ) {
        var before = entityStats.get(healthStatId);
        if (before == null) return;

        float maxHealthBefore = before.getMax();

        entityStats.putModifier(
                healthStatId,
                feature.getFeatureKey(),
                new StaticModifier(
                        Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        Math.max(0.01f, totalMultiplier)
                )
        );

        EliteMobsLogger.debug(LOGGER,
                              "[HealthScaling] Registered modifier: mult=%.2f beforeMax=%.1f",
                              EliteMobsLogLevel.INFO,
                              totalMultiplier,
                              maxHealthBefore
        );

        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);
    }


    public void applyHealthScalingOnSpawn(Ref<EntityStore> npcRef, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        EliteMobsConfig config = plugin.getConfig();
        if (config == null || !config.healthConfig.enableMobHealthScaling) return;

        EliteMobsTierComponent tierComponent = store.getComponent(npcRef, plugin.getEliteMobsComponentType());
        EliteMobsHealthScalingComponent healthScalingComponent = store.getComponent(npcRef,
                                                                                    plugin.getHealthScalingComponentType()
        );

        if (tierComponent == null) return;

        if (healthScalingComponent != null && healthScalingComponent.healthApplied) return;


        int tierIndex = tierComponent.tierIndex;
        float tierHealthMult = 1.0f;
        if (config.healthConfig.mobHealthMultiplierPerTier != null && config.healthConfig.mobHealthMultiplierPerTier.length > tierIndex) {
            tierHealthMult = config.healthConfig.mobHealthMultiplierPerTier[tierIndex];
        }


        float distanceHealthBonus = 0f;
        EliteMobsProgressionComponent progressionComponent = store.getComponent(npcRef,
                                                                                plugin.getProgressionComponentType()
        );
        if (progressionComponent != null) {
            distanceHealthBonus = progressionComponent.distanceHealthBonus();
        }

        float healthRandomVariance = config.healthConfig.mobHealthRandomVariance;
        if (healthRandomVariance > 0f) {
            tierHealthMult += (random.nextFloat() * 2f - 1f) * healthRandomVariance;
        }

        float totalMultiplier = tierHealthMult + distanceHealthBonus;


        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return;

        float baseHealthMax = healthStatValue.getMax();

        EliteMobsLogger.debug(LOGGER,
                              "[HealthScaling] BEFORE spawn scaling: baseMax=%.1f tierMult=%.2f distBonus=%.2f totalMult=%.2f tier=%d",
                              EliteMobsLogLevel.INFO,
                              baseHealthMax,
                              tierHealthMult,
                              distanceHealthBonus,
                              totalMultiplier,
                              tierIndex
        );

        applyHealthModifier(npcRef, commandBuffer, entityStats, healthStatId, totalMultiplier);


        if (healthScalingComponent != null) {
            healthScalingComponent.healthApplied = true;
            healthScalingComponent.appliedHealthMult = tierHealthMult;
            healthScalingComponent.baseHealthMax = baseHealthMax;
            healthScalingComponent.healthFinalized = false;
            healthScalingComponent.healthFinalizeTries = 0;
            healthScalingComponent.resyncDone = true;
            commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthScalingComponent);

            EliteMobsLogger.debug(LOGGER,
                                  "[HealthScaling] Component stored: healthApplied=true baseMax=%.1f appliedMult=%.2f",
                                  EliteMobsLogLevel.INFO,
                                  healthScalingComponent.baseHealthMax,
                                  healthScalingComponent.appliedHealthMult
            );
        }

        float modelScale = 1.0f;
        EliteMobsModelScalingComponent modelComp = store.getComponent(npcRef, plugin.getModelScalingComponentType());
        if (modelComp != null && modelComp.scaledApplied) {
            modelScale = modelComp.appliedScale;
        }

        float damageMultiplier = 1.0f + (tierIndex * 0.5f);
        EliteMobsProgressionComponent prog = store.getComponent(npcRef, plugin.getProgressionComponentType());
        if (prog != null) {
            damageMultiplier += prog.distanceDamageBonus();
        }

        var finalHealthStat = store.getComponent(npcRef, EntityStatMap.getComponentType());
        float finalHealth = baseHealthMax * totalMultiplier;
        if (finalHealthStat != null) {
            var hv = finalHealthStat.get(healthStatId);
            if (hv != null) finalHealth = hv.getMax();
        }

        plugin.getEventBus().fire(new EliteMobScalingAppliedEvent(
                npcRef, tierIndex, tierHealthMult, damageMultiplier,
                modelScale, baseHealthMax, finalHealth, false
        ));
    }


    private void verifyHealthScalingFreshSpawn(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                                CommandBuffer<EntityStore> commandBuffer,
                                                EliteMobsHealthScalingComponent healthComp) {
        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return;

        float actualMax = healthStatValue.getMax();
        float actualCurrent = healthStatValue.get();
        float baseMax = healthComp.baseHealthMax;

        EliteMobsLogger.debug(LOGGER,
                              "[HealthScaling] VERIFY tick: actualCurrent=%.1f actualMax=%.1f baseMax=%.1f tries=%d",
                              EliteMobsLogLevel.INFO,
                              actualCurrent,
                              actualMax,
                              baseMax,
                              healthComp.healthFinalizeTries
        );

        boolean modifierApplied = Math.abs(actualMax - baseMax) > 1.0f;

        if (modifierApplied) {
            if (actualCurrent < (actualMax - HEALTH_MAX_EPSILON)) {
                entityStats.maximizeStatValue(healthStatId);
                entityStats.update();
                commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

                EliteMobsLogger.debug(LOGGER,
                                      "[HealthScaling] VERIFY top-off: current was %.1f, maximized to max=%.1f",
                                      EliteMobsLogLevel.INFO,
                                      actualCurrent,
                                      actualMax
                );
            }

            healthComp.healthFinalized = true;
            healthComp.healthFinalizeTries = HEALTH_FINALIZE_MAX_TRIES;
            commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);

            EliteMobsLogger.debug(LOGGER,
                                  "[HealthScaling] VERIFY finalized: actualMax=%.1f (base was %.1f)",
                                  EliteMobsLogLevel.INFO,
                                  actualMax,
                                  baseMax
            );
        } else {
            healthComp.incrementFinalizeTries();

            if (healthComp.healthFinalizeTries >= HEALTH_FINALIZE_MAX_TRIES) {
                if (actualCurrent < (actualMax - HEALTH_MAX_EPSILON)) {
                    entityStats.maximizeStatValue(healthStatId);
                    entityStats.update();
                    commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

                    EliteMobsLogger.debug(LOGGER,
                                          "[HealthScaling] VERIFY exhausted top-off: current was %.1f, maximized to max=%.1f",
                                          EliteMobsLogLevel.INFO,
                                          actualCurrent,
                                          actualMax
                    );
                }

                healthComp.healthFinalized = true;

                EliteMobsLogger.debug(LOGGER,
                                      "[HealthScaling] VERIFY exhausted: accepting actualMax=%.1f as final (base was %.1f). Modifier may not have applied.",
                                      EliteMobsLogLevel.WARNING,
                                      actualMax,
                                      baseMax
                );
            }

            commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);
        }
    }

    @Override
    public void onEliteMobSpawned(EliteMobSpawnedEvent event) {
        if (event.isCancelled()) return;

        Ref<EntityStore> npcRef = event.getEntityRef();
        Store<EntityStore> store = npcRef.getStore();

        NPCEntity npcEntity = store.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npcEntity == null || npcEntity.getWorld() == null) return;

        npcEntity.getWorld().execute(() -> {
            EntityStore entityStoreProvider = npcEntity.getWorld().getEntityStore();
            if (entityStoreProvider == null) return;
            Store<EntityStore> entityStore = entityStoreProvider.getStore();

            StoreHelpers.withEntity(entityStore,
                                    npcRef,
                                    (_, commandBuffer, _) -> applyHealthScalingOnSpawn(npcRef,
                                                                                       entityStore,
                                                                                       commandBuffer
                                    )
            );
        });
    }
}
