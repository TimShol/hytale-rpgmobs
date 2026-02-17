package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.api.IEliteMobsEventListener;
import com.frotty27.elitemobs.api.events.*;
import com.frotty27.elitemobs.assets.TemplateNameGenerator;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.elitemobs.components.ability.EliteMobsAbilityLockComponent;
import com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.elitemobs.components.combat.EliteMobsCombatTrackingComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonMinionTrackingComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.features.EliteMobsAbilityFeatureHelpers;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.frotty27.elitemobs.utils.Constants;
import com.frotty27.elitemobs.utils.StoreHelpers;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class EliteMobsAbilityTriggerListener implements IEliteMobsEventListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EliteMobsPlugin plugin;

    public EliteMobsAbilityTriggerListener(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reevaluateAbilitiesForCombatEntity(Ref<EntityStore> entityRef) {
        evaluateAbilitiesForEntity(entityRef, AbilityTriggerSource.AGGRO);
    }

    @Override
    public void onEliteMobAggro(EliteMobAggroEvent event) {
        LOGGER.atInfo().log("[Aggro] Mob aggro'd tier=%d", event.getTier());
        evaluateAbilitiesForEntity(event.getEntityRef(), AbilityTriggerSource.AGGRO);
    }

    @Override
    public void onEliteMobDamageReceived(EliteMobDamageReceivedEvent event) {
        if (checkHealLeapInterrupt(event)) return;

        evaluateAbilitiesForEntity(event.getEntityRef(), AbilityTriggerSource.DAMAGE_RECEIVED);
    }

    @Override
    public void onEliteMobAbilityCompleted(EliteMobAbilityCompletedEvent event) {
        handleAbilityCompletionRetrigger(event);
    }

    @Override
    public void onEliteMobDeath(EliteMobDeathEvent event) {
        handleDeathInterrupt(event);
    }

    @Override
    public void onEliteMobDeaggro(EliteMobDeaggroEvent event) {
        handleDeaggroInterrupt(event);
    }

    private void evaluateAbilitiesForEntity(Ref<EntityStore> entityRef, AbilityTriggerSource source) {
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        // Dead mobs cannot use abilities
        DeathComponent death = store.getComponent(entityRef, DeathComponent.getComponentType());
        if (death != null) return;

        EliteMobsTierComponent tier = store.getComponent(entityRef, plugin.getEliteMobsComponentType());
        if (tier == null || tier.tierIndex < 0) return;

        EliteMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock != null && (lock.isLocked() || lock.isChainStartPending())) {
            LOGGER.atInfo().log("[AbilityEval] SKIP: lock active (locked=%b pending=%b ability=%s) source=%s",
                    lock.isLocked(), lock.isChainStartPending(), lock.activeAbilityId, source.name());
            return;
        }

        EliteMobsConfig config = plugin.getConfig();
        if (config == null) return;

        int tierIndex = clampTierIndex(tier.tierIndex);

        if (source == AbilityTriggerSource.DAMAGE_RECEIVED) {
            if (tryTriggerHealLeap(entityRef, store, config, tierIndex)) return;
        }

        if (source == AbilityTriggerSource.AGGRO) {
            if (tryTriggerChargeLeap(entityRef, store, config, tierIndex)) return;
            tryTriggerSummonUndead(entityRef, store, config, tierIndex);
        }
    }

    private boolean tryTriggerHealLeap(Ref<EntityStore> entityRef, Store<EntityStore> store, EliteMobsConfig config,
            int tierIndex
    ) {
        HealLeapAbilityComponent healLeap = store.getComponent(entityRef, plugin.getHealLeapAbilityComponentType());
        if (healLeap == null || !healLeap.abilityEnabled) return false;

        if (healLeap.cooldownTicksRemaining > 0) return false;

        float healthPercent = calculateHealthPercent(entityRef, store);
        if (healthPercent >= healLeap.triggerHealthPercent) return false;

        Ref<EntityStore> targetRef = getAggroTarget(entityRef, store);
        EliteMobAbilityStartedEvent startedEvent = new EliteMobAbilityStartedEvent(
                entityRef, AbilityIds.HEAL_LEAP, tierIndex, targetRef
        );
        plugin.getEventBus().fire(startedEvent);
        if (startedEvent.isCancelled()) return false;

        boolean started = startAbilityChain(entityRef, store, AbilityIds.HEAL_LEAP, tierIndex, config);
        if (!started) {
            EliteMobsLogger.debug(LOGGER,
                    "[AbilityTrigger] heal_leap chain failed to start for tier %d",
                    EliteMobsLogLevel.WARNING, tierIndex);
            return false;
        }

        EliteMobsConfig.HealLeapAbilityConfig abilityConfig = getHealLeapConfig(config);
        if (abilityConfig != null) {
            healLeap.cooldownTicksRemaining = getCooldownTicks(abilityConfig.cooldownSecondsPerTier, tierIndex);
        }

        lockAbility(entityRef, store, AbilityIds.HEAL_LEAP);

        EliteMobsLogger.debug(LOGGER,
                "[AbilityTrigger] heal_leap triggered at %.0f%% health (threshold=%.0f%%) tier=%d",
                EliteMobsLogLevel.INFO, healthPercent * 100f, healLeap.triggerHealthPercent * 100f, tierIndex);

        return true;
    }

    private boolean tryTriggerChargeLeap(Ref<EntityStore> entityRef, Store<EntityStore> store, EliteMobsConfig config,
            int tierIndex
    ) {
        ChargeLeapAbilityComponent chargeLeap = store.getComponent(entityRef,
                                                                   plugin.getChargeLeapAbilityComponentType()
        );
        if (chargeLeap == null || !chargeLeap.abilityEnabled) return false;

        if (chargeLeap.cooldownTicksRemaining > 0) {
            LOGGER.atInfo().log("[ChargeLeap] BLOCKED by cooldown: remaining=%d ticks (%.1f sec)",
                    chargeLeap.cooldownTicksRemaining,
                                chargeLeap.cooldownTicksRemaining / (float) Constants.TICKS_PER_SECOND
            );
            return false;
        }

        EliteMobsCombatTrackingComponent combat = store.getComponent(entityRef,
                                                                     plugin.getCombatTrackingComponentType()
        );
        if (combat == null || !combat.isInCombat()) {
            LOGGER.atInfo().log("[ChargeLeap] BLOCKED: mob not in combat (state=%s)",
                    combat != null ? combat.state.name() : "null");
            return false;
        }

        Ref<EntityStore> targetRef = combat.getBestTarget();
        if (targetRef == null || !targetRef.isValid()) {
            LOGGER.atInfo().log("[ChargeLeap] BLOCKED: no valid target");
            return false;
        }

        if (combat.aiTarget == null || !combat.aiTarget.isValid()) {
            LOGGER.atInfo().log("[ChargeLeap] BLOCKED: no AI target (mob may be retreating, damage target still set)");
            return false;
        }

        EliteMobsConfig.ChargeLeapAbilityConfig abilityConfig = getChargeLeapConfig(config);
        if (abilityConfig == null) return false;

        float distance = calculateDistance(entityRef, targetRef, store);
        if (distance < abilityConfig.minRange || distance > abilityConfig.maxRange) {
            LOGGER.atInfo().log("[ChargeLeap] BLOCKED by distance: dist=%.1f (range=%.1f-%.1f)",
                                distance,
                                abilityConfig.minRange,
                                abilityConfig.maxRange
            );
            return false;
        }

        EliteMobAbilityStartedEvent startedEvent = new EliteMobAbilityStartedEvent(
                entityRef, AbilityIds.CHARGE_LEAP, tierIndex, targetRef
        );
        plugin.getEventBus().fire(startedEvent);
        if (startedEvent.isCancelled()) return false;

        boolean started = startAbilityChain(entityRef, store, AbilityIds.CHARGE_LEAP, tierIndex, config);
        if (!started) {
            LOGGER.atInfo().log("[ChargeLeap] chain failed to start for tier %d", tierIndex);
            return false;
        }

        long cooldownTicks = getCooldownTicks(abilityConfig.cooldownSecondsPerTier, tierIndex);
        chargeLeap.cooldownTicksRemaining = cooldownTicks;

        lockAbility(entityRef, store, AbilityIds.CHARGE_LEAP);

        LOGGER.atInfo().log("[ChargeLeap] TRIGGERED: dist=%.1f (range=%.1f-%.1f) tier=%d cooldown=%d ticks (%.1f sec)",
                distance, abilityConfig.minRange, abilityConfig.maxRange, tierIndex,
                            cooldownTicks,
                            cooldownTicks / (float) Constants.TICKS_PER_SECOND
        );

        return true;
    }

    private static final long SUMMON_SPAWN_DELAY_TICKS = 66;

    private void tryTriggerSummonUndead(Ref<EntityStore> entityRef, Store<EntityStore> store, EliteMobsConfig config,
            int tierIndex
    ) {
        SummonUndeadAbilityComponent summon = store.getComponent(entityRef,
                                                                 plugin.getSummonUndeadAbilityComponentType()
        );
        if (summon == null || !summon.abilityEnabled) return;

        if (summon.cooldownTicksRemaining > 0) return;

        EliteMobsConfig.SummonAbilityConfig abilityConfig = getSummonConfig(config);
        if (abilityConfig == null) return;

        EliteMobsSummonMinionTrackingComponent tracking = store.getComponent(entityRef,
                                                                             plugin.getSummonMinionTrackingComponentType()
        );
        if (tracking != null) {
            int maxAlive = Math.max(0, Math.min(50, abilityConfig.maxAlive));
            if (!tracking.canSummonMore(maxAlive)) {
                LOGGER.atInfo().log("[SummonUndead] BLOCKED: cap reached alive=%d max=%d",
                                    tracking.summonedAliveCount,
                                    maxAlive
                );
                return;
            }
        }

        String roleIdentifier = resolveSummonRole(entityRef, store, config);

        Ref<EntityStore> targetRef = getAggroTarget(entityRef, store);
        EliteMobAbilityStartedEvent startedEvent = new EliteMobAbilityStartedEvent(
                entityRef, AbilityIds.SUMMON_UNDEAD, tierIndex, targetRef
        );
        plugin.getEventBus().fire(startedEvent);
        if (startedEvent.isCancelled()) return;

        boolean started = startAbilityChain(entityRef, store, AbilityIds.SUMMON_UNDEAD, tierIndex, config);
        if (!started) {
            EliteMobsLogger.debug(LOGGER,
                    "[AbilityTrigger] summon_undead chain failed to start for tier %d",
                    EliteMobsLogLevel.WARNING, tierIndex);
            return;
        }

        summon.pendingSummonRole = roleIdentifier;
        summon.pendingSummonTicksRemaining = SUMMON_SPAWN_DELAY_TICKS;

        long cooldownTicks = getCooldownTicks(abilityConfig.cooldownSecondsPerTier, tierIndex);
        summon.cooldownTicksRemaining = cooldownTicks;

        lockAbility(entityRef, store, AbilityIds.SUMMON_UNDEAD);

        LOGGER.atInfo().log("[SummonUndead] TRIGGERED: tier=%d role=%s spawnDelay=%d cooldown=%d ticks",
                            tierIndex,
                            roleIdentifier,
                            SUMMON_SPAWN_DELAY_TICKS,
                            cooldownTicks
        );

    }

    private void handleAbilityCompletionRetrigger(EliteMobAbilityCompletedEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        String abilityId = event.getAbilityId();

        if (AbilityIds.CHARGE_LEAP.equals(abilityId)) {
            ChargeLeapAbilityComponent chargeLeap = store.getComponent(entityRef,
                                                                       plugin.getChargeLeapAbilityComponentType()
            );
            if (chargeLeap != null) {
                LOGGER.atInfo().log("[ChargeLeap] COMPLETED: cooldownRemaining=%d ticks (%.1f sec)",
                        chargeLeap.cooldownTicksRemaining,
                                    chargeLeap.cooldownTicksRemaining / (float) Constants.TICKS_PER_SECOND
                );
            }
        }

        unlockAbility(entityRef, store);

        if (AbilityIds.SUMMON_UNDEAD.equals(abilityId)) {
            EliteMobsTierComponent tier = store.getComponent(entityRef, plugin.getEliteMobsComponentType());
            if (tier != null && tier.tierIndex >= 0) {
                EliteMobsCombatTrackingComponent combat = store.getComponent(entityRef,
                                                                             plugin.getCombatTrackingComponentType()
                );
                if (combat != null && combat.isInCombat()) {
                    EliteMobsConfig config = plugin.getConfig();
                    if (config != null) {
                        int tierIndex = clampTierIndex(tier.tierIndex);
                        tryTriggerSummonUndead(entityRef, store, config, tierIndex);
                    }
                }
            }
        }
    }

    private void handleDeathInterrupt(EliteMobDeathEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        EliteMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock == null || !lock.isLocked()) return;

        String activeAbilityId = lock.activeAbilityId;
        int tierIndex = clampTierIndex(event.getTier());

        EliteMobAbilityInterruptedEvent interruptedEvent = new EliteMobAbilityInterruptedEvent(
                entityRef, activeAbilityId, tierIndex, "death"
        );
        plugin.getEventBus().fire(interruptedEvent);

        unlockAbility(entityRef, store);

        EliteMobsLogger.debug(LOGGER,
                "[AbilityTrigger] %s interrupted by death tier=%d",
                EliteMobsLogLevel.INFO, activeAbilityId, tierIndex);
    }

    private void handleDeaggroInterrupt(EliteMobDeaggroEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        LOGGER.atInfo().log("[Deaggro] Mob deaggro'd tier=%d", event.getTier());

        EliteMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock == null || !lock.isLocked()) return;

        String activeAbilityId = lock.activeAbilityId;

        EliteMobsTierComponent tier = store.getComponent(entityRef, plugin.getEliteMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        EliteMobAbilityInterruptedEvent interruptedEvent = new EliteMobAbilityInterruptedEvent(
                entityRef, activeAbilityId, tierIndex, "deaggro"
        );
        plugin.getEventBus().fire(interruptedEvent);

        unlockAbility(entityRef, store);

        EliteMobsLogger.debug(LOGGER,
                "[AbilityTrigger] %s interrupted by deaggro tier=%d",
                EliteMobsLogLevel.INFO, activeAbilityId, tierIndex);
    }

    private static final int HEAL_LEAP_INTERRUPT_HITS = 3;

    private boolean checkHealLeapInterrupt(EliteMobDamageReceivedEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return false;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return false;

        EliteMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock == null || !lock.isLocked()) return false;
        if (!AbilityIds.HEAL_LEAP.equals(lock.activeAbilityId)) return false;

        HealLeapAbilityComponent healLeap = store.getComponent(entityRef, plugin.getHealLeapAbilityComponentType());
        if (healLeap == null) return false;

        healLeap.hitsTaken++;
        LOGGER.atInfo().log("[HealLeap] Hit taken during ability: hitsTaken=%d/%d",
                healLeap.hitsTaken, HEAL_LEAP_INTERRUPT_HITS);

        if (healLeap.hitsTaken < HEAL_LEAP_INTERRUPT_HITS) {
            return true;
        }

        LOGGER.atInfo().log("[HealLeap] INTERRUPTED by %d hits, cancelling chain", healLeap.hitsTaken);

        NPCEntity npcEntity = store.getComponent(entityRef, Objects.requireNonNull(NPCEntity.getComponentType()));
        if (npcEntity != null) {
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, healLeap);
        }

        healLeap.hitsTaken = 0;

        EliteMobsConfig config = plugin.getConfig();
        EliteMobsTierComponent tier2 = store.getComponent(entityRef, plugin.getEliteMobsComponentType());
        int cancelTierIndex = (tier2 != null) ? clampTierIndex(tier2.tierIndex) : 0;

        if (npcEntity != null && npcEntity.getWorld() != null) {
            npcEntity.getWorld().execute(() -> {
                if (!entityRef.isValid()) return;
                var entityStoreProvider = npcEntity.getWorld().getEntityStore();
                if (entityStoreProvider == null) return;
                Store<EntityStore> worldStore = entityStoreProvider.getStore();
                StoreHelpers.withEntity(worldStore, entityRef, (_, commandBuffer, _) -> {
                    String cancelRootId = resolveCancelRootInteractionId(config, cancelTierIndex);
                    if (cancelRootId != null) {
                        EliteMobsAbilityFeatureHelpers.tryStartInteraction(entityRef,
                                                                           worldStore,
                                                                           commandBuffer,
                                                                           InteractionType.Ability2,
                                cancelRootId
                        );
                    } else {
                        AbilityHelpers.cancelInteractionType(worldStore, commandBuffer, entityRef,
                                                             InteractionType.Ability2
                        );
                    }
                });
            });
        }

        EliteMobsTierComponent tier = store.getComponent(entityRef, plugin.getEliteMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        EliteMobAbilityInterruptedEvent interruptedEvent = new EliteMobAbilityInterruptedEvent(
                entityRef, AbilityIds.HEAL_LEAP, tierIndex, "hit_cancel"
        );
        plugin.getEventBus().fire(interruptedEvent);

        unlockAbility(entityRef, store);

        return true;
    }

    private boolean startAbilityChain(
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            String abilityId,
            int tierIndex,
            EliteMobsConfig config
    ) {
        EliteMobsConfig.AbilityConfig abilityConfig = getAbilityConfig(config, abilityId);
        if (abilityConfig == null) {
            EliteMobsLogger.debug(LOGGER,
                    "[AbilityTrigger] No AbilityConfig found for abilityId=%s",
                    EliteMobsLogLevel.WARNING, abilityId);
            return false;
        }

        String rootInteractionTemplatePath = abilityConfig.templates.getTemplate(
                EliteMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION
        );
        if (rootInteractionTemplatePath == null || rootInteractionTemplatePath.isBlank()) {
            EliteMobsLogger.debug(LOGGER,
                    "[AbilityTrigger] No rootInteraction template for abilityId=%s",
                    EliteMobsLogLevel.WARNING, abilityId);
            return false;
        }

        String rootInteractionId = TemplateNameGenerator.getTemplateNameWithTierFromPath(
                rootInteractionTemplatePath, config, tierIndex
        );
        if (rootInteractionId == null || rootInteractionId.isBlank()) {
            EliteMobsLogger.debug(LOGGER,
                    "[AbilityTrigger] Failed to resolve root interaction id for abilityId=%s tier=%d",
                    EliteMobsLogLevel.WARNING, abilityId, tierIndex);
            return false;
        }

        NPCEntity npcEntity = store.getComponent(entityRef, Objects.requireNonNull(NPCEntity.getComponentType()));
        if (npcEntity == null || npcEntity.getWorld() == null) {
            EliteMobsLogger.debug(LOGGER,
                    "[AbilityTrigger] NPCEntity or World is null for abilityId=%s",
                    EliteMobsLogLevel.WARNING, abilityId);
            return false;
        }

        final String resolvedRootId = rootInteractionId;
        npcEntity.getWorld().execute(() -> {
            if (!entityRef.isValid()) return;

            EntityStore entityStoreProvider = npcEntity.getWorld().getEntityStore();
            if (entityStoreProvider == null) return;
            Store<EntityStore> worldStore = entityStoreProvider.getStore();

            StoreHelpers.withEntity(worldStore, entityRef, (_, commandBuffer, _) -> {
                EliteMobsAbilityLockComponent lock = worldStore.getComponent(entityRef,
                                                                             plugin.getAbilityLockComponentType()
                );

                if (AbilityIds.HEAL_LEAP.equals(abilityId)) {
                    performHealLeapWeaponSwap(entityRef, worldStore, npcEntity);
                }
                if (AbilityIds.SUMMON_UNDEAD.equals(abilityId)) {
                    performSummonSpellbookSwap(entityRef, worldStore, npcEntity);
                }

                boolean started;
                try {
                    started = EliteMobsAbilityFeatureHelpers.tryStartInteraction(entityRef,
                                                                                 worldStore,
                                                                                 commandBuffer,
                                                                                 InteractionType.Ability2,
                                                                                 resolvedRootId
                    );
                } catch (Exception e) {
                    LOGGER.atWarning().log("[AbilityTrigger] Chain start threw exception for rootId=%s: %s",
                                           resolvedRootId,
                                           e.getMessage()
                    );
                    started = false;
                }

                if (!started) {
                    if (lock != null && lock.isLocked()) {
                        lock.unlock();
                        commandBuffer.replaceComponent(entityRef, plugin.getAbilityLockComponentType(), lock);
                    }
                    if (AbilityIds.HEAL_LEAP.equals(abilityId)) {
                        AbilityHelpers.restorePreviousItemIfNeeded(npcEntity,
                                                                   worldStore.getComponent(entityRef,
                                                                                           plugin.getHealLeapAbilityComponentType()
                                                                   )
                        );
                    }
                    if (AbilityIds.SUMMON_UNDEAD.equals(abilityId)) {
                        AbilityHelpers.restoreSummonWeaponIfNeeded(npcEntity,
                                                                   worldStore.getComponent(entityRef,
                                                                                           plugin.getSummonUndeadAbilityComponentType()
                                                                   )
                        );
                    }
                    EliteMobsLogger.debug(LOGGER,
                            "[AbilityTrigger] Deferred chain start failed for rootId=%s",
                            EliteMobsLogLevel.WARNING, resolvedRootId);
                } else {
                    if (lock != null) {
                        lock.markChainStarted(plugin.getTickClock().getTick());
                        commandBuffer.replaceComponent(entityRef, plugin.getAbilityLockComponentType(), lock);
                    }

                    EliteMobsLogger.debug(LOGGER,
                            "[AbilityTrigger] Deferred chain started for rootId=%s tick=%d",
                            EliteMobsLogLevel.INFO, resolvedRootId, plugin.getTickClock().getTick());
                }
            });
        });

        return true;
    }

    private float calculateHealthPercent(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStats = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStats == null) return 1.0f;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return 1.0f;

        float current = healthStatValue.get();
        float max = healthStatValue.getMax();
        if (max <= 0) return 1.0f;

        return current / max;
    }

    private float calculateDistance(Ref<EntityStore> entityRef, Ref<EntityStore> targetRef, Store<EntityStore> store) {
        TransformComponent mobTransform = store.getComponent(entityRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());

        if (mobTransform == null || targetTransform == null) return Float.MAX_VALUE;

        Vector3d mobPos = mobTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();

        double dx = targetPos.getX() - mobPos.getX();
        double dy = targetPos.getY() - mobPos.getY();
        double dz = targetPos.getZ() - mobPos.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private @Nullable Ref<EntityStore> getAggroTarget(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EliteMobsCombatTrackingComponent combat = store.getComponent(entityRef,
                                                                     plugin.getCombatTrackingComponentType()
        );
        if (combat == null) return null;
        return combat.getBestTarget();
    }

    private void lockAbility(Ref<EntityStore> entityRef, Store<EntityStore> store, String abilityId) {
        EliteMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock != null) {
            lock.lock(abilityId);
        }
    }

    private void unlockAbility(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EliteMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock != null) {
            lock.unlock();
        }
    }

    private long getCooldownTicks(float[] cooldownSecondsPerTier, int tierIndex) {
        if (cooldownSecondsPerTier == null || tierIndex < 0 || tierIndex >= cooldownSecondsPerTier.length) {
            return 0L;
        }
        float seconds = cooldownSecondsPerTier[tierIndex];
        if (seconds <= 0f) return 0L;
        return (long) (seconds * Constants.TICKS_PER_SECOND);
    }

    private void performHealLeapWeaponSwap(
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            NPCEntity npcEntity
    ) {
        HealLeapAbilityComponent healLeap = store.getComponent(entityRef, plugin.getHealLeapAbilityComponentType());
        if (healLeap == null) return;

        EliteMobsConfig config = plugin.getConfig();
        EliteMobsConfig.HealLeapAbilityConfig healConfig = getHealLeapConfig(config);
        if (healConfig == null) return;

        String potionItemId = healConfig.npcDrinkItemId;
        if (potionItemId == null || potionItemId.isBlank()) {
            potionItemId = "Potion_Health_Greater";
        }

        boolean swapped = AbilityHelpers.swapToPotionInHand(npcEntity, healLeap, potionItemId);
        if (swapped) {
            LOGGER.atInfo().log("[HealLeap] Weapon swapped to potion '%s'", potionItemId);
        } else {
            LOGGER.atInfo().log("[HealLeap] Weapon swap failed (itemId=%s)", potionItemId);
        }
    }

    private void performSummonSpellbookSwap(Ref<EntityStore> entityRef, Store<EntityStore> store, NPCEntity npcEntity) {
        SummonUndeadAbilityComponent summon = store.getComponent(entityRef,
                                                                 plugin.getSummonUndeadAbilityComponentType()
        );
        if (summon == null) return;

        String staffItemId = "Weapon_Staff_Bone";

        boolean swapped = AbilityHelpers.swapToSpellbookInHand(npcEntity, summon, staffItemId);
        if (swapped) {
            LOGGER.atInfo().log("[SummonUndead] Weapon swapped to staff '%s'", staffItemId);
        } else {
            LOGGER.atInfo().log("[SummonUndead] Weapon swap failed (itemId=%s)", staffItemId);
        }
    }

    private EliteMobsConfig.@Nullable AbilityConfig getAbilityConfig(EliteMobsConfig config, String abilityId) {
        if (config.abilitiesConfig == null || config.abilitiesConfig.defaultAbilities == null) return null;
        return config.abilitiesConfig.defaultAbilities.get(abilityId);
    }

    private EliteMobsConfig.@Nullable ChargeLeapAbilityConfig getChargeLeapConfig(EliteMobsConfig config) {
        EliteMobsConfig.AbilityConfig raw = getAbilityConfig(config, AbilityIds.CHARGE_LEAP);
        return (raw instanceof EliteMobsConfig.ChargeLeapAbilityConfig c) ? c : null;
    }

    private EliteMobsConfig.@Nullable HealLeapAbilityConfig getHealLeapConfig(EliteMobsConfig config) {
        EliteMobsConfig.AbilityConfig raw = getAbilityConfig(config, AbilityIds.HEAL_LEAP);
        return (raw instanceof EliteMobsConfig.HealLeapAbilityConfig c) ? c : null;
    }

    private EliteMobsConfig.@Nullable SummonAbilityConfig getSummonConfig(EliteMobsConfig config) {
        EliteMobsConfig.AbilityConfig raw = getAbilityConfig(config, AbilityIds.SUMMON_UNDEAD);
        return (raw instanceof EliteMobsConfig.SummonAbilityConfig c) ? c : null;
    }

    private @NonNull String resolveSummonRole(Ref<EntityStore> entityRef, Store<EntityStore> store,
                                              EliteMobsConfig config) {
        NPCEntity npc = store.getComponent(entityRef, Objects.requireNonNull(NPCEntity.getComponentType()));
        if (npc == null) return "default";

        String roleName = npc.getRoleName();
        if (roleName == null || roleName.isBlank()) return "default";

        EliteMobsConfig.SummonAbilityConfig summonConfig = getSummonConfig(config);
        if (summonConfig == null || summonConfig.roleIdentifiers == null) return "default";

        String roleNameLower = roleName.toLowerCase(Locale.ROOT);
        for (String identifier : summonConfig.roleIdentifiers) {
            if (identifier == null || identifier.isBlank()) continue;
            if (roleNameLower.contains(identifier.toLowerCase(Locale.ROOT))) {
                return identifier;
            }
        }
        return "default";
    }

    private @Nullable String resolveCancelRootInteractionId(EliteMobsConfig config, int tierIndex) {
        if (config == null) return null;
        EliteMobsConfig.HealLeapAbilityConfig healConfig = getHealLeapConfig(config);
        if (healConfig == null) return null;
        String cancelTemplatePath = healConfig.templates.getTemplate(
                EliteMobsConfig.HealLeapAbilityConfig.TEMPLATE_ROOT_INTERACTION_CANCEL
        );
        if (cancelTemplatePath == null || cancelTemplatePath.isBlank()) return null;
        return TemplateNameGenerator.getTemplateNameWithTierFromPath(cancelTemplatePath, config, tierIndex);
    }
}

enum AbilityTriggerSource {
    AGGRO, DAMAGE_RECEIVED
}
