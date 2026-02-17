package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.config.EliteMobsConfig.AbilityConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.SummonAbilityConfig;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsAbilityFeatureHelpers {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String DEFAULT_IDENTIFIER = "default";

    private EliteMobsAbilityFeatureHelpers() {
    }

    public static String resolveSummonRoleIdentifier(AbilityConfig summonConfig, String roleName) {
        if (!(summonConfig instanceof SummonAbilityConfig s)) return DEFAULT_IDENTIFIER;
        if (roleName == null || roleName.isBlank()) return DEFAULT_IDENTIFIER;
        if (s.roleIdentifiers == null || s.roleIdentifiers.isEmpty()) return DEFAULT_IDENTIFIER;

        String roleLower = roleName.toLowerCase();
        for (String identifier : s.roleIdentifiers) {
            if (identifier == null || identifier.isBlank()) continue;
            if (roleLower.contains(identifier.toLowerCase())) {
                return identifier;
            }
        }
        return DEFAULT_IDENTIFIER;
    }

    public static boolean tryStartInteraction(Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                       CommandBuffer<EntityStore> commandBuffer, InteractionType interactionType,
                                       String rootInteractionId) {
        RootInteraction rootInteraction = AbilityHelpers.getRootInteraction(rootInteractionId);
        if (rootInteraction == null) {
            LOGGER.atWarning().log("[tryStartInteraction] rootInteraction not found for id='%s'", rootInteractionId);
            return false;
        }

        ComponentType<EntityStore, InteractionManager> interactionManagerComponentType = InteractionModule.get().getInteractionManagerComponent();

        InteractionManager interactionManager = entityStore.getComponent(npcRef, interactionManagerComponentType);
        if (interactionManager == null) {
            LOGGER.atWarning().log("[tryStartInteraction] InteractionManager is null for rootId='%s'", rootInteractionId);
            return false;
        }

        var chains = interactionManager.getChains();
        if (!chains.isEmpty()) {
            for (var entry : chains.entrySet()) {
                InteractionChain chain = entry.getValue();
                if (chain != null) {
                    EliteMobsLogger.debug(LOGGER,
                                          "[tryStartInteraction] Active chain: type=%s for rootId='%s'",
                                          EliteMobsLogLevel.INFO,
                            chain.getType() != null ? chain.getType().name() : "null",
                            rootInteractionId);
                }
            }
        } else {
            EliteMobsLogger.debug(LOGGER,
                                  "[tryStartInteraction] No active chains before starting '%s'",
                                  EliteMobsLogLevel.INFO,
                                  rootInteractionId
            );
        }

        if (!chains.isEmpty()) {
            for (InteractionChain chain : chains.values()) {
                if (chain != null) {
                    EliteMobsLogger.debug(LOGGER,
                                          "[tryStartInteraction] Pre-cancelling %s chain before starting '%s'",
                                          EliteMobsLogLevel.INFO,
                            chain.getType() != null ? chain.getType().name() : "null", rootInteractionId);
                    interactionManager.cancelChains(chain);
                }
            }
        }

        InteractionContext interactionContext = InteractionContext.forInteraction(interactionManager,
                                                                                  npcRef,
                                                                                  interactionType,
                                                                                  entityStore
        );

        boolean started = interactionManager.tryStartChain(npcRef,
                                                           commandBuffer,
                                                           interactionType,
                                                           interactionContext,
                                                           rootInteraction
        );

        if (!started) {
            LOGGER.atWarning().log("[tryStartInteraction] tryStartChain returned false for rootId='%s' type=%s",
                    rootInteractionId, interactionType.name());
        }

        commandBuffer.replaceComponent(npcRef, interactionManagerComponentType, interactionManager);

        return started;
    }
}
