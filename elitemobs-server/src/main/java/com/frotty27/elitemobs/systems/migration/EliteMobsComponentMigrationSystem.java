package com.frotty27.elitemobs.systems.migration;

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
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.Random;

public final class EliteMobsComponentMigrationSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int CURRENT_MIGRATION_VERSION = 2;

    private final EliteMobsPlugin plugin;
    private final Random random = new Random();

    public EliteMobsComponentMigrationSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(plugin.getEliteMobsComponentType());
    }

    @Override
    public void tick(float v, int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> entityStore,
                     @NonNull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);
        EliteMobsTierComponent tier = chunk.getComponent(entityIndex, plugin.getEliteMobsComponentType());

        EliteMobsMigrationComponent migration = entityStore.getComponent(entityRef, plugin.getMigrationComponentType());
        int currentVersion = (migration != null) ? migration.migrationVersion : 0;

        if (currentVersion >= CURRENT_MIGRATION_VERSION) {
            return;
        }

        if (currentVersion == 0) {
            migrateFromV0(entityRef, tier, commandBuffer);
        } else if (currentVersion == 1) {
            migrateFromV1(entityRef, entityStore, commandBuffer);
        }

        EliteMobsMigrationComponent updatedMigration = new EliteMobsMigrationComponent(CURRENT_MIGRATION_VERSION);
        commandBuffer.putComponent(entityRef, plugin.getMigrationComponentType(), updatedMigration);

        LOGGER.atInfo().log("Migrated entity from version %d to %d (tier %d)", currentVersion, CURRENT_MIGRATION_VERSION, tier.tierIndex);
    }

    private void migrateFromV0(Ref<EntityStore> entityRef, EliteMobsTierComponent tier,
                               CommandBuffer<EntityStore> commandBuffer
    ) {
        EliteMobsConfig config = plugin.getConfig();
        int tierIndex = tier.tierIndex;

        LOGGER.atInfo().log("Migrating entity from v0 (tier %d) - creating all components with defaults", tierIndex);

        migrateHealLeapAbility(entityRef, config, tierIndex, commandBuffer);
        migrateChargeLeapAbility(entityRef, config, tierIndex, commandBuffer);
        migrateSummonUndeadAbility(entityRef, config, tierIndex, commandBuffer);

        if (config.healthConfig.enableHealthScaling) {
            commandBuffer.putComponent(entityRef,
                                       plugin.getHealthScalingComponentType(),
                                       new EliteMobsHealthScalingComponent()
            );
        }
        if (config.modelConfig.enableModelScaling) {
            commandBuffer.putComponent(entityRef,
                                       plugin.getModelScalingComponentType(),
                                       new EliteMobsModelScalingComponent()
            );
        }

        commandBuffer.putComponent(entityRef,
                                   plugin.getProgressionComponentType(),
                                   new EliteMobsProgressionComponent()
        );

        commandBuffer.putComponent(entityRef,
                                   plugin.getActiveEffectsComponentType(),
                                   new EliteMobsActiveEffectsComponent()
        );

        commandBuffer.putComponent(entityRef,
                                   plugin.getCombatTrackingComponentType(),
                                   new EliteMobsCombatTrackingComponent()
        );

        commandBuffer.putComponent(entityRef,
                                   plugin.getAbilityLockComponentType(),
                                   new EliteMobsAbilityLockComponent()
        );
    }

    private void migrateFromV1(
        Ref<EntityStore> entityRef,
        Store<EntityStore> entityStore,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        EliteMobsConfig config = plugin.getConfig();
        LOGGER.atInfo().log("Migrating entity from v1 - creating split scaling components with defaults");

        if (config.healthConfig.enableHealthScaling) {
            EliteMobsHealthScalingComponent healthScaling = entityStore.getComponent(entityRef,
                                                                                     plugin.getHealthScalingComponentType()
            );
            if (healthScaling == null) {
                commandBuffer.putComponent(entityRef,
                                           plugin.getHealthScalingComponentType(),
                                           new EliteMobsHealthScalingComponent()
                );
            }
        }

        if (config.modelConfig.enableModelScaling) {
            EliteMobsModelScalingComponent modelScaling = entityStore.getComponent(entityRef,
                                                                                   plugin.getModelScalingComponentType()
            );
            if (modelScaling == null) {
                commandBuffer.putComponent(entityRef,
                                           plugin.getModelScalingComponentType(),
                                           new EliteMobsModelScalingComponent()
                );
            }
        }

        EliteMobsAbilityLockComponent abilityLock = entityStore.getComponent(entityRef,
                                                                             plugin.getAbilityLockComponentType()
        );
        if (abilityLock == null) {
            commandBuffer.putComponent(entityRef,
                                       plugin.getAbilityLockComponentType(),
                                       new EliteMobsAbilityLockComponent()
            );
        }
    }

    private void migrateHealLeapAbility(
        Ref<EntityStore> entityRef,
        EliteMobsConfig config,
        int tierIndex,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        EliteMobsConfig.HealLeapAbilityConfig abilityConfig =
            (EliteMobsConfig.HealLeapAbilityConfig) config.abilitiesConfig.defaultAbilities.get(AbilityIds.HEAL_LEAP);

        if (abilityConfig == null || !abilityConfig.isEnabled) {
            return;
        }

        if (tierIndex < 0 || tierIndex >= abilityConfig.isEnabledPerTier.length) {
            return;
        }

        if (!abilityConfig.isEnabledPerTier[tierIndex]) {
            return;
        }

        float spawnChance = tierIndex < abilityConfig.chancePerTier.length
            ? abilityConfig.chancePerTier[tierIndex]
            : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        HealLeapAbilityComponent component = new HealLeapAbilityComponent();
        component.abilityEnabled = enabled;
        component.triggerHealthPercent = 0.25f;
        component.cooldownTicksRemaining = 0L;
        commandBuffer.putComponent(entityRef, plugin.getHealLeapAbilityComponentType(), component);
    }

    private void migrateChargeLeapAbility(
        Ref<EntityStore> entityRef,
        EliteMobsConfig config,
        int tierIndex,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        EliteMobsConfig.ChargeLeapAbilityConfig abilityConfig =
            (EliteMobsConfig.ChargeLeapAbilityConfig) config.abilitiesConfig.defaultAbilities.get(AbilityIds.CHARGE_LEAP);

        if (abilityConfig == null || !abilityConfig.isEnabled) {
            return;
        }

        if (tierIndex < 0 || tierIndex >= abilityConfig.isEnabledPerTier.length) {
            return;
        }

        if (!abilityConfig.isEnabledPerTier[tierIndex]) {
            return;
        }

        float spawnChance = tierIndex < abilityConfig.chancePerTier.length
            ? abilityConfig.chancePerTier[tierIndex]
            : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        ChargeLeapAbilityComponent component = new ChargeLeapAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        commandBuffer.putComponent(entityRef, plugin.getChargeLeapAbilityComponentType(), component);
    }

    private void migrateSummonUndeadAbility(
        Ref<EntityStore> entityRef,
        EliteMobsConfig config,
        int tierIndex,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        EliteMobsConfig.SummonAbilityConfig abilityConfig =
            (EliteMobsConfig.SummonAbilityConfig) config.abilitiesConfig.defaultAbilities.get(AbilityIds.SUMMON_UNDEAD);

        if (abilityConfig == null || !abilityConfig.isEnabled) {
            return;
        }

        if (tierIndex < 0 || tierIndex >= abilityConfig.isEnabledPerTier.length) {
            return;
        }

        if (!abilityConfig.isEnabledPerTier[tierIndex]) {
            return;
        }

        float spawnChance = tierIndex < abilityConfig.chancePerTier.length
            ? abilityConfig.chancePerTier[tierIndex]
            : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        SummonUndeadAbilityComponent component = new SummonUndeadAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        component.pendingSummonTicksRemaining = 0L;
        component.pendingSummonRole = null;
        commandBuffer.putComponent(entityRef, plugin.getSummonUndeadAbilityComponentType(), component);

        EliteMobsSummonMinionTrackingComponent trackingComponent = EliteMobsSummonMinionTrackingComponent.forParent();
        commandBuffer.putComponent(entityRef, plugin.getSummonMinionTrackingComponentType(), trackingComponent);
    }
}
