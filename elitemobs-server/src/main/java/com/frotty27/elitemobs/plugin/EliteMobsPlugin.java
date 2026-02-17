package com.frotty27.elitemobs.plugin;

import com.frotty27.elitemobs.api.EliteMobsAPI;
import com.frotty27.elitemobs.api.EliteMobsEventBus;
import com.frotty27.elitemobs.api.IEliteMobsEventListener;
import com.frotty27.elitemobs.api.events.EliteMobReconcileEvent;
import com.frotty27.elitemobs.api.events.EliteMobSpawnedEvent;
import com.frotty27.elitemobs.api.query.EliteMobsQueryAPI;
import com.frotty27.elitemobs.assets.EliteMobsAssetGenerator;
import com.frotty27.elitemobs.assets.EliteMobsAssetRetriever;
import com.frotty27.elitemobs.commands.EliteMobsRootCommand;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.elitemobs.components.ability.EliteMobsAbilityLockComponent;
import com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.elitemobs.components.combat.EliteMobsCombatTrackingComponent;
import com.frotty27.elitemobs.components.effects.EliteMobsActiveEffectsComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsHealthScalingComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsMigrationComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsModelScalingComponent;
import com.frotty27.elitemobs.components.progression.EliteMobsProgressionComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonMinionTrackingComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonRiseComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonedMinionComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.config.schema.YamlSerializer;
import com.frotty27.elitemobs.features.EliteMobsFeatureRegistry;
import com.frotty27.elitemobs.features.EliteMobsSpawningFeature;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.nameplates.EliteMobsNameplateService;
import com.frotty27.elitemobs.systems.ability.EliteMobsAbilityTriggerListener;
import com.frotty27.elitemobs.systems.combat.EliteMobsAITargetPollingSystem;
import com.frotty27.elitemobs.systems.combat.EliteMobsCombatStateSystem;
import com.frotty27.elitemobs.systems.death.EliteMobsVanillaDropsCullZoneManager;
import com.frotty27.elitemobs.systems.drops.EliteMobsExtraDropsScheduler;
import com.frotty27.elitemobs.systems.migration.EliteMobsComponentMigrationSystem;
import com.frotty27.elitemobs.systems.spawn.EliteMobsSpawnSystem;
import com.frotty27.elitemobs.systems.visual.HealthScalingSystem;
import com.frotty27.elitemobs.systems.visual.ModelScalingSystem;
import com.frotty27.elitemobs.utils.StoreHelpers;
import com.frotty27.elitemobs.utils.TickClock;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetRegistryLoader;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class EliteMobsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ASSET_PACK_NAME = "EliteMobsGenerated";
    private EliteMobsConfig config;

    private ComponentType<EntityStore, EliteMobsTierComponent> eliteMobsComponentType;
    private ComponentType<EntityStore, EliteMobsProgressionComponent> progressionComponentType;
    private ComponentType<EntityStore, EliteMobsHealthScalingComponent> healthScalingComponentType;
    private ComponentType<EntityStore, EliteMobsModelScalingComponent> modelScalingComponentType;
    private ComponentType<EntityStore, EliteMobsActiveEffectsComponent> activeEffectsComponentType;
    private ComponentType<EntityStore, EliteMobsCombatTrackingComponent> combatTrackingComponentType;
    private ComponentType<EntityStore, EliteMobsMigrationComponent> migrationComponentType;
    private ComponentType<EntityStore, EliteMobsSummonedMinionComponent> summonedMinionComponentType;
    private ComponentType<EntityStore, EliteMobsSummonMinionTrackingComponent> summonMinionTrackingComponentType;
    private ComponentType<EntityStore, EliteMobsSummonRiseComponent> summonRiseComponentType;
    private ComponentType<EntityStore, ChargeLeapAbilityComponent> chargeLeapAbilityComponentType;
    private ComponentType<EntityStore, HealLeapAbilityComponent> healLeapAbilityComponentType;
    private ComponentType<EntityStore, SummonUndeadAbilityComponent> summonUndeadAbilityComponentType;
    private ComponentType<EntityStore, EliteMobsAbilityLockComponent> abilityLockComponentType;

    private final TickClock tickClock = new TickClock();
    private final EliteMobsVanillaDropsCullZoneManager cullZoneManager = new EliteMobsVanillaDropsCullZoneManager(
            tickClock);
    private final EliteMobsExtraDropsScheduler dropsScheduler = new EliteMobsExtraDropsScheduler(tickClock);
    private final EliteMobsNameplateService nameplateService = new EliteMobsNameplateService();
    private final EliteMobsAssetRetriever eliteMobsAssetRetriever = new EliteMobsAssetRetriever(this);
    private final EliteMobsFeatureRegistry featureRegistry = new EliteMobsFeatureRegistry(this);
    private final EliteMobsEventBus eventBus = new EliteMobsEventBus();

    private HealthScalingSystem healthScalingSystem;
    private ModelScalingSystem modelScalingSystem;
    private EliteMobsAbilityTriggerListener abilityTriggerListener;

    private final AtomicBoolean reconcileRequested = new AtomicBoolean(false);
    private final AtomicInteger reconcileTicksRemaining = new AtomicInteger(0);
    private boolean reconcileActive = false;

    public EliteMobsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        EliteMobsAPI.setEventBus(eventBus);
        EliteMobsAPI.setQueryAPI(new EliteMobsQueryAPI(this));

        loadOrCreateEliteMobsConfig();

        getEventRegistry().register(LoadAssetEvent.class, this::onLoadAssets);
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);

        nameplateService.describeSegments(this);

        registerComponents();

        abilityTriggerListener = new EliteMobsAbilityTriggerListener(this);

        registerSystems();
        registerCommands();
        registerEventListeners();

        LOGGER.atInfo().log("Setup complete!");
    }

    private void registerEventListeners() {
        eventBus.registerListener(new IEliteMobsEventListener() {
            @Override
            public void onEliteMobSpawned(EliteMobSpawnedEvent event) {
                if (event.isCancelled()) {
                    LOGGER.atInfo().log("[SpawnEvent] Spawn event cancelled, skipping health scaling");
                    return;
                }

                LOGGER.atInfo().log("[SpawnEvent] Received spawn event tier=%d healthSystem=%b modelSystem=%b",
                        event.getTier(), healthScalingSystem != null, modelScalingSystem != null);

                if (healthScalingSystem != null) {
                    Ref<EntityStore> npcRef = event.getEntityRef();
                    Store<EntityStore> store = npcRef.getStore();

                    NPCEntity npcEntity = store.getComponent(npcRef,
                                                             Objects.requireNonNull(NPCEntity.getComponentType())
                    );
                    LOGGER.atInfo().log("[SpawnEvent] npcEntity=%b world=%b",
                            npcEntity != null, npcEntity != null && npcEntity.getWorld() != null);

                    if (npcEntity != null && npcEntity.getWorld() != null) {
                        npcEntity.getWorld().execute(() -> {
                            LOGGER.atInfo().log("[SpawnEvent] Deferred callback executing");
                            EntityStore entityStoreProvider = npcEntity.getWorld().getEntityStore();
                            if (entityStoreProvider == null) {
                                LOGGER.atInfo().log("[SpawnEvent] entityStoreProvider is null!");
                                return;
                            }
                            Store<EntityStore> entityStore = entityStoreProvider.getStore();

                            StoreHelpers.withEntity(entityStore, npcRef, (_, commandBuffer, _) -> {
                                LOGGER.atInfo().log("[SpawnEvent] Inside withEntity - applying scaling");

                                if (healthScalingSystem != null) {
                                    healthScalingSystem.applyHealthScalingOnSpawn(npcRef, entityStore, commandBuffer);
                                }

                                if (modelScalingSystem != null) {
                                    modelScalingSystem.applyModelScalingOnSpawn(npcRef, entityStore, commandBuffer);
                                }
                            });
                        });
                    }
                }
            }
        });

        LOGGER.atInfo().log("Registered event listeners for event-driven scaling.");

        eventBus.registerListener(abilityTriggerListener);
        LOGGER.atInfo().log("Registered EliteMobsAbilityTriggerListener for event-driven ability triggers.");
    }

    private void onLoadAssets(LoadAssetEvent event) {
        try {
            loadOrCreateEliteMobsConfig();
            Path eliteMobsDirectory = getModDirectory();
            EliteMobsAssetGenerator.generateAll(eliteMobsDirectory, config, true);

            AssetPack assetPack = new AssetPack(eliteMobsDirectory,
                                           ASSET_PACK_NAME,
                                           eliteMobsDirectory,
                                           FileSystems.getDefault(),
                                           false,
                                           getManifest()
            );

            AssetRegistryLoader.loadAssets(event, assetPack);

            LOGGER.atInfo().log("Loaded generated AssetPack '%s' from: %s",
                                ASSET_PACK_NAME,
                                eliteMobsDirectory.toAbsolutePath()
            );

            reloadNpcRoleAssetsIfPossible();
        } catch (Throwable error) {
            LOGGER.atWarning().log("onLoadAssets failed: %s", error.toString());
            error.printStackTrace();
        }
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
    }

    public void reloadConfigAndAssets() {
        loadOrCreateEliteMobsConfig();

        EliteMobsConfig cfg = getConfig();
        if (cfg == null) throw new IllegalStateException("EliteMobsConfig is null after force reload!");

        Path eliteMobsDir = getModDirectory();
        EliteMobsAssetGenerator.generateAll(eliteMobsDir, cfg, true);

        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            LOGGER.atWarning().log("[EliteMobs] AssetModule is null; cannot force reload asset pack.");
            return;
        }

        try {
            assetModule.registerPack(ASSET_PACK_NAME, eliteMobsDir, getManifest(), true);
            LOGGER.atInfo().log("[EliteMobs] Reloaded config & regenerated assets successfully!");
            reloadNpcRoleAssetsIfPossible();
        } catch (Throwable error) {
            LOGGER.atWarning().log("[EliteMobs] Failed to reload: %s", error.toString());
            error.printStackTrace();
        }
    }

    private void reloadNpcRoleAssetsIfPossible() {
    }

    public synchronized void loadOrCreateEliteMobsConfig() {
        Path modDirectory = getModDirectory();

        String oldVersion = YamlSerializer.readConfigVersion(modDirectory, "core.yml", "configVersion");

        EliteMobsConfig defaults = new EliteMobsConfig();
        try {
            Object versionObj = getManifest().getVersion();
            if (versionObj != null) defaults.configVersion = versionObj.toString();
        } catch (Throwable ignored) {}

        config = YamlSerializer.loadOrCreate(modDirectory, defaults);

        if (config != null) {
            config.migrate(oldVersion);

            config.populateSummonMarkerEntriesIfEmpty();
            config.populateSummonMarkerEntriesByRoleIfEmpty();
            config.upgradeSummonMarkerEntriesToVariantIds();
            if (config.isSummonMarkerEntriesEmpty()) {
                LOGGER.atWarning().log("[EliteMobs] Undead summon is enabled but no bow NPCs were found in mob rules.");
            }
        }

        EliteMobsLogger.init(config);
        requestReconcileOnNextWorldTick();

        LOGGER.atInfo().log("Config loaded/reloaded from: %s", modDirectory.toAbsolutePath());
    }

    private Path getModDirectory() {
        return getDataDirectory().getParent().resolve("EliteMobs");
    }

    private void registerComponents() {

        eliteMobsComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsTierComponent.class,
                "EliteMobsTierComponent",
                EliteMobsTierComponent.CODEC
        );
        LOGGER.atInfo().log("[1/14] Registered EliteMobsTierComponent");

        progressionComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsProgressionComponent.class,
                "EliteMobsProgressionComponent",
                EliteMobsProgressionComponent.CODEC
        );
        LOGGER.atInfo().log("[2/14] Registered EliteMobsProgressionComponent");

        healthScalingComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsHealthScalingComponent.class,
                "EliteMobsHealthScalingComponent",
                EliteMobsHealthScalingComponent.CODEC
        );
        LOGGER.atInfo().log("[3/14] Registered EliteMobsHealthScalingComponent");

        modelScalingComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsModelScalingComponent.class,
                "EliteMobsModelScalingComponent",
                EliteMobsModelScalingComponent.CODEC
        );
        LOGGER.atInfo().log("[4/14] Registered EliteMobsModelScalingComponent");

        activeEffectsComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsActiveEffectsComponent.class,
                "EliteMobsActiveEffectsComponent",
                EliteMobsActiveEffectsComponent.CODEC
        );
        LOGGER.atInfo().log("[5/14] Registered EliteMobsActiveEffectsComponent");

        combatTrackingComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsCombatTrackingComponent.class,
                "EliteMobsCombatTrackingComponent",
                EliteMobsCombatTrackingComponent.CODEC
        );
        LOGGER.atInfo().log("[6/14] Registered EliteMobsCombatTrackingComponent (with marker-based aggro)");

        migrationComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsMigrationComponent.class,
                "EliteMobsMigrationComponent",
                EliteMobsMigrationComponent.CODEC
        );
        LOGGER.atInfo().log("[7/14] Registered EliteMobsMigrationComponent (temporary)");

        summonedMinionComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsSummonedMinionComponent.class,
                "EliteMobsSummonedMinionComponent",
                EliteMobsSummonedMinionComponent.CODEC
        );
        LOGGER.atInfo().log("[8/14] Registered EliteMobsSummonedMinionComponent");

        summonMinionTrackingComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsSummonMinionTrackingComponent.class,
                "EliteMobsSummonMinionTrackingComponent",
                EliteMobsSummonMinionTrackingComponent.CODEC
        );
        LOGGER.atInfo().log("[9/14] Registered EliteMobsSummonMinionTrackingComponent");

        summonRiseComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsSummonRiseComponent.class,
                "EliteMobsSummonRiseComponent",
                EliteMobsSummonRiseComponent.CODEC
        );
        LOGGER.atInfo().log("[10/14] Registered EliteMobsSummonRiseComponent");

        chargeLeapAbilityComponentType = getEntityStoreRegistry().registerComponent(ChargeLeapAbilityComponent.class,
                                                                                    "ChargeLeapAbilityComponent",
                                                                                    ChargeLeapAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[11/14] Registered ChargeLeapAbilityComponent (unified: enabled + cooldown)");

        healLeapAbilityComponentType = getEntityStoreRegistry().registerComponent(HealLeapAbilityComponent.class,
                                                                                  "HealLeapAbilityComponent",
                                                                                  HealLeapAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[12/14] Registered HealLeapAbilityComponent (unified: replaces 5 components)");

        summonUndeadAbilityComponentType = getEntityStoreRegistry().registerComponent(SummonUndeadAbilityComponent.class,
                                                                                      "SummonUndeadAbilityComponent",
                                                                                      SummonUndeadAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[13/14] Registered SummonUndeadAbilityComponent (unified: replaces 4 components)");

        abilityLockComponentType = getEntityStoreRegistry().registerComponent(
                EliteMobsAbilityLockComponent.class,
                "EliteMobsAbilityLockComponent",
                EliteMobsAbilityLockComponent.CODEC
        );
        LOGGER.atInfo().log("[14/14] Registered EliteMobsAbilityLockComponent");

        LOGGER.atInfo().log("Component registration complete: 14 total (10 core + 4 ability)");
    }

    @SuppressWarnings("unchecked")
    public void registerSystem(Object system) {
        if (system instanceof EntityTickingSystem) {
            getEntityStoreRegistry().registerSystem((EntityTickingSystem<EntityStore>) system);
        } else if (system instanceof DamageEventSystem) {
            getEntityStoreRegistry().registerSystem((DamageEventSystem) system);
        } else if (system instanceof DeathSystems.OnDeathSystem) {
            getEntityStoreRegistry().registerSystem((DeathSystems.OnDeathSystem) system);
        } else if (system instanceof com.hypixel.hytale.component.system.System) {
            getEntityStoreRegistry().registerSystem((com.hypixel.hytale.component.system.System<EntityStore>) system);
        } else {
            LOGGER.atWarning().log("Unknown system type: " + system.getClass().getName());
        }
    }

    private void registerSystems() {

        registerSystem(new EliteMobsComponentMigrationSystem(this));
        LOGGER.atInfo().log("Registered Migration System.");

        registerSystem(new EliteMobsCombatStateSystem(this));
        registerSystem(new EliteMobsAITargetPollingSystem(this));
        LOGGER.atInfo().log("Registered Combat State Systems (event-driven + AI polling).");

        featureRegistry.registerSystems(this);
        LOGGER.atInfo().log("Registered Feature Systems.");
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new EliteMobsRootCommand(this));
        LOGGER.atInfo().log("Registered EliteMobs commands.");
    }

    public EliteMobsVanillaDropsCullZoneManager getMobDropsCleanupManager() {
        return cullZoneManager;
    }

    public EliteMobsExtraDropsScheduler getExtraDropsScheduler() {
        return dropsScheduler;
    }

    public EliteMobsConfig getConfig() {
        return config;
    }

    public TickClock getTickClock() {
        return tickClock;
    }

    public ComponentType<EntityStore, EliteMobsTierComponent> getEliteMobsComponentType() {
        return eliteMobsComponentType;
    }

    public ComponentType<EntityStore, EliteMobsProgressionComponent> getProgressionComponentType() {
        return progressionComponentType;
    }

    public ComponentType<EntityStore, EliteMobsHealthScalingComponent> getHealthScalingComponentType() {
        return healthScalingComponentType;
    }

    public ComponentType<EntityStore, EliteMobsModelScalingComponent> getModelScalingComponentType() {
        return modelScalingComponentType;
    }

    public ComponentType<EntityStore, EliteMobsActiveEffectsComponent> getActiveEffectsComponentType() {
        return activeEffectsComponentType;
    }

    public ComponentType<EntityStore, EliteMobsCombatTrackingComponent> getCombatTrackingComponentType() {
        return combatTrackingComponentType;
    }

    public ComponentType<EntityStore, EliteMobsMigrationComponent> getMigrationComponentType() {
        return migrationComponentType;
    }

    public ComponentType<EntityStore, EliteMobsSummonedMinionComponent> getSummonedMinionComponentType() {
        return summonedMinionComponentType;
    }

    public ComponentType<EntityStore, EliteMobsSummonMinionTrackingComponent> getSummonMinionTrackingComponentType() {
        return summonMinionTrackingComponentType;
    }

    public ComponentType<EntityStore, EliteMobsSummonRiseComponent> getSummonRiseComponentType() {
        return summonRiseComponentType;
    }

    public ComponentType<EntityStore, ChargeLeapAbilityComponent> getChargeLeapAbilityComponentType() {
        return chargeLeapAbilityComponentType;
    }

    public ComponentType<EntityStore, HealLeapAbilityComponent> getHealLeapAbilityComponentType() {
        return healLeapAbilityComponentType;
    }

    public ComponentType<EntityStore, SummonUndeadAbilityComponent> getSummonUndeadAbilityComponentType() {
        return summonUndeadAbilityComponentType;
    }

    public ComponentType<EntityStore, EliteMobsAbilityLockComponent> getAbilityLockComponentType() {
        return abilityLockComponentType;
    }

    public EliteMobsAbilityTriggerListener getAbilityTriggerListener() {
        return abilityTriggerListener;
    }

    public HealthScalingSystem getHealthScalingSystem() {
        return healthScalingSystem;
    }

    public void setHealthScalingSystem(HealthScalingSystem system) {
        this.healthScalingSystem = system;
    }

    public ModelScalingSystem getModelScalingSystem() {
        return modelScalingSystem;
    }

    public void setModelScalingSystem(ModelScalingSystem system) {
        this.modelScalingSystem = system;
    }

    public EliteMobsNameplateService getNameplateService() {
        return nameplateService;
    }

    public EliteMobsSpawnSystem getSpawnSystem() {
        EliteMobsSpawningFeature spawning = (EliteMobsSpawningFeature) featureRegistry.getFeature("Spawning");
        return spawning != null ? spawning.getSpawnSystem() : null;
    }

    public EliteMobsAssetRetriever getEliteMobsAssetLoader() {
        return eliteMobsAssetRetriever;
    }

    public EliteMobsFeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    public EliteMobsEventBus getEventBus() {
        return eventBus;
    }

    public void requestReconcileOnNextWorldTick() {
        reconcileRequested.set(true);
    }

    public boolean shouldReconcileThisTick() {
        return reconcileTicksRemaining.get() > 0;
    }

    public void onWorldTick() {
        EliteMobsConfig cfg = config;
        if (cfg == null) return;

        if (reconcileRequested.getAndSet(false)) {
            int windowTicks = Math.max(0, cfg.reconcileConfig.reconcileWindowTicks);
            reconcileTicksRemaining.set(windowTicks);
            reconcileActive = windowTicks > 0;
            if (windowTicks > 0) {
                eventBus.fire(new EliteMobReconcileEvent());
            }
            if (cfg.reconcileConfig.announceReconcile) {
                if (windowTicks > 0) {
                    LOGGER.atInfo().log("[EliteMobs] Reconcile started (%d ticks).", windowTicks);
                } else {
                    LOGGER.atInfo().log("[EliteMobs] Reconcile skipped (window=0).");
                }
            }
            return;
        }

        int remaining = reconcileTicksRemaining.updateAndGet(value -> value > 0 ? value - 1 : 0);
        if (reconcileActive && remaining == 0) {
            reconcileActive = false;
            if (cfg.reconcileConfig.announceReconcile) {
                LOGGER.atInfo().log("[EliteMobs] Reconcile finished.");
            }
        }
    }

}
