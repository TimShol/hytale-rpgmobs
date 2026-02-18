package com.frotty27.rpgmobs.plugin;

import com.frotty27.rpgmobs.api.RPGMobsAPI;
import com.frotty27.rpgmobs.api.RPGMobsEventBus;
import com.frotty27.rpgmobs.api.events.RPGMobsReconcileEvent;
import com.frotty27.rpgmobs.api.query.RPGMobsQueryAPI;
import com.frotty27.rpgmobs.assets.RPGMobsAssetGenerator;
import com.frotty27.rpgmobs.assets.RPGMobsAssetRetriever;
import com.frotty27.rpgmobs.commands.RPGMobsRootCommand;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.effects.RPGMobsActiveEffectsComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsHealthScalingComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsMigrationComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.components.progression.RPGMobsProgressionComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonRiseComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.schema.YamlSerializer;
import com.frotty27.rpgmobs.features.RPGMobsFeatureRegistry;
import com.frotty27.rpgmobs.features.RPGMobsSpawningFeature;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.nameplates.RPGMobsNameplateService;
import com.frotty27.rpgmobs.systems.combat.RPGMobsAITargetPollingSystem;
import com.frotty27.rpgmobs.systems.combat.RPGMobsCombatStateSystem;
import com.frotty27.rpgmobs.systems.death.RPGMobsVanillaDropsCullZoneManager;
import com.frotty27.rpgmobs.systems.drops.RPGMobsExtraDropsScheduler;
import com.frotty27.rpgmobs.systems.migration.RPGMobsComponentMigrationSystem;
import com.frotty27.rpgmobs.systems.spawn.RPGMobsSpawnSystem;
import com.frotty27.rpgmobs.utils.TickClock;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.ComponentType;
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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class RPGMobsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ASSET_PACK_NAME = "RPGMobsGenerated";
    private RPGMobsConfig config;

    private ComponentType<EntityStore, RPGMobsTierComponent> RPGMobsComponentType;
    private ComponentType<EntityStore, RPGMobsProgressionComponent> progressionComponentType;
    private ComponentType<EntityStore, RPGMobsHealthScalingComponent> healthScalingComponentType;
    private ComponentType<EntityStore, RPGMobsModelScalingComponent> modelScalingComponentType;
    private ComponentType<EntityStore, RPGMobsActiveEffectsComponent> activeEffectsComponentType;
    private ComponentType<EntityStore, RPGMobsCombatTrackingComponent> combatTrackingComponentType;
    private ComponentType<EntityStore, RPGMobsMigrationComponent> migrationComponentType;
    private ComponentType<EntityStore, RPGMobsSummonedMinionComponent> summonedMinionComponentType;
    private ComponentType<EntityStore, RPGMobsSummonMinionTrackingComponent> summonMinionTrackingComponentType;
    private ComponentType<EntityStore, RPGMobsSummonRiseComponent> summonRiseComponentType;
    private ComponentType<EntityStore, ChargeLeapAbilityComponent> chargeLeapAbilityComponentType;
    private ComponentType<EntityStore, HealLeapAbilityComponent> healLeapAbilityComponentType;
    private ComponentType<EntityStore, SummonUndeadAbilityComponent> summonUndeadAbilityComponentType;
    private ComponentType<EntityStore, RPGMobsAbilityLockComponent> abilityLockComponentType;

    private final TickClock tickClock = new TickClock();
    private final RPGMobsVanillaDropsCullZoneManager cullZoneManager = new RPGMobsVanillaDropsCullZoneManager(tickClock);
    private final RPGMobsExtraDropsScheduler dropsScheduler = new RPGMobsExtraDropsScheduler(tickClock);
    private final RPGMobsNameplateService nameplateService = new RPGMobsNameplateService();
    private final RPGMobsAssetRetriever RPGMobsAssetRetriever = new RPGMobsAssetRetriever(this);
    private final RPGMobsFeatureRegistry featureRegistry = new RPGMobsFeatureRegistry(this);
    private final RPGMobsEventBus eventBus = new RPGMobsEventBus();

    private final AtomicBoolean reconcileRequested = new AtomicBoolean(false);
    private final AtomicInteger reconcileTicksRemaining = new AtomicInteger(0);
    private boolean reconcileActive = false;

    public RPGMobsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        RPGMobsAPI.setEventBus(eventBus);
        RPGMobsAPI.setQueryAPI(new RPGMobsQueryAPI(this));

        loadOrCreateRPGMobsConfig();

        getEventRegistry().register(LoadAssetEvent.class, this::onLoadAssets);
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);

        nameplateService.describeSegments(this);

        registerComponents();
        registerSystems();
        registerCommands();

        LOGGER.atInfo().log("Setup complete!");
    }

    private void onLoadAssets(LoadAssetEvent event) {
        try {
            loadOrCreateRPGMobsConfig();
            Path RPGMobsDirectory = getModDirectory();
            RPGMobsAssetGenerator.generateAll(RPGMobsDirectory, config, true);

            AssetPack assetPack = new AssetPack(RPGMobsDirectory,
                                                ASSET_PACK_NAME,
                                                RPGMobsDirectory,
                                                FileSystems.getDefault(),
                                                false,
                                                getManifest()
            );

            AssetRegistryLoader.loadAssets(event, assetPack);

            LOGGER.atInfo().log("Loaded generated AssetPack '%s' from: %s",
                                ASSET_PACK_NAME,
                                RPGMobsDirectory.toAbsolutePath()
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
        loadOrCreateRPGMobsConfig();

        RPGMobsConfig cfg = getConfig();
        if (cfg == null) throw new IllegalStateException("RPGMobsConfig is null after force reload!");

        Path RPGMobsDir = getModDirectory();
        RPGMobsAssetGenerator.generateAll(RPGMobsDir, cfg, true);

        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            LOGGER.atWarning().log("[RPGMobs] AssetModule is null; cannot force reload asset pack.");
            return;
        }

        try {
            assetModule.registerPack(ASSET_PACK_NAME, RPGMobsDir, getManifest(), true);
            LOGGER.atInfo().log("[RPGMobs] Reloaded config & regenerated assets successfully!");
            reloadNpcRoleAssetsIfPossible();
        } catch (Throwable error) {
            LOGGER.atWarning().log("[RPGMobs] Failed to reload: %s", error.toString());
            error.printStackTrace();
        }
    }

    private void reloadNpcRoleAssetsIfPossible() {
    }

    private static final String[] CONFIG_FILES = {"core.yml", "visuals.yml", "spawning.yml", "stats.yml", "gear.yml", "loot.yml", "effects.yml", "abilities.yml", "mobrules.yml"};

    public synchronized void loadOrCreateRPGMobsConfig() {
        Path modDirectory = getModDirectory();

        String oldVersion = YamlSerializer.readConfigVersion(modDirectory, "core.yml", "configVersion");

        if ("0.0.0".equals(oldVersion) && Files.exists(modDirectory.resolve("core.yml"))) {
            LOGGER.atWarning().log(
                    "[RPGMobs] Config version missing (0.0.0) — wiping all config files to regenerate fresh defaults.");
            deleteConfigFiles(modDirectory);
        }

        RPGMobsConfig defaults = new RPGMobsConfig();
        try {
            Object versionObj = getManifest().getVersion();
            if (versionObj != null) defaults.configVersion = versionObj.toString();
        } catch (Throwable ignored) {
        }

        config = YamlSerializer.loadOrCreate(modDirectory, defaults);

        if (config != null) {
            config.migrate(oldVersion);

            config.populateSummonMarkerEntriesIfEmpty();
            config.populateSummonMarkerEntriesByRoleIfEmpty();
            config.upgradeSummonMarkerEntriesToVariantIds();
            if (config.isSummonMarkerEntriesEmpty()) {
                LOGGER.atWarning().log("[RPGMobs] Undead summon is enabled but no archer NPCs were found in mob rules.");
            }
        }

        RPGMobsLogger.init(config);
        requestReconcileOnNextWorldTick();

        LOGGER.atInfo().log("Config loaded/reloaded from: %s", modDirectory.toAbsolutePath());
    }

    private Path getModDirectory() {
        return getDataDirectory().getParent().resolve("RPGMobs");
    }

    private void deleteConfigFiles(Path directory) {
        for (String fileName : CONFIG_FILES) {
            try {
                Path file = directory.resolve(fileName);
                if (Files.deleteIfExists(file)) {
                    LOGGER.atInfo().log("Deleted outdated config: %s", fileName);
                }
            } catch (IOException e) {
                LOGGER.atWarning().log("Failed to delete config file %s: %s", fileName, e.getMessage());
            }
        }
    }

    private void registerComponents() {

        RPGMobsComponentType = getEntityStoreRegistry().registerComponent(RPGMobsTierComponent.class,
                                                                          "RPGMobsTierComponent",
                                                                          RPGMobsTierComponent.CODEC
        );
        LOGGER.atInfo().log("[1/14] Registered RPGMobsTierComponent");

        progressionComponentType = getEntityStoreRegistry().registerComponent(RPGMobsProgressionComponent.class,
                                                                              "RPGMobsProgressionComponent",
                                                                              RPGMobsProgressionComponent.CODEC
        );
        LOGGER.atInfo().log("[2/14] Registered RPGMobsProgressionComponent");

        healthScalingComponentType = getEntityStoreRegistry().registerComponent(RPGMobsHealthScalingComponent.class,
                                                                                "RPGMobsHealthScalingComponent",
                                                                                RPGMobsHealthScalingComponent.CODEC
        );
        LOGGER.atInfo().log("[3/14] Registered RPGMobsHealthScalingComponent");

        modelScalingComponentType = getEntityStoreRegistry().registerComponent(RPGMobsModelScalingComponent.class,
                                                                               "RPGMobsModelScalingComponent",
                                                                               RPGMobsModelScalingComponent.CODEC
        );
        LOGGER.atInfo().log("[4/14] Registered RPGMobsModelScalingComponent");

        activeEffectsComponentType = getEntityStoreRegistry().registerComponent(RPGMobsActiveEffectsComponent.class,
                                                                                "RPGMobsActiveEffectsComponent",
                                                                                RPGMobsActiveEffectsComponent.CODEC
        );
        LOGGER.atInfo().log("[5/14] Registered RPGMobsActiveEffectsComponent");

        combatTrackingComponentType = getEntityStoreRegistry().registerComponent(RPGMobsCombatTrackingComponent.class,
                                                                                 "RPGMobsCombatTrackingComponent",
                                                                                 RPGMobsCombatTrackingComponent.CODEC
        );
        LOGGER.atInfo().log("[6/14] Registered RPGMobsCombatTrackingComponent");

        migrationComponentType = getEntityStoreRegistry().registerComponent(RPGMobsMigrationComponent.class,
                                                                            "RPGMobsMigrationComponent",
                                                                            RPGMobsMigrationComponent.CODEC
        );
        LOGGER.atInfo().log("[7/14] Registered RPGMobsMigrationComponent (for pre 1.1.0)");

        summonedMinionComponentType = getEntityStoreRegistry().registerComponent(RPGMobsSummonedMinionComponent.class,
                                                                                 "RPGMobsSummonedMinionComponent",
                                                                                 RPGMobsSummonedMinionComponent.CODEC
        );
        LOGGER.atInfo().log("[8/14] Registered RPGMobsSummonedMinionComponent");

        summonMinionTrackingComponentType = getEntityStoreRegistry().registerComponent(
                RPGMobsSummonMinionTrackingComponent.class,
                "RPGMobsSummonMinionTrackingComponent",
                RPGMobsSummonMinionTrackingComponent.CODEC
        );
        LOGGER.atInfo().log("[9/14] Registered RPGMobsSummonMinionTrackingComponent");

        summonRiseComponentType = getEntityStoreRegistry().registerComponent(RPGMobsSummonRiseComponent.class,
                                                                             "RPGMobsSummonRiseComponent",
                                                                             RPGMobsSummonRiseComponent.CODEC
        );
        LOGGER.atInfo().log("[10/14] Registered RPGMobsSummonRiseComponent");

        chargeLeapAbilityComponentType = getEntityStoreRegistry().registerComponent(ChargeLeapAbilityComponent.class,
                                                                                    "ChargeLeapAbilityComponent",
                                                                                    ChargeLeapAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[11/14] Registered ChargeLeapAbilityComponent");

        healLeapAbilityComponentType = getEntityStoreRegistry().registerComponent(HealLeapAbilityComponent.class,
                                                                                  "HealLeapAbilityComponent",
                                                                                  HealLeapAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[12/14] Registered HealLeapAbilityComponent");

        summonUndeadAbilityComponentType = getEntityStoreRegistry().registerComponent(SummonUndeadAbilityComponent.class,
                                                                                      "SummonUndeadAbilityComponent",
                                                                                      SummonUndeadAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[13/14] Registered SummonUndeadAbilityComponent");

        abilityLockComponentType = getEntityStoreRegistry().registerComponent(RPGMobsAbilityLockComponent.class,
                                                                              "RPGMobsAbilityLockComponent",
                                                                              RPGMobsAbilityLockComponent.CODEC
        );
        LOGGER.atInfo().log("[14/14] Registered RPGMobsAbilityLockComponent");

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

        registerSystem(new RPGMobsComponentMigrationSystem(this));
        LOGGER.atInfo().log("Registered Migration System");

        registerSystem(new RPGMobsCombatStateSystem(this));
        registerSystem(new RPGMobsAITargetPollingSystem(this));
        LOGGER.atInfo().log("Registered Combat State Systems");

        featureRegistry.registerSystems(this);
        LOGGER.atInfo().log("Registered Feature Systems");
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new RPGMobsRootCommand(this));
        LOGGER.atInfo().log("Registered RPGMobs commands");
    }

    public RPGMobsVanillaDropsCullZoneManager getMobDropsCleanupManager() {
        return cullZoneManager;
    }

    public RPGMobsExtraDropsScheduler getExtraDropsScheduler() {
        return dropsScheduler;
    }

    public RPGMobsConfig getConfig() {
        return config;
    }

    public TickClock getTickClock() {
        return tickClock;
    }

    public ComponentType<EntityStore, RPGMobsTierComponent> getRPGMobsComponentType() {
        return RPGMobsComponentType;
    }

    public ComponentType<EntityStore, RPGMobsProgressionComponent> getProgressionComponentType() {
        return progressionComponentType;
    }

    public ComponentType<EntityStore, RPGMobsHealthScalingComponent> getHealthScalingComponentType() {
        return healthScalingComponentType;
    }

    public ComponentType<EntityStore, RPGMobsModelScalingComponent> getModelScalingComponentType() {
        return modelScalingComponentType;
    }

    public ComponentType<EntityStore, RPGMobsActiveEffectsComponent> getActiveEffectsComponentType() {
        return activeEffectsComponentType;
    }

    public ComponentType<EntityStore, RPGMobsCombatTrackingComponent> getCombatTrackingComponentType() {
        return combatTrackingComponentType;
    }

    public ComponentType<EntityStore, RPGMobsMigrationComponent> getMigrationComponentType() {
        return migrationComponentType;
    }

    public ComponentType<EntityStore, RPGMobsSummonedMinionComponent> getSummonedMinionComponentType() {
        return summonedMinionComponentType;
    }

    public ComponentType<EntityStore, RPGMobsSummonMinionTrackingComponent> getSummonMinionTrackingComponentType() {
        return summonMinionTrackingComponentType;
    }

    public ComponentType<EntityStore, RPGMobsSummonRiseComponent> getSummonRiseComponentType() {
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

    public ComponentType<EntityStore, RPGMobsAbilityLockComponent> getAbilityLockComponentType() {
        return abilityLockComponentType;
    }

    public RPGMobsNameplateService getNameplateService() {
        return nameplateService;
    }

    public RPGMobsSpawnSystem getSpawnSystem() {
        RPGMobsSpawningFeature spawning = (RPGMobsSpawningFeature) featureRegistry.getFeature("Spawning");
        return spawning != null ? spawning.getSpawnSystem() : null;
    }

    public RPGMobsAssetRetriever getRPGMobsAssetLoader() {
        return RPGMobsAssetRetriever;
    }

    public RPGMobsFeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    public RPGMobsEventBus getEventBus() {
        return eventBus;
    }

    public void requestReconcileOnNextWorldTick() {
        reconcileRequested.set(true);
    }

    public boolean shouldReconcileThisTick() {
        return reconcileTicksRemaining.get() > 0;
    }

    public void onWorldTick() {
        RPGMobsConfig cfg = config;
        if (cfg == null) return;

        if (reconcileRequested.getAndSet(false)) {
            int windowTicks = Math.max(0, cfg.reconcileConfig.reconcileWindowTicks);
            reconcileTicksRemaining.set(windowTicks);
            reconcileActive = windowTicks > 0;
            if (windowTicks > 0) {
                eventBus.fire(new RPGMobsReconcileEvent());
            }
            if (cfg.reconcileConfig.announceReconcile) {
                if (windowTicks > 0) {
                    LOGGER.atInfo().log("[RPGMobs] Reconcile started (%d ticks).", windowTicks);
                } else {
                    LOGGER.atInfo().log("[RPGMobs] Reconcile skipped (window=0).");
                }
            }
            return;
        }

        int remaining = reconcileTicksRemaining.updateAndGet(value -> value > 0 ? value - 1 : 0);
        if (reconcileActive && remaining == 0) {
            reconcileActive = false;
            if (cfg.reconcileConfig.announceReconcile) {
                LOGGER.atInfo().log("[RPGMobs] Reconcile finished.");
            }
        }
    }

}
