package com.frotty27.elitemobs.utils;

import com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import java.util.Random;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clamp01;

public final class AbilityHelpers {

    private AbilityHelpers() {
    }

    public static @Nullable RootInteraction getRootInteraction(@Nullable String rootInteractionId) {
        if (rootInteractionId == null || rootInteractionId.isBlank()) return null;
        return RootInteraction.getAssetMap().getAsset(rootInteractionId);
    }


    public static boolean isInteractionTypeRunning(
            Store<EntityStore> entityStore,
            Ref<EntityStore> npcRef,
            InteractionType interactionType
    ) {
        ComponentType<EntityStore, InteractionManager> interactionManagerComponentType =
                InteractionModule.get().getInteractionManagerComponent();
        InteractionManager interactionManager = entityStore.getComponent(npcRef, interactionManagerComponentType);
        if (interactionManager == null) return false;
        var chains = interactionManager.getChains();
        if (chains == null || chains.isEmpty()) return false;
        for (InteractionChain chain : chains.values()) {
            if (chain != null && chain.getType() == interactionType) return true;
        }
        return false;
    }

    public static void cancelInteractionType(
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            Ref<EntityStore> npcRef,
            InteractionType interactionType
    ) {
        ComponentType<EntityStore, InteractionManager> interactionManagerComponentType =
                InteractionModule.get().getInteractionManagerComponent();
        InteractionManager interactionManager = entityStore.getComponent(npcRef, interactionManagerComponentType);
        if (interactionManager == null) return;

        var chains = interactionManager.getChains();
        if (chains == null || chains.isEmpty()) return;

        for (InteractionChain chain : chains.values()) {
            if (chain != null && chain.getType() == interactionType) {
                interactionManager.cancelChains(chain);
            }
        }
        commandBuffer.replaceComponent(npcRef, interactionManagerComponentType, interactionManager);
    }

    public static boolean swapToPotionInHand(NPCEntity npcEntity, HealLeapAbilityComponent healLeapAbility,
            String potionItemId
    ) {
        if (npcEntity == null || healLeapAbility == null) return false;
        if (potionItemId == null || potionItemId.isBlank()) return false;
        if (healLeapAbility.swapActive) return false;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return false;

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot == Inventory.INACTIVE_SLOT_INDEX) activeSlot = 0;

        ItemStack previousItem = inventory.getHotbar().getItemStack(activeSlot);

        ItemStack potionItem = new ItemStack(potionItemId, 1);
        inventory.getHotbar().setItemStackForSlot(activeSlot, potionItem);
        inventory.markChanged();

        healLeapAbility.swapActive = true;
        healLeapAbility.swapSlot = activeSlot;
        healLeapAbility.swapPreviousItem = previousItem;

        return true;
    }

    public static void restorePreviousItemIfNeeded(NPCEntity npcEntity, HealLeapAbilityComponent healLeapAbility
    ) {
        if (npcEntity == null || healLeapAbility == null) return;
        if (!healLeapAbility.swapActive) return;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        byte slot = healLeapAbility.swapSlot;
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;

        ItemStack previous = healLeapAbility.swapPreviousItem;
        inventory.getHotbar().setItemStackForSlot(slot, previous);
        inventory.markChanged();

        healLeapAbility.swapActive = false;
        healLeapAbility.swapSlot = -1;
        healLeapAbility.swapPreviousItem = null;
    }

    public static boolean swapToSpellbookInHand(NPCEntity npcEntity, SummonUndeadAbilityComponent summonAbility,
                                                String spellbookItemId) {
        if (npcEntity == null || summonAbility == null) return false;
        if (spellbookItemId == null || spellbookItemId.isBlank()) return false;
        if (summonAbility.swapActive) return false;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return false;

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot == Inventory.INACTIVE_SLOT_INDEX) activeSlot = 0;

        ItemStack previousItem = inventory.getHotbar().getItemStack(activeSlot);

        ItemStack spellbook = new ItemStack(spellbookItemId, 1);
        inventory.getHotbar().setItemStackForSlot(activeSlot, spellbook);
        inventory.markChanged();

        summonAbility.swapActive = true;
        summonAbility.swapSlot = activeSlot;
        summonAbility.swapPreviousItem = previousItem;

        return true;
    }

    public static void restoreSummonWeaponIfNeeded(NPCEntity npcEntity, SummonUndeadAbilityComponent summonAbility) {
        if (npcEntity == null || summonAbility == null) return;
        if (!summonAbility.swapActive) return;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        byte slot = summonAbility.swapSlot;
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;

        ItemStack previous = summonAbility.swapPreviousItem;
        inventory.getHotbar().setItemStackForSlot(slot, previous);
        inventory.markChanged();

        summonAbility.swapActive = false;
        summonAbility.swapSlot = -1;
        summonAbility.swapPreviousItem = null;
    }

    public static float rollPercentInRange(
            Random random,
            float minPercent,
            float maxPercent,
            float fallback
    ) {
        if (random == null) return fallback;

        float min = clamp01(minPercent);
        float max = clamp01(maxPercent);
        if (max < min) {
            float tmp = min;
            min = max;
            max = tmp;
        }
        if (max <= 0f) return Math.max(0.01f, fallback);
        if (max == min) return max;
        return min + random.nextFloat() * (max - min);
    }
}
