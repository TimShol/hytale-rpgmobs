package com.frotty27.elitemobs.systems.spawn;

import com.frotty27.elitemobs.api.events.EliteMobSpawnedEvent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsMigrationComponent;
import com.frotty27.elitemobs.components.progression.EliteMobsProgressionComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonMinionTrackingComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonRiseComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonedMinionComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.AbilityConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.SummonAbilityConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.SummonMarkerEntry;
import com.frotty27.elitemobs.equipment.EliteMobsEquipmentService;
import com.frotty27.elitemobs.exceptions.EliteMobsException;
import com.frotty27.elitemobs.exceptions.EliteMobsSystemException;
import com.frotty27.elitemobs.exceptions.EntityComponentException;
import com.frotty27.elitemobs.features.EliteMobsFeatureRegistry;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.rules.MobRuleMatcher;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.frotty27.elitemobs.utils.Constants;
import com.frotty27.elitemobs.utils.StoreHelpers;
import com.frotty27.elitemobs.utils.WeightedIndexSelector;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembershipSystems;
import com.hypixel.hytale.server.flock.FlockPlugin;
import com.hypixel.hytale.server.flock.config.FlockAsset;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.frotty27.elitemobs.features.EliteMobsUndeadSummonAbilityFeature.ABILITY_UNDEAD_SUMMON;
import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class EliteMobsSpawnSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double SUMMON_RISE_VELOCITY_Y = 3.0;
    private static final double SUMMON_RISE_SPAWN_OFFSET_Y = 0.6;
    private static final double SUMMON_SPAWN_RADIUS = 6.0;
    private static final int SUMMON_MIN_COUNT = 3;
    private static final int SUMMON_MAX_COUNT = 7;
    private static final InteractionType HEAL_INTERACTION_TYPE = InteractionType.Ability2;
    /**
     * Ticks between each minion death in the chain reaction (~3 per second at 20 TPS).
     */
    private static final long CHAIN_DEATH_STAGGER_TICKS = Constants.TICKS_PER_SECOND / 3;
    /**
     * Modifier key used to zero out health for chain-death kills.
     */
    private static final String CHAIN_DEATH_HEALTH_MODIFIER_KEY = "elitemobs_chain_death";
    /**
     * Particle system spawned as a one-shot burst when a minion chain-dies.
     */
    private static final String CHAIN_DEATH_PARTICLE = "Explosion_Medium";

    private static final ComponentType<EntityStore, Velocity> VELOCITY_COMPONENT_TYPE = Velocity.getComponentType();
    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE = TransformComponent.getComponentType();
    private final EliteMobsPlugin eliteMobsPlugin;
    private final Random random = new Random();

    private final MobRuleMatcher mobRuleMatcher = new MobRuleMatcher();
    private final EliteMobsEquipmentService equipmentService = new EliteMobsEquipmentService();
    private final EliteMobsFeatureRegistry featureRegistry;

    private long lastReportTimestampMs = System.currentTimeMillis();
    private long mobsSeenCount;
    private long mobsMatchedCount;
    private long mobsAppliedCount;
    private final Object minionRemovalLock = new Object();
    private final List<PendingSummonerDeath> pendingSummonerDeaths = new ArrayList<>();

    private static final class PendingSummonerDeath {
        final UUID summonerId;
        final long deathTick;
        int assignedCount;

        PendingSummonerDeath(UUID summonerId, long deathTick) {
            this.summonerId = summonerId;
            this.deathTick = deathTick;
            this.assignedCount = 0;
        }
    }

    public EliteMobsSpawnSystem(EliteMobsPlugin eliteMobsPlugin) {
        this.eliteMobsPlugin = eliteMobsPlugin;
        this.featureRegistry = eliteMobsPlugin.getFeatureRegistry();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Constants.NPC_COMPONENT_TYPE);
    }

    @Override
    public void tick(float deltaTimeSeconds, int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNull Store<EntityStore> entityStore, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        try {
            NPCEntity npc = archetypeChunk.getComponent(entityIndex, Constants.NPC_COMPONENT_TYPE);
            if (npc == null) {
                throw new EntityComponentException("NPCEntity", entityIndex);
            }
            processTick(entityIndex, archetypeChunk, entityStore, commandBuffer);
        } catch (EliteMobsException e) {
            throw e;
        } catch (Exception e) {
            throw new EliteMobsSystemException("Error in EliteMobsSpawnSystem tick", e);
        }
    }

    private void processTick(int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                             @NonNull Store<EntityStore> entityStore,
                             @NonNull CommandBuffer<EntityStore> commandBuffer) {

        EliteMobsConfig config = eliteMobsPlugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> npcRef = archetypeChunk.getReferenceTo(entityIndex);

        NPCEntity npcEntity = archetypeChunk.getComponent(entityIndex, Constants.NPC_COMPONENT_TYPE);
        if (npcEntity == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        long currentTick = eliteMobsPlugin.getTickClock().getTick();

        checkMinionOfDeadSummoner(npcRef, entityStore, commandBuffer, currentTick);
        clearProcessedSummonerDeaths(currentTick);

        if (applySummonRiseIfNeeded(npcEntity, npcRef, entityStore, commandBuffer)) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        EliteMobsSummonedMinionComponent minionComponent = entityStore.getComponent(npcRef,
                                                                                    eliteMobsPlugin.getSummonedMinionComponentType()
        );
        EliteMobsTierComponent existingTierComponent = entityStore.getComponent(npcRef,
                                                                                eliteMobsPlugin.getEliteMobsComponentType()
        );

        if (minionComponent != null && (existingTierComponent == null || existingTierComponent.tierIndex < 0 || existingTierComponent.tierIndex > 1)) {
            int minTier = Math.max(0, minionComponent.minTierIndex);
            int maxTier = Math.max(minTier, minionComponent.maxTierIndex);
            int tierIndex = minTier + random.nextInt((maxTier - minTier) + 1);

            minionComponent.tierApplied = applyTierFromCommand(config,
                                                               npcRef,
                                                               entityStore,
                                                               commandBuffer, npcEntity, tierIndex
            );
            commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getSummonedMinionComponentType(), minionComponent);
            logNpcScanSummaryIfDue(config);
            return;
        }

        if (existingTierComponent != null && existingTierComponent.tierIndex >= 0) {
            tickExistingEliteMob(config, npcRef, entityStore, commandBuffer, existingTierComponent);
            logNpcScanSummaryIfDue(config);
            return;
        }

        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) {
            logNpcScanSummaryIfDue(config);
            return;
        }
        if (roleName.startsWith(EliteMobsConfig.SUMMON_ROLE_PREFIX)) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        mobsSeenCount++;

        MobRuleMatcher.MatchResult matchResult = mobRuleMatcher.findBestMatch(config, roleName);
        if (matchResult == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        mobsMatchedCount++;

        double[] spawnChances = resolveSpawnChances(config, npcEntity);
        if (spawnChances == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        int pickedTierIndex = WeightedIndexSelector.pickWeightedIndex(spawnChances, random);
        int tierIndex = clampTierIndex(pickedTierIndex);

        mobsAppliedCount++;

        EliteMobsTierComponent newTierComponent = new EliteMobsTierComponent();
        newTierComponent.tierIndex = tierIndex;

        equipmentService.buildAndApply(npcEntity, config, tierIndex, matchResult.mobRule());

        TransformComponent spawnTransform = entityStore.getComponent(npcRef, TransformComponent.getComponentType());
        var spawnedEvent = new EliteMobSpawnedEvent(npcRef,
                                                    tierIndex,
                                                    roleName,
                                                    spawnTransform != null ? spawnTransform.getPosition().clone() : new Vector3d()
        );
        eliteMobsPlugin.getEventBus().fire(spawnedEvent);
        if (spawnedEvent.isCancelled()) return;

        commandBuffer.putComponent(npcRef, eliteMobsPlugin.getEliteMobsComponentType(), newTierComponent);

        createNewSchemaComponents(config, npcRef, commandBuffer, npcEntity);
        featureRegistry.applyAll(eliteMobsPlugin,
                                 config,
                                 npcRef,
                                 entityStore,
                                 commandBuffer,
                                 newTierComponent,
                                 roleName
        );

        if (config.debugConfig.isDebugModeEnabled) {
            EliteMobsLogger.debug(LOGGER,
                                  "Elite applied: role=%s tier=%d ruleKey=%s matchKind=%s score=%d",
                                  EliteMobsLogLevel.INFO,
                                  roleName,
                                  tierIndex,
                                  matchResult.key(),
                                  String.valueOf(matchResult.matchKind()),
                                  matchResult.score()
            );
        }

        logNpcScanSummaryIfDue(config);
    }


    private boolean applySummonRiseIfNeeded(NPCEntity npcEntity, Ref<EntityStore> npcRef,
                                            Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) return false;
        boolean summonRole = roleName.startsWith(EliteMobsConfig.SUMMON_ROLE_PREFIX);
        EliteMobsSummonedMinionComponent minionComponent = entityStore.getComponent(npcRef,
                                                                                    eliteMobsPlugin.getSummonedMinionComponentType()
        );
        boolean trackedSummon = summonRole || minionComponent != null;
        if (!trackedSummon) return false;

        TransformComponent transform = entityStore.getComponent(npcRef, TRANSFORM_COMPONENT_TYPE);

        EliteMobsSummonRiseComponent riseComponent = entityStore.getComponent(npcRef,
                                                                              eliteMobsPlugin.getSummonRiseComponentType()
        );
        if (riseComponent != null && riseComponent.applied) return false;

        if (transform != null) {
            TransformComponent updated = transform.clone();
            Vector3d pos = updated.getPosition();
            updated.setPosition(new Vector3d(pos.getX(), pos.getY() - SUMMON_RISE_SPAWN_OFFSET_Y, pos.getZ()));
            commandBuffer.putComponent(npcRef, TRANSFORM_COMPONENT_TYPE, updated);
        }

        Velocity velocity = entityStore.getComponent(npcRef, VELOCITY_COMPONENT_TYPE);
        if (velocity == null) {
            velocity = new Velocity();
        }
        double nextY = Math.max(velocity.getY(), SUMMON_RISE_VELOCITY_Y);
        velocity.set(velocity.getX(), nextY, velocity.getZ());
        commandBuffer.putComponent(npcRef, VELOCITY_COMPONENT_TYPE, velocity);

        if (riseComponent == null) riseComponent = new EliteMobsSummonRiseComponent();
        riseComponent.applied = true;
        commandBuffer.putComponent(npcRef, eliteMobsPlugin.getSummonRiseComponentType(), riseComponent);
        return true;
    }

    public void queueSummonerDeath(UUID summonerId, long deathTick) {
        if (summonerId == null) return;
        synchronized (minionRemovalLock) {
            pendingSummonerDeaths.add(new PendingSummonerDeath(summonerId, deathTick));
        }
    }

    void checkMinionOfDeadSummoner(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                   CommandBuffer<EntityStore> commandBuffer, long currentTick) {
        synchronized (minionRemovalLock) {
            if (pendingSummonerDeaths.isEmpty()) return;
        }
        EliteMobsSummonedMinionComponent minion = store.getComponent(npcRef,
                                                                     eliteMobsPlugin.getSummonedMinionComponentType()
        );
        if (minion == null || minion.summonerId == null) return;

        // Already has DeathComponent — engine is handling death
        DeathComponent existingDeath = store.getComponent(npcRef, DeathComponent.getComponentType());
        if (existingDeath != null) return;

        if (minion.chainDeathAtTick == -1L) {
            // Already fired — waiting for engine to process death
            return;
        }

        if (minion.chainDeathAtTick > 0) {
            // Death already scheduled — check if it's time
            if (currentTick >= minion.chainDeathAtTick) {
                EliteMobsLogger.debug(LOGGER,
                                      "[MinionDespawn] Chain-death firing: summoner=%s tick=%d",
                                      EliteMobsLogLevel.INFO,
                                      minion.summonerId,
                                      currentTick
                );
                spawnChainDeathExplosion(npcRef, store);
                killMinionViaHealth(npcRef, store, commandBuffer);
                minion.chainDeathAtTick = -1L;
                commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getSummonedMinionComponentType(), minion);
            }
            return;
        }

        // Not yet scheduled — check if summoner is dead and assign staggered tick
        synchronized (minionRemovalLock) {
            for (PendingSummonerDeath death : pendingSummonerDeaths) {
                if (death.summonerId.equals(minion.summonerId)) {
                    long scheduledTick = death.deathTick + (death.assignedCount * CHAIN_DEATH_STAGGER_TICKS) + 1;
                    death.assignedCount++;
                    minion.chainDeathAtTick = scheduledTick;
                    commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getSummonedMinionComponentType(), minion);
                    EliteMobsLogger.debug(LOGGER,
                                          "[MinionDespawn] Scheduled chain-death: summoner=%s atTick=%d (index=%d)",
                                          EliteMobsLogLevel.INFO,
                                          minion.summonerId,
                                          scheduledTick,
                                          death.assignedCount - 1
                    );
                    return;
                }
            }
        }
    }

    void clearProcessedSummonerDeaths(long currentTick) {
        synchronized (minionRemovalLock) {
            pendingSummonerDeaths.removeIf(d -> currentTick - d.deathTick > Constants.TICKS_PER_SECOND * 5);
        }
    }

    /**
     * Spawns a one-shot explosion particle burst at the minion's position using
     * {@link ParticleUtil} — completely independent of the EffectController,
     * so it plays exactly once and never loops.
     */
    private void spawnChainDeathExplosion(Ref<EntityStore> npcRef, Store<EntityStore> store) {
        TransformComponent transform = store.getComponent(npcRef, TRANSFORM_COMPONENT_TYPE);
        if (transform == null) return;
        Vector3d pos = transform.getPosition();
        ParticleUtil.spawnParticleEffect(CHAIN_DEATH_PARTICLE, pos, store);
    }

    /**
     * Kills a minion by applying a -1.0 multiplicative modifier on MAX health,
     * which triggers the engine's natural death system (animation + sound).
     */
    private void killMinionViaHealth(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                     CommandBuffer<EntityStore> commandBuffer) {
        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        entityStats.putModifier(healthStatId,
                                CHAIN_DEATH_HEALTH_MODIFIER_KEY,
                                new StaticModifier(Modifier.ModifierTarget.MAX,
                                                   StaticModifier.CalculationType.MULTIPLICATIVE,
                                                   -1.0f
                                )
        );
        entityStats.update();
        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);
    }

    public boolean applyTierFromCommand(EliteMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                        CommandBuffer<EntityStore> commandBuffer, NPCEntity npcEntity, int tierIndex) {
        if (config == null || npcEntity == null) return false;

        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) return false;

        int clampedTierIndex = clampTierIndex(tierIndex);

        MobRuleMatcher.MatchResult matchResult = mobRuleMatcher.findBestMatch(config, roleName);

        EliteMobsTierComponent newTierComponent = new EliteMobsTierComponent();
        newTierComponent.tierIndex = clampedTierIndex;

        if (matchResult != null) {
            equipmentService.buildAndApply(npcEntity, config, clampedTierIndex, matchResult.mobRule());
        }

        commandBuffer.putComponent(npcRef, eliteMobsPlugin.getEliteMobsComponentType(), newTierComponent);

        createNewSchemaComponents(config, npcRef, commandBuffer, npcEntity);

        featureRegistry.applyAll(eliteMobsPlugin,
                                 config,
                                 npcRef,
                                 entityStore,
                                 commandBuffer,
                                 newTierComponent,
                                 roleName
        );

        TransformComponent spawnTransform = entityStore.getComponent(npcRef, TransformComponent.getComponentType());
        eliteMobsPlugin.getEventBus().fire(new EliteMobSpawnedEvent(npcRef,
                                                                    clampedTierIndex,
                                                                    roleName,
                                                                    spawnTransform != null ? spawnTransform.getPosition().clone() : new Vector3d()
        ));

        if (config.debugConfig.isDebugModeEnabled) {
            EliteMobsLogger.debug(LOGGER,
                                  "Elite applied (command): role=%s tier=%d ruleKey=%s matchKind=%s score=%d",
                                  EliteMobsLogLevel.INFO,
                                  roleName,
                                  clampedTierIndex,
                                  matchResult == null ? "none" : matchResult.key(),
                                  matchResult == null ? "none" : String.valueOf(matchResult.matchKind()),
                                  matchResult == null ? -1 : matchResult.score()
            );
        }

        return true;
    }

    private double[] resolveSpawnChances(EliteMobsConfig config, NPCEntity npcEntity) {
        if (config.spawning.progressionStyle == EliteMobsConfig.ProgressionStyle.DISTANCE_FROM_SPAWN) {
            return resolveSpawnChancesByDistance(config, npcEntity);
        }
        if (config.spawning.progressionStyle == EliteMobsConfig.ProgressionStyle.NONE) {
            return config.spawning.spawnChancePerTier;
        }
        return resolveSpawnChancesForEnvironment(config, npcEntity);
    }

    private double[] resolveSpawnChancesByDistance(EliteMobsConfig config, NPCEntity npcEntity) {
        double dist = getXZDistance(npcEntity);
        double distPerTier = Math.max(1.0, config.spawning.distancePerTier);

        int tier = (int) (dist / distPerTier);
        tier = clampTierIndex(tier);

        double[] chances = new double[Constants.TIERS_AMOUNT];
        chances[tier] = 1.0;
        return chances;
    }

    private double getXZDistance(NPCEntity npcEntity) {
        if (npcEntity == null) return 0.0;

        Ref<EntityStore> ref = npcEntity.getReference();
        if (ref == null) return 0.0;
        Store<EntityStore> store = ref.getStore();
        if (store == null) return 0.0;
        TransformComponent t = store.getComponent(ref, TRANSFORM_COMPONENT_TYPE);
        if (t == null) return 0.0;

        Vector3d pos = t.getPosition();
        return Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
    }

    private double[] resolveSpawnChancesForEnvironment(EliteMobsConfig config, NPCEntity npcEntity) {
        if (!config.spawning.enableEnvironmentTierSpawns) return config.spawning.spawnChancePerTier;
        if (config.spawning.defaultEnvironmentTierSpawns == null || config.spawning.defaultEnvironmentTierSpawns.isEmpty())
            return config.spawning.spawnChancePerTier;

        int envIndex = npcEntity.getEnvironment();
        Environment env = Environment.getAssetMap().getAsset(envIndex);
        String envId = env == null ? null : env.getId();

        EliteMobsConfig.EnvironmentTierRule rule = null;
        if (envId != null) {
            rule = config.spawning.defaultEnvironmentTierSpawns.get(envId);
        }
        if (rule == null) {
            rule = config.spawning.defaultEnvironmentTierSpawns.get("default");
        }
        if (rule == null) return config.spawning.spawnChancePerTier;
        if (!rule.enabled) return null;
        if (rule.spawnChancePerTier == null || rule.spawnChancePerTier.length == 0)
            return config.spawning.spawnChancePerTier;
        return rule.spawnChancePerTier;
    }

    private void tickExistingEliteMob(EliteMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                      CommandBuffer<EntityStore> commandBuffer, EliteMobsTierComponent tierComponent) {

        boolean tierComponentChanged = false;
        long currentTick = eliteMobsPlugin.getTickClock().getTick();

        String roleName = null;
        NPCEntity npcEntity = entityStore.getComponent(npcRef, Constants.NPC_COMPONENT_TYPE);
        if (npcEntity != null) roleName = npcEntity.getRoleName();

        decrementAbilityCooldowns(npcRef, entityStore, commandBuffer);

        tierComponentChanged |= tryHandlePendingSummon(config,
                                                       npcRef, npcEntity, entityStore, commandBuffer
        );

        tierComponentChanged |= finalizeHealSwapIfNeeded(npcRef, npcEntity, entityStore, commandBuffer
        );

        finalizeSummonSwapIfNeeded(npcRef, npcEntity, entityStore, commandBuffer);

        if (eliteMobsPlugin.shouldReconcileThisTick()) {
            featureRegistry.reconcileAll(eliteMobsPlugin,
                                         config,
                                         npcRef,
                                         entityStore,
                                         commandBuffer,
                                         tierComponent,
                                         roleName
            );
        }

        if (tierComponentChanged) {
            commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getEliteMobsComponentType(), tierComponent);
        }
    }

    private void decrementAbilityCooldowns(Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                            CommandBuffer<EntityStore> commandBuffer) {
        ChargeLeapAbilityComponent chargeLeap = entityStore.getComponent(npcRef,
                                                                         eliteMobsPlugin.getChargeLeapAbilityComponentType()
        );
        if (chargeLeap != null && chargeLeap.cooldownTicksRemaining > 0) {
            chargeLeap.cooldownTicksRemaining--;
            commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getChargeLeapAbilityComponentType(), chargeLeap);
        }

        HealLeapAbilityComponent healLeap = entityStore.getComponent(npcRef,
                                                                     eliteMobsPlugin.getHealLeapAbilityComponentType()
        );
        if (healLeap != null && healLeap.cooldownTicksRemaining > 0) {
            healLeap.cooldownTicksRemaining--;
            commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getHealLeapAbilityComponentType(), healLeap);
        }

        SummonUndeadAbilityComponent summon = entityStore.getComponent(npcRef,
                                                                       eliteMobsPlugin.getSummonUndeadAbilityComponentType()
        );
        if (summon != null) {
            boolean changed = false;
            if (summon.cooldownTicksRemaining > 0) {
                summon.cooldownTicksRemaining--;
                changed = true;
            }
            if (summon.pendingSummonTicksRemaining > 0) {
                summon.pendingSummonTicksRemaining--;
                changed = true;
            }
            if (changed) {
                commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getSummonUndeadAbilityComponentType(), summon);
            }
        }
    }

    private boolean tryHandlePendingSummon(EliteMobsConfig config, Ref<EntityStore> npcRef, NPCEntity npcEntity,
                                           Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        boolean changed = false;

        DeathComponent deathCheck = entityStore.getComponent(npcRef, DeathComponent.getComponentType());
        if (deathCheck != null) return false;

        SummonUndeadAbilityComponent summonAbility = entityStore.getComponent(npcRef,
                                                                              eliteMobsPlugin.getSummonUndeadAbilityComponentType()
        );
        if (summonAbility == null) return false;

        if (summonAbility.pendingSummonRole == null || summonAbility.pendingSummonTicksRemaining > 0) return false;
        summonAbility.pendingSummonTicksRemaining = 0L;

        SummonAbilityConfig summonConfig = getSummonAbilityConfig(config);
        if (summonConfig == null || npcEntity == null) {
            summonAbility.pendingSummonRole = null;
            commandBuffer.replaceComponent(npcRef,
                                           eliteMobsPlugin.getSummonUndeadAbilityComponentType(),
                                           summonAbility
            );
            return false;
        }

        String roleIdentifier = summonAbility.pendingSummonRole;
        summonAbility.pendingSummonRole = null;
        commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getSummonUndeadAbilityComponentType(), summonAbility);

        List<SummonMarkerEntry> entries = resolveSummonEntries(summonConfig, roleIdentifier);
        if (entries.isEmpty()) {
            if (config.debugConfig.isDebugModeEnabled) {
                EliteMobsLogger.debug(LOGGER,
                                      "Pending summon skipped: no entries for role=%s",
                                      EliteMobsLogLevel.INFO,
                                      String.valueOf(roleIdentifier)
                );
            }
            return changed;
        }

        EliteMobsSummonMinionTrackingComponent summonTracking = entityStore.getComponent(npcRef,
                                                                                         eliteMobsPlugin.getSummonMinionTrackingComponentType()
        );
        int summonedAliveCount = summonTracking != null ? summonTracking.summonedAliveCount : 0;

        int maxAlive = clampSummonMaxAlive(summonConfig.maxAlive);
        int remaining = Math.max(0, maxAlive - summonedAliveCount);
        if (remaining == 0) {
            if (config.debugConfig.isDebugModeEnabled) {
                EliteMobsLogger.debug(LOGGER,
                                      "Pending summon skipped: cap reached alive=%d max=%d",
                                      EliteMobsLogLevel.INFO,
                                      summonedAliveCount,
                                      maxAlive
                );
            }
            return changed;
        }

        TransformComponent transform = entityStore.getComponent(npcRef, TRANSFORM_COMPONENT_TYPE);
        if (transform == null) return changed;

        Vector3d center = transform.getPosition();
        int spawnCount = clampSummonCount(pickFlockSize(entries));
        spawnCount = Math.min(spawnCount, remaining);
        if (spawnCount <= 0) {
            if (config.debugConfig.isDebugModeEnabled) {
                EliteMobsLogger.debug(LOGGER,
                                      "Pending summon skipped: spawnCount=0 remaining=%d",
                                      EliteMobsLogLevel.INFO,
                                      remaining
                );
            }
            return changed;
        }

        if (config.debugConfig.isDebugModeEnabled) {
            EliteMobsLogger.debug(LOGGER,
                                  "Spawning minions: role=%s count=%d remaining=%d",
                                  EliteMobsLogLevel.INFO,
                                  String.valueOf(roleIdentifier),
                                  spawnCount,
                                  remaining
            );
        }

        var world = npcEntity.getWorld();
        if (world == null) return changed;
        var entityStoreProvider = world.getEntityStore();
        if (entityStoreProvider == null) return changed;
        UUIDComponent uuidComponent = entityStore.getComponent(npcRef, UUIDComponent.getComponentType());
        if (uuidComponent == null && npcEntity.getReference() != null) {
            Ref<EntityStore> ref = npcEntity.getReference();
            uuidComponent = ref.getStore().getComponent(ref, UUIDComponent.getComponentType());
        }
        UUID summonerId = uuidComponent == null ? null : uuidComponent.getUuid();
        if (summonerId == null) return changed;

        for (int i = 0; i < spawnCount; i++) {
            SummonMarkerEntry entry = pickWeightedEntry(entries);
            if (entry == null || entry.Name == null || entry.Name.isBlank()) continue;
            Vector3d pos = pickSpawnPosition(center);
            String roleName = entry.Name.trim();
            if (roleName.startsWith(EliteMobsConfig.SUMMON_ROLE_PREFIX)) {
                roleName = roleName.substring(EliteMobsConfig.SUMMON_ROLE_PREFIX.length()).trim();
            }
            final String roleNameFinal = roleName;
            final Vector3d posFinal = pos;
            world.execute(() -> {
                var spawned = NPCPlugin.get().spawnNPC(entityStoreProvider.getStore(), roleNameFinal, null,
                                                       posFinal,
                                                       new Vector3f(0f, 0f, 0f)
                );
                if (spawned == null || spawned.first() == null) {
                    if (config.debugConfig.isDebugModeEnabled) {
                        EliteMobsLogger.debug(LOGGER, "Spawn failed: role=%s", EliteMobsLogLevel.INFO, roleNameFinal
                        );
                    }
                    return;
                }
                Ref<EntityStore> spawnedRef = spawned.first();
                EntityStore storeProvider = world.getEntityStore();
                if (storeProvider == null) return;
                Store<EntityStore> store = storeProvider.getStore();

                StoreHelpers.withEntity(store, spawnedRef, (_, cb, _) -> {
                                            EliteMobsSummonedMinionComponent minion = store.getComponent(spawnedRef,
                                                                                                         eliteMobsPlugin.getSummonedMinionComponentType()
                                            );
                                            if (minion == null) {
                                                minion = new EliteMobsSummonedMinionComponent();
                                            }
                                            minion.summonerId = summonerId;
                                            minion.minTierIndex = 0;
                    minion.maxTierIndex = 2;
                                            minion.tierApplied = false;
                    cb.putComponent(spawnedRef, eliteMobsPlugin.getSummonedMinionComponentType(), minion);
                                        }
                );

                // Join minion to summoner's flock so it follows
                joinMinionToSummonerFlock(store, npcRef, spawnedRef);

                if (npcRef.isValid()) {
                    StoreHelpers.withEntity(store, npcRef, (_, cb, _) -> {
                                                EliteMobsSummonMinionTrackingComponent tracking = store.getComponent(npcRef,
                                                                                                                     eliteMobsPlugin.getSummonMinionTrackingComponentType()
                                                );
                        if (tracking == null) tracking = new EliteMobsSummonMinionTrackingComponent();
                                                if (tracking.summonedAliveCount < 0) tracking.summonedAliveCount = 0;
                                                tracking.summonedAliveCount++;
                                                cb.replaceComponent(npcRef, eliteMobsPlugin.getSummonMinionTrackingComponentType(), tracking);
                                            }
                    );
                }
            });
        }

        return changed;
    }

    private boolean finalizeHealSwapIfNeeded(Ref<EntityStore> npcRef, NPCEntity npcEntity,
                                             Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        if (npcEntity == null) return false;

        HealLeapAbilityComponent healLeapAbility = entityStore.getComponent(npcRef,
                                                                            eliteMobsPlugin.getHealLeapAbilityComponentType()
        );
        if (healLeapAbility == null || !healLeapAbility.swapActive) return false;

        boolean healChainActive = AbilityHelpers.isInteractionTypeRunning(entityStore, npcRef, HEAL_INTERACTION_TYPE);

        if (!healChainActive && healLeapAbility.swapActive) {
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, healLeapAbility);
            healLeapAbility.swapActive = false;
            healLeapAbility.swapSlot = -1;
            healLeapAbility.swapPreviousItem = null;
            healLeapAbility.hitsTaken = 0;
            commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getHealLeapAbilityComponentType(), healLeapAbility);
            return false;
        }

        return false;
    }

    private void finalizeSummonSwapIfNeeded(Ref<EntityStore> npcRef, NPCEntity npcEntity,
                                            Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        if (npcEntity == null) return;

        SummonUndeadAbilityComponent summonAbility = entityStore.getComponent(npcRef,
                                                                              eliteMobsPlugin.getSummonUndeadAbilityComponentType()
        );
        if (summonAbility == null || !summonAbility.swapActive) return;

        boolean summonChainActive = AbilityHelpers.isInteractionTypeRunning(entityStore, npcRef, HEAL_INTERACTION_TYPE);

        if (!summonChainActive) {
            AbilityHelpers.restoreSummonWeaponIfNeeded(npcEntity, summonAbility);
            commandBuffer.replaceComponent(npcRef,
                                           eliteMobsPlugin.getSummonUndeadAbilityComponentType(),
                                           summonAbility
            );
        }
    }

    private static int clampSummonCount(int count) {
        int clamped = Math.max(SUMMON_MIN_COUNT, count);
        return Math.min(SUMMON_MAX_COUNT, clamped);
    }

    private static int clampSummonMaxAlive(int value) {
        if (value < EliteMobsConfig.SUMMON_MAX_ALIVE_MIN) return EliteMobsConfig.SUMMON_MAX_ALIVE_MIN;
        return Math.min(value, EliteMobsConfig.SUMMON_MAX_ALIVE_MAX);
    }

    private SummonAbilityConfig getSummonAbilityConfig(EliteMobsConfig config) {
        if (config.abilitiesConfig == null || config.abilitiesConfig.defaultAbilities == null) return null;
        AbilityConfig raw = config.abilitiesConfig.defaultAbilities.get(ABILITY_UNDEAD_SUMMON);
        return (raw instanceof SummonAbilityConfig summonAbilityConfig) ? summonAbilityConfig : null;
    }

    private List<SummonMarkerEntry> resolveSummonEntries(SummonAbilityConfig config, String roleIdentifier) {
        if (config == null) return List.of();
        String normalized = roleIdentifier == null ? "" : EliteMobsConfig.normalizeRoleIdentifier(roleIdentifier);
        if (config.spawnMarkerEntriesByRole != null && !normalized.isBlank()) {
            List<SummonMarkerEntry> byRole = config.spawnMarkerEntriesByRole.get(normalized);
            if (byRole != null && !byRole.isEmpty()) return byRole;
        }
        if (config.spawnMarkerEntriesByRole != null) {
            List<SummonMarkerEntry> fallback = config.spawnMarkerEntriesByRole.get("default");
            if (fallback != null && !fallback.isEmpty()) return fallback;
            fallback = config.spawnMarkerEntriesByRole.get("Default");
            if (fallback != null && !fallback.isEmpty()) return fallback;
        }
        if (config.spawnMarkerEntries != null && !config.spawnMarkerEntries.isEmpty()) return config.spawnMarkerEntries;
        return List.of();
    }

    private int pickFlockSize(List<SummonMarkerEntry> entries) {
        String flockId = null;
        for (SummonMarkerEntry entry : entries) {
            if (entry != null && entry.Flock != null && !entry.Flock.isBlank()) {
                flockId = entry.Flock.trim();
                break;
            }
        }
        if (flockId == null) return 1;
        FlockAsset flock = FlockAsset.getAssetMap().getAsset(flockId);
        if (flock == null) return 1;
        return Math.max(1, flock.pickFlockSize());
    }

    private SummonMarkerEntry pickWeightedEntry(List<SummonMarkerEntry> entries) {
        if (entries == null || entries.isEmpty()) return null;
        double total = 0.0;
        for (SummonMarkerEntry entry : entries) {
            if (entry == null) continue;
            total += Math.max(0.0, entry.Weight);
        }
        if (total <= 0.0) return entries.getFirst();
        double roll = random.nextDouble() * total;
        double cumulative = 0.0;
        for (SummonMarkerEntry entry : entries) {
            if (entry == null) continue;
            cumulative += Math.max(0.0, entry.Weight);
            if (roll <= cumulative) return entry;
        }
        return entries.getLast();
    }

    private Vector3d pickSpawnPosition(Vector3d center) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = random.nextDouble() * SUMMON_SPAWN_RADIUS;
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;
        return new Vector3d(center.getX() + dx, center.getY(), center.getZ() + dz);
    }

    /**
     * Makes a minion follow its summoner by joining it to the summoner's flock.
     * If the summoner doesn't have a flock yet, one is created from its NPC role.
     */
    private void joinMinionToSummonerFlock(Store<EntityStore> store, Ref<EntityStore> summonerRef,
                                           Ref<EntityStore> minionRef) {
        if (!summonerRef.isValid() || !minionRef.isValid()) return;

        try {
            // Get existing flock or create one for the summoner
            Ref<EntityStore> flockRef = FlockPlugin.getFlockReference(summonerRef, store);
            if (flockRef == null || !flockRef.isValid()) {
                NPCEntity summonerNpc = store.getComponent(summonerRef, Constants.NPC_COMPONENT_TYPE);
                if (summonerNpc == null || summonerNpc.getRole() == null) return;
                flockRef = FlockPlugin.createFlock(store, summonerNpc.getRole());
                if (flockRef == null || !flockRef.isValid()) {
                    LOGGER.atWarning().log("[FlockFollow] Failed to create flock for summoner");
                    return;
                }
                FlockMembershipSystems.join(summonerRef, flockRef, store);
                EliteMobsLogger.debug(LOGGER,
                                      "[FlockFollow] Created flock for summoner, joined as leader",
                                      EliteMobsLogLevel.INFO
                );
            }

            FlockMembershipSystems.join(minionRef, flockRef, store);
            EliteMobsLogger.debug(LOGGER, "[FlockFollow] Minion joined summoner's flock", EliteMobsLogLevel.INFO);
        } catch (Exception e) {
            LOGGER.atWarning().log("[FlockFollow] Failed to join minion to flock: %s", e.getMessage());
        }
    }

    private void logNpcScanSummaryIfDue(EliteMobsConfig config) {
        if (!config.debugConfig.isDebugModeEnabled) return;

        long now = System.currentTimeMillis();
        long everyMs = Math.max(1, config.debugConfig.debugMobRuleScanIntervalSeconds) * 1000L;
        if (now - lastReportTimestampMs < everyMs) return;

        lastReportTimestampMs = now;

        EliteMobsLogger.debug(LOGGER,
                              "Scan: seen=%d matched=%d applied=%d",
                              EliteMobsLogLevel.INFO,
                              mobsSeenCount,
                              mobsMatchedCount,
                              mobsAppliedCount
        );
    }

    private void createNewSchemaComponents(EliteMobsConfig config, Ref<EntityStore> npcRef,
                                           CommandBuffer<EntityStore> commandBuffer, NPCEntity npcEntity) {

        EliteMobsMigrationComponent migration = new EliteMobsMigrationComponent(2);
        commandBuffer.putComponent(npcRef, eliteMobsPlugin.getMigrationComponentType(), migration);

        if (config.spawning.progressionStyle == EliteMobsConfig.ProgressionStyle.DISTANCE_FROM_SPAWN) {
            double dist = getXZDistance(npcEntity);
            EliteMobsProgressionComponent progression = getProgressionComponent(config, dist);
            commandBuffer.putComponent(npcRef, eliteMobsPlugin.getProgressionComponentType(), progression);
        }
    }

    private static @NonNull EliteMobsProgressionComponent getProgressionComponent(EliteMobsConfig config, double dist) {
        double interval = Math.max(1.0, config.spawning.distanceBonusInterval);
        int intervals = (int) (dist / interval);

        float healthBonus = 0f;
        float damageBonus = 0f;
        if (intervals > 0) {
            healthBonus = intervals * config.spawning.distanceHealthBonusPerInterval;
            damageBonus = intervals * config.spawning.distanceDamageBonusPerInterval;

            if (healthBonus > config.spawning.distanceHealthBonusCap) {
                healthBonus = config.spawning.distanceHealthBonusCap;
            }
            if (damageBonus > config.spawning.distanceDamageBonusCap) {
                damageBonus = config.spawning.distanceDamageBonusCap;
            }
        }

        EliteMobsProgressionComponent progression = new EliteMobsProgressionComponent(healthBonus,
                                                                                      damageBonus,
                                                                                      (float) dist
        );
        return progression;
    }
}
