package com.frotty27.elitemobs.systems.death;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public final class EliteMobsVanillaDropsCullSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String WEAPON_PREFIX = "weapon_";
    private static final String ARMOR_PREFIX = "armor_";

    private final EliteMobsPlugin eliteMobsPlugin;
    private final EliteMobsDropsCullHandler cullHandler = new EliteMobsDropsCullHandler(this);

    public EliteMobsVanillaDropsCullSystem(EliteMobsPlugin eliteMobsPlugin) {
        this.eliteMobsPlugin = eliteMobsPlugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                ItemComponent.getComponentType(),
                TransformComponent.getComponentType()
        );
    }

    @Override
    public void tick(
            float deltaTimeSeconds,
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        cullHandler.handle(entityIndex, archetypeChunk, commandBuffer);
    }

    void processTick(int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> itemEntityRef = archetypeChunk.getReferenceTo(entityIndex);

        ComponentType<EntityStore, ItemComponent> itemComponentType = ItemComponent.getComponentType();
        ComponentType<EntityStore, TransformComponent> transformComponentType = TransformComponent.getComponentType();

        ItemComponent itemComponent = archetypeChunk.getComponent(entityIndex, itemComponentType);
        TransformComponent transformComponent = archetypeChunk.getComponent(entityIndex, transformComponentType);
        if (itemComponent == null || transformComponent == null) return;

        if (!eliteMobsPlugin.getMobDropsCleanupManager().shouldCull(transformComponent.getPosition())) return;

        ItemStack itemStack = itemComponent.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) return;

        String itemId = itemStack.getItemId();
        if (itemId.isBlank()) return;

        String itemIdLowercase = itemId.toLowerCase(Locale.ROOT);
        boolean isWeaponOrArmorDrop =
                itemIdLowercase.startsWith(WEAPON_PREFIX) || itemIdLowercase.startsWith(ARMOR_PREFIX);
        if (!isWeaponOrArmorDrop) return;

        EliteMobsConfig config = eliteMobsPlugin.getConfig();
        if (config != null && config.debugConfig.isDebugModeEnabled) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "CULLING item=%s pos=%s",
                    EliteMobsLogLevel.INFO,
                    itemId,
                    transformComponent.getPosition()
            );
        }

        commandBuffer.removeEntity(itemEntityRef, RemoveReason.REMOVE);
    }

}
