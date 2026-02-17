package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent;
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

public final class EliteMobsHealLeapAbilityFeature implements IEliteMobsAbilityFeature {

    public static final String ABILITY_HEAL_LEAP = AbilityIds.HEAL_LEAP;

    private final Random random = new Random();

    @Override
    public String getFeatureKey() {
        return "HealLeap";
    }

    @Override
    public String id() {
        return ABILITY_HEAL_LEAP;
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
        
        EliteMobsConfig.HealLeapAbilityConfig abilityConfig =
            (EliteMobsConfig.HealLeapAbilityConfig) config.abilitiesConfig.defaultAbilities.get(AbilityIds.HEAL_LEAP);

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

        float triggerHealthPercent = abilityConfig.minHealthTriggerPercent +
            random.nextFloat() * (abilityConfig.maxHealthTriggerPercent - abilityConfig.minHealthTriggerPercent);

        HealLeapAbilityComponent component = new HealLeapAbilityComponent();
        component.abilityEnabled = enabled;
        component.triggerHealthPercent = triggerHealthPercent;
        component.cooldownTicksRemaining = 0L;

        commandBuffer.putComponent(npcRef, plugin.getHealLeapAbilityComponentType(), component);
    }
}
