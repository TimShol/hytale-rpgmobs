package com.frotty27.elitemobs.systems.combat;

import com.frotty27.elitemobs.api.events.EliteMobAggroEvent;
import com.frotty27.elitemobs.api.events.EliteMobDeaggroEvent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.combat.EliteMobsCombatTrackingComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonedMinionComponent;
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
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class EliteMobsAITargetPollingSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long AI_POLL_INTERVAL_TICKS = 5;

    private final EliteMobsPlugin plugin;

    public EliteMobsAITargetPollingSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(plugin.getEliteMobsComponentType(), plugin.getCombatTrackingComponentType()
        );
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> mobRef = chunk.getReferenceTo(entityIndex);

        EliteMobsCombatTrackingComponent combat = chunk.getComponent(entityIndex,
                                                                     plugin.getCombatTrackingComponentType()
        );
        if (combat == null) return;

        long currentTick = plugin.getTickClock().getTick();

        if (currentTick - combat.lastTargetUpdateTick >= AI_POLL_INTERVAL_TICKS) {
            combat.lastTargetUpdateTick = currentTick;

            NPCEntity npc = store.getComponent(mobRef, Objects.requireNonNull(NPCEntity.getComponentType()));
            if (npc != null) {
                Ref<EntityStore> aiTarget = getAITarget(npc);

                // Filter out own-minion targets — prevents summoner from aggro-ing its minions
                if (aiTarget != null && isOwnMinion(store, mobRef, aiTarget)) {
                    aiTarget = null;
                }

                if (aiTarget != null) {
                    boolean wasIdle = combat.state == EliteMobsCombatTrackingComponent.CombatState.IDLE;
                    combat.updateAITarget(aiTarget);

                    if (wasIdle) {
                        EliteMobsTierComponent tier = store.getComponent(mobRef, plugin.getEliteMobsComponentType());
                        if (tier != null) {
                            combat.transitionToInCombat(aiTarget, currentTick);
                            commandBuffer.replaceComponent(mobRef, plugin.getCombatTrackingComponentType(), combat);

                            String roleName = npc.getRoleName();
                            plugin.getEventBus().fire(new EliteMobAggroEvent(
                                mobRef,
                                aiTarget,
                                tier.tierIndex,
                                roleName != null ? roleName : ""
                            ));

                            if (plugin.getConfig().debugConfig.isDebugModeEnabled) {
                                EliteMobsLogger.debug(
                                    LOGGER,
                                    "Combat state: %s IDLE → IN_COMBAT (AI marker acquired) tier=%d",
                                    EliteMobsLogLevel.INFO,
                                    npc.getRoleName(),
                                    tier.tierIndex
                                );
                            }
                        }
                    } else {
                        commandBuffer.replaceComponent(mobRef, plugin.getCombatTrackingComponentType(), combat);
                    }
                } else if (combat.state == EliteMobsCombatTrackingComponent.CombatState.IN_COMBAT) {
                    combat.transitionToIdle(currentTick);
                    commandBuffer.replaceComponent(mobRef, plugin.getCombatTrackingComponentType(), combat);

                    EliteMobsTierComponent deaggroTier = store.getComponent(mobRef, plugin.getEliteMobsComponentType());
                    int deaggroTierIndex = (deaggroTier != null) ? deaggroTier.tierIndex : 0;
                    String deaggroRole = npc.getRoleName();
                    plugin.getEventBus().fire(new EliteMobDeaggroEvent(
                        mobRef, deaggroTierIndex, deaggroRole != null ? deaggroRole : ""
                    ));

                    if (plugin.getConfig().debugConfig.isDebugModeEnabled) {
                        EliteMobsLogger.debug(
                            LOGGER,
                            "Combat state: %s IN_COMBAT → IDLE (AI marker lost)",
                            EliteMobsLogLevel.INFO,
                            npc.getRoleName()
                        );
                    }
                }
            }
        }

    }

    private @Nullable Ref<EntityStore> getAITarget(NPCEntity npc) {
        Role role = npc.getRole();
        if (role == null) return null;

        MarkedEntitySupport markedEntitySupport = role.getMarkedEntitySupport();
        if (markedEntitySupport == null) return null;

        String[] primaryKeys = {"LockedTarget", "Target", "CombatTarget"};
        for (String key : primaryKeys) {
            Ref<EntityStore> target = markedEntitySupport.getMarkedEntityRef(key);
            if (target != null && target.isValid()) {
                return target;
            }
        }

        return null;
    }

    /**
     * Returns true if the target is a summoned minion belonging to the mob at mobRef.
     * This prevents summoners from targeting (and aggro-ing on) their own minions.
     */
    private boolean isOwnMinion(Store<EntityStore> store, Ref<EntityStore> mobRef, Ref<EntityStore> targetRef) {
        EliteMobsSummonedMinionComponent targetMinion = store.getComponent(targetRef,
                                                                           plugin.getSummonedMinionComponentType()
        );
        if (targetMinion == null || targetMinion.summonerId == null) return false;

        UUIDComponent mobUuid = store.getComponent(mobRef, UUIDComponent.getComponentType());
        return mobUuid != null && targetMinion.summonerId.equals(mobUuid.getUuid());
    }
}
