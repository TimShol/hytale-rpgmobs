package com.frotty27.elitemobs.systems.death;

import com.frotty27.elitemobs.api.events.EliteMobDeathEvent;
import com.frotty27.elitemobs.api.events.EliteMobDropsEvent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.combat.EliteMobsCombatTrackingComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonMinionTrackingComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonedMinionComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.exceptions.EliteMobsException;
import com.frotty27.elitemobs.exceptions.EliteMobsSystemException;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.utils.Constants;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.elitemobs.utils.Constants.UTILITY_SLOT_INDEX;
import static com.frotty27.elitemobs.utils.InventoryHelpers.copyExactSingle;

public final class EliteMobsDeathSystem extends DeathSystems.OnDeathSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double CLEANUP_RADIUS_BLOCKS = 2.0;
    private static final double DROP_SPAWN_Y_OFFSET = 1.0;
    private static final long CULL_WINDOW_TICKS = 2L;
    private static final long MIN_MOB_DROPS_SPAWN_DELAY_TICKS = CULL_WINDOW_TICKS + 1;

    private static final double[] EXTRA_DROPS_DELAY_SECONDS_BY_TIER = {0.0, 0.0, 0.0, 0.5, 1.0};

    private final EliteMobsPlugin plugin;
    private final Random random = new Random();
    private final EliteMobsDropsHandler dropsHandler = new EliteMobsDropsHandler(this);
    private final EliteMobsMinionDeathHandler minionDeathHandler = new EliteMobsMinionDeathHandler(this);

    public EliteMobsDeathSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType(), DeathComponent.getComponentType());
    }

    @Override
    public void onComponentAdded(@NonNull Ref<EntityStore> ref, @NonNull DeathComponent death,
                                 @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        try {
            processDeath(ref, death, store, commandBuffer);
        } catch (EliteMobsException e) {
            throw e;
        } catch (Exception e) {
            throw new EliteMobsSystemException("Error in EliteMobsDeathSystem", e);
        }
    }

    private void processDeath(
            Ref<EntityStore> ref,
            DeathComponent death,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
    ) {
        if (minionDeathHandler.handle(ref, death, store, commandBuffer)) {
            return;
        }

        dropsHandler.handle(ref, death, store);
    }

    void processOnDeath(Ref<EntityStore> ref, DeathComponent death, Store<EntityStore> store) {
        EliteMobsConfig cfg = plugin.getConfig();
        if (cfg == null) return;

        NPCEntity npc = store.getComponent(ref, Objects.requireNonNull(NPCEntity.getComponentType()));
        if (npc == null) return;

        EliteMobsTierComponent tier = store.getComponent(ref, plugin.getEliteMobsComponentType());
        if (tier == null || tier.tierIndex < 0) return;


        EliteMobsSummonMinionTrackingComponent tracking = store.getComponent(ref,
                                                                             plugin.getSummonMinionTrackingComponentType()
        );
        if (tracking != null && tracking.disableDrops) return;

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        if (transformComponent == null || headRotation == null) return;

        var spawnSystem = plugin.getSpawnSystem();
        if (spawnSystem != null) {
            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent != null) {
                UUID deadSummonerId = uuidComponent.getUuid();
                long deathTick = plugin.getTickClock().getTick();
                int aliveCount = tracking != null ? tracking.summonedAliveCount : 0;
                LOGGER.atInfo().log(
                        "[DeathSystem] Summoner died, queuing minion chain despawn for summonerId=%s alive=%d",
                        deadSummonerId,
                        aliveCount
                );
                spawnSystem.queueSummonerDeath(deadSummonerId, deathTick);
            }
        }

        int tierId = clampTierIndex(tier.tierIndex);
        death.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);

        plugin.getMobDropsCleanupManager().addCullZone(
                transformComponent.getPosition().clone(),
                CLEANUP_RADIUS_BLOCKS,
                CULL_WINDOW_TICKS
        );

        ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();
        Inventory inv = npc.getInventory();
        if (inv != null) {
            addWeaponDrop(cfg, inv, drops);
            addArmorDrops(cfg, inv, drops);
            addUtilityDrop(cfg, inv, drops);
        }

        addExtraDrops(cfg, tierId, drops);

        var pos = transformComponent.getPosition().clone().add(0.0, DROP_SPAWN_Y_OFFSET, 0.0);
        var rot = headRotation.getRotation().clone();
        String roleName = npc.getRoleName() != null ? npc.getRoleName() : "";


        EliteMobsCombatTrackingComponent combatTracking = store.getComponent(ref,
                                                                             plugin.getCombatTrackingComponentType()
        );
        Ref<EntityStore> killerRef = (combatTracking != null) ? combatTracking.getBestTarget() : null;
        if (killerRef != null && !killerRef.isValid()) killerRef = null;
        plugin.getEventBus().fire(new EliteMobDeathEvent(
                ref, tierId, roleName, killerRef, transformComponent.getPosition().clone()));

        if (drops.isEmpty()) return;


        var dropsEvent = new EliteMobDropsEvent(
                ref, tierId, roleName, drops, pos.clone());
        plugin.getEventBus().fire(dropsEvent);
        if (dropsEvent.isCancelled() || drops.isEmpty()) return;

        double seconds = 0.0;
        if (EXTRA_DROPS_DELAY_SECONDS_BY_TIER.length > tierId) {
            seconds = Math.max(0.0, EXTRA_DROPS_DELAY_SECONDS_BY_TIER[tierId]);
        }

        long requestedDelayTicks = Math.round(seconds * Constants.TICKS_PER_SECOND);
        long delayTicks = Math.max(MIN_MOB_DROPS_SPAWN_DELAY_TICKS, requestedDelayTicks);

        plugin.getExtraDropsScheduler().enqueueDrops(delayTicks, pos, rot, drops, null);
    }

    private void addWeaponDrop(EliteMobsConfig cfg, Inventory inv, List<ItemStack> drops) {
        double chance = cfg.lootConfig.dropWeaponChance;
        if (random.nextDouble() > chance) return;
        byte slot = inv.getActiveHotbarSlot();
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;
        ItemStack mainHand = inv.getHotbar().getItemStack(slot);
        if (mainHand != null && !mainHand.isEmpty()) {
            drops.add(copyExactSingle(mainHand));
        }
    }

    private void addArmorDrops(EliteMobsConfig cfg, Inventory inv, List<ItemStack> drops) {
        double chance = cfg.lootConfig.dropArmorPieceChance;
        for (ItemArmorSlot slot : ItemArmorSlot.values()) {
            if (random.nextDouble() > chance) continue;
            ItemStack item = inv.getArmor().getItemStack((short) slot.ordinal());
            if (item != null && !item.isEmpty()) {
                drops.add(copyExactSingle(item));
            }
        }
    }

    private void addUtilityDrop(EliteMobsConfig cfg, Inventory inv, List<ItemStack> drops) {
        double chance = cfg.lootConfig.dropOffhandItemChance;
        if (random.nextDouble() > chance) return;
        ItemStack utility = inv.getHotbar().getItemStack((short) UTILITY_SLOT_INDEX);
        if (utility != null && !utility.isEmpty()) {
            drops.add(copyExactSingle(utility));
        }
    }

    private void addExtraDrops(EliteMobsConfig cfg, int tierId, List<ItemStack> drops) {
        List<EliteMobsConfig.ExtraDropRule> rules = cfg.lootConfig.defaultExtraDrops;
        if (rules == null || rules.isEmpty()) return;

        for (EliteMobsConfig.ExtraDropRule rule : rules) {
            if (rule == null) continue;
            if (rule.itemId == null || rule.itemId.isBlank()) continue;
            if (tierId < rule.minTierInclusive || tierId > rule.maxTierInclusive) continue;
            if (rule.chance <= 0.0) continue;
            if (rule.chance < 1.0 && random.nextDouble() > rule.chance) continue;

            int min = Math.max(1, rule.minQty);
            int max = Math.max(min, rule.maxQty);
            int qty = (min == max) ? min : min + random.nextInt((max - min) + 1);

            drops.add(new ItemStack(rule.itemId, qty));
        }
    }

    void decrementSummonerAliveCount(NPCEntity npc, EliteMobsSummonedMinionComponent minion, Store<EntityStore> store,
                                     CommandBuffer<EntityStore> commandBuffer) {
        if (minion.summonerId == null) return;
        var world = npc.getWorld();
        if (world == null) return;
        Ref<EntityStore> summonerRef = world.getEntityRef(minion.summonerId);
        if (summonerRef == null || !summonerRef.isValid()) return;
        EliteMobsSummonMinionTrackingComponent summonerTracking = store.getComponent(summonerRef,
                                                                                     plugin.getSummonMinionTrackingComponentType()
        );
        if (summonerTracking == null) return;
        summonerTracking.decrementCount();
        commandBuffer.replaceComponent(summonerRef, plugin.getSummonMinionTrackingComponentType(), summonerTracking);
    }

    ComponentType<EntityStore, EliteMobsSummonedMinionComponent> getSummonedMinionComponentType() {
        return plugin.getSummonedMinionComponentType();
    }
}
