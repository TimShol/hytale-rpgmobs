package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonMinionTrackingComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.rules.AbilityGateEvaluator;
import com.frotty27.elitemobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.Random;

public final class EliteMobsUndeadSummonAbilityFeature implements IEliteMobsAbilityFeature {

    public static final String ABILITY_UNDEAD_SUMMON = AbilityIds.SUMMON_UNDEAD;

    private final Random random = new Random();

    @Override
    public String getFeatureKey() {
        return "UndeadSummon";
    }

    @Override
    public String id() {
        return ABILITY_UNDEAD_SUMMON;
    }

    
    @Override
    public void apply(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        
        EliteMobsConfig.SummonAbilityConfig abilityConfig =
            (EliteMobsConfig.SummonAbilityConfig) config.abilitiesConfig.defaultAbilities.get(AbilityIds.SUMMON_UNDEAD);

        if (abilityConfig == null) {
            return;
        }

        int tierIndex = tierComponent.tierIndex;

        if (!AbilityGateEvaluator.isAllowed(abilityConfig, roleName, "", tierIndex)) {
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

        commandBuffer.putComponent(npcRef, plugin.getSummonUndeadAbilityComponentType(), component);

        EliteMobsSummonMinionTrackingComponent trackingComponent = EliteMobsSummonMinionTrackingComponent.forParent();
        commandBuffer.putComponent(npcRef, plugin.getSummonMinionTrackingComponentType(), trackingComponent);

    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
    }
}
