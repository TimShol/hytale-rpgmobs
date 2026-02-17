package com.frotty27.elitemobs.equipment;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.util.InventoryHelper;

import java.util.*;

import static com.frotty27.elitemobs.utils.ClampingHelpers.*;
import static com.frotty27.elitemobs.utils.Constants.*;
import static com.frotty27.elitemobs.utils.InventoryHelpers.getContainerSizeSafe;

public final class EliteMobsEquipmentService {

    private static final String SHIELD_TOKEN = "weapon_shield";
    private static final String ARMOR_PREFIX = "Armor_";

    private final Random random = new Random();

    public void buildAndApply(NPCEntity npcEntity, EliteMobsConfig config, int tierIndex,
                              EliteMobsConfig.MobRule mobRule) {
        if (npcEntity == null || config == null || mobRule == null) return;

        int clampedTierIndex = clampTierIndex(tierIndex);

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) {
            inventory = new Inventory();
            npcEntity.setInventory(inventory);
        }

        boolean inventoryChanged = false;

        inventoryChanged |= equipArmor(inventory.getArmor(), config, clampedTierIndex);

        
        ItemStack chosenWeapon = maybePickWeapon(inventory, config, clampedTierIndex, mobRule);
        if (chosenWeapon != null) {
            setInHand(inventory, chosenWeapon);
            inventoryChanged = true;
        }

        
        inventoryChanged |= applyInHandDurability(inventory,
                                                  config.gearConfig.spawnGearDurabilityMin,
                                                  config.gearConfig.spawnGearDurabilityMax
        );

        
        ItemStack chosenShield = maybeEquipUtilityShield(inventory, config, clampedTierIndex);
        if (chosenShield != null) inventoryChanged = true;

        if (inventoryChanged) inventory.markChanged();
    }

    private boolean equipArmor(ItemContainer armorContainer, EliteMobsConfig config, int tierIndex) {
        if (armorContainer == null) return false;

        if (config.gearConfig.armorPiecesToEquipPerTier == null || config.gearConfig.armorPiecesToEquipPerTier.length <= tierIndex) {
            return false;
        }

        int armorSlotsToFill = clampArmorSlots(config.gearConfig.armorPiecesToEquipPerTier[tierIndex]);
        if (armorSlotsToFill == 0) return false;

        String wantedRarity = pickRarityForTier(config, tierIndex);

        List<String> armorMaterials = pickArmorMaterialsOfRarity(config, wantedRarity);
        if (armorMaterials.isEmpty()) {
            for (String allowedRarity : allowedRaritiesForTier(config, tierIndex)) {
                armorMaterials = pickArmorMaterialsOfRarity(config, allowedRarity);
                if (!armorMaterials.isEmpty()) break;
            }
        }
        if (armorMaterials.isEmpty()) return false;

        enum ArmorSlot {
            HEAD("Head"), CHEST("Chest"), HANDS("Hands"), LEGS("Legs");

            final String itemIdSuffix;

            ArmorSlot(String itemIdSuffix) {
                this.itemIdSuffix = itemIdSuffix;
            }
        }

        List<ArmorSlot> availableSlots = new ArrayList<>(List.of(ArmorSlot.values()));
        Collections.shuffle(availableSlots, random);

        boolean changed = false;

        int slotsToEquip = Math.min(armorSlotsToFill, availableSlots.size());
        for (int slotIndex = 0; slotIndex < slotsToEquip; slotIndex++) {
            ArmorSlot armorSlot = availableSlots.get(slotIndex);
            String material = armorMaterials.get(random.nextInt(armorMaterials.size()));

            String itemId = ARMOR_PREFIX + material + "_" + armorSlot.itemIdSuffix;
            if (Item.getAssetMap().getAsset(itemId) == null) continue;

            ItemStack armorPiece = new ItemStack(itemId, 1);
            armorPiece = withRandomDurabilityFraction(armorPiece,
                                                      config.gearConfig.spawnGearDurabilityMin,
                                                      config.gearConfig.spawnGearDurabilityMax
            );

            InventoryHelper.useArmor(armorContainer, armorPiece);
            changed = true;
        }

        return changed;
    }

    private ItemStack maybePickWeapon(Inventory inventory, EliteMobsConfig config, int tierIndex,
                                      EliteMobsConfig.MobRule mobRule) {
        if (!mobRule.enabled) return null;

        if (mobRule.enableWeaponOverrideForTier == null || mobRule.enableWeaponOverrideForTier.length < TIERS_AMOUNT) {
            return null;
        }

        if (!mobRule.enableWeaponOverrideForTier[tierIndex]) return null;

        ItemStack itemInHand = inventory.getItemInHand();
        boolean isHandEmpty = (itemInHand == null || itemInHand.isEmpty());
        boolean isHandShield = !isHandEmpty && isShieldItemId(itemInHand.getItemId());

        boolean shouldEquipWeapon = switch (mobRule.weaponOverrideMode) {
            case NONE -> false;
            case ONLY_IF_EMPTY -> isHandEmpty || isHandShield;
            case ALWAYS -> true;
        };
        if (!shouldEquipWeapon) return null;

        String weaponItemId = pickWeaponForRuleAndTier(config, mobRule, tierIndex);
        if (weaponItemId == null || weaponItemId.isBlank()) {
            if (isHandEmpty) {
                int fallbackTier = Math.max(0, tierIndex - 1);
                if (fallbackTier != tierIndex) {
                    weaponItemId = pickWeaponForRuleAndTier(config, mobRule, fallbackTier);
                }
            }
            if (weaponItemId == null || weaponItemId.isBlank()) return null;
        }

        ItemStack weapon = new ItemStack(weaponItemId, 1);
        return withRandomDurabilityFraction(weapon, config.gearConfig.spawnGearDurabilityMin, config.gearConfig.spawnGearDurabilityMax);
    }

    private boolean applyInHandDurability(Inventory inventory, double minFraction, double maxFraction) {
        ItemStack itemInHand = inventory.getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty()) return false;
        if (itemInHand.getMaxDurability() <= 0) return false;

        ItemStack updatedInHand = withRandomDurabilityFraction(itemInHand, minFraction, maxFraction);

        byte activeHotbarSlot = inventory.getActiveHotbarSlot();
        if (activeHotbarSlot == Inventory.INACTIVE_SLOT_INDEX) return false;

        inventory.getHotbar().setItemStackForSlot(activeHotbarSlot, updatedInHand);
        return true;
    }

    private ItemStack withRandomDurabilityFraction(ItemStack itemStack, double minFraction, double maxFraction) {
        double durabilityFraction = minFraction + random.nextDouble() * (maxFraction - minFraction);
        durabilityFraction = clampDouble(durabilityFraction, 0.0, 1.0);

        double maxDurability = itemStack.getMaxDurability();
        if (maxDurability <= 0) return itemStack;

        return itemStack.withDurability(maxDurability * durabilityFraction);
    }

    private void setInHand(Inventory inventory, ItemStack itemStack) {
        byte activeHotbarSlot = inventory.getActiveHotbarSlot();
        if (activeHotbarSlot == Inventory.INACTIVE_SLOT_INDEX) {
            activeHotbarSlot = 0;
            inventory.setActiveHotbarSlot(activeHotbarSlot);
        }
        inventory.getHotbar().setItemStackForSlot(activeHotbarSlot, itemStack);
    }

    private ItemStack maybeEquipUtilityShield(Inventory inventory, EliteMobsConfig config, int tierIndex) {
        
        if (config.gearConfig.shieldUtilityChancePerTier == null || config.gearConfig.shieldUtilityChancePerTier.length < TIERS_AMOUNT) {
            return null;
        }

        int clampedTierIndex = clampTierIndex(tierIndex);
        double chance = clampDouble(config.gearConfig.shieldUtilityChancePerTier[clampedTierIndex], 0.0, 1.0);
        if (chance <= 0.0) return null;

        ItemContainer utilityContainer = inventory.getUtility();
        if (utilityContainer == null) return null;

        int utilityContainerSize = getContainerSizeSafe(utilityContainer);
        if (utilityContainerSize <= 0) return null;

        ItemStack itemInHand = inventory.getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty()) {
            
            itemInHand = inventory.getHotbar().getItemStack((short) 0);
            if (itemInHand == null || itemInHand.isEmpty()) return null;
            
            
            inventory.setActiveHotbarSlot((byte) 0);
        }

        String weaponItemId = itemInHand.getItemId();
        if (weaponItemId.isBlank()) return null;

        
        if (!isOneHandedWeaponIdInternal(config, weaponItemId)) return null;

        if (random.nextDouble() >= chance) return null;

        int utilitySlotIndex = clampInt(UTILITY_SLOT_INDEX, 0, utilityContainerSize - 1);
        short utilitySlot = (short) utilitySlotIndex;

        ItemStack existingUtilityItem;
        try {
            existingUtilityItem = utilityContainer.getItemStack(utilitySlot);
        } catch (Throwable ignored) {
            return null;
        }
        if (existingUtilityItem != null && !existingUtilityItem.isEmpty()) return null;

        String shieldItemId = pickShieldForTier(config, tierIndex);
        if (shieldItemId == null || shieldItemId.isBlank()) return null;

        ItemStack shield = new ItemStack(shieldItemId, 1);
        shield = withRandomDurabilityFraction(shield, config.gearConfig.spawnGearDurabilityMin, config.gearConfig.spawnGearDurabilityMax);

        try {
            utilityContainer.setItemStackForSlot(utilitySlot, shield);
            
            inventory.setActiveUtilitySlot((byte) utilitySlot);
        } catch (Throwable ignored) {
            return null;
        }

        return shield;
    }

    private boolean isOneHandedWeaponIdInternal(EliteMobsConfig config, String weaponItemId) {
        String lowercaseWeaponId = weaponItemId.toLowerCase(Locale.ROOT);
        if (lowercaseWeaponId.contains(SHIELD_TOKEN)) return false;

        
        if (config.gearConfig.twoHandedWeaponIds == null) return true;

        for (String twoHandedWeaponIdFragment : config.gearConfig.twoHandedWeaponIds) {
            if (twoHandedWeaponIdFragment == null || twoHandedWeaponIdFragment.isBlank()) continue;
            if (lowercaseWeaponId.contains(twoHandedWeaponIdFragment.toLowerCase(Locale.ROOT))) return false;
        }

        return true;
    }

    private boolean isShieldItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        return itemId.toLowerCase(Locale.ROOT).contains(SHIELD_TOKEN);
    }

    private String pickShieldForTier(EliteMobsConfig config, int tierIndex) {
        
        if (config.gearConfig.defaultWeaponCatalog == null || config.gearConfig.defaultWeaponCatalog.isEmpty()) return null;

        String wantedRarity = pickRarityForTier(config, tierIndex);

        ArrayList<String> shieldCandidates = new ArrayList<>();
        for (String itemId : config.gearConfig.defaultWeaponCatalog) {
            if (itemId == null || itemId.isBlank()) continue;
            if (Item.getAssetMap().getAsset(itemId) == null) continue;

            String lowercaseItemId = itemId.toLowerCase(Locale.ROOT);
            if (!lowercaseItemId.contains(SHIELD_TOKEN)) continue;

            if (wantedRarity.equals(classifyWeaponRarity(config, itemId))) {
                shieldCandidates.add(itemId);
            }
        }

        if (shieldCandidates.isEmpty()) {
            for (String itemId : config.gearConfig.defaultWeaponCatalog) {
                if (itemId == null || itemId.isBlank()) continue;
                if (Item.getAssetMap().getAsset(itemId) == null) continue;
                if (itemId.toLowerCase(Locale.ROOT).contains(SHIELD_TOKEN)) shieldCandidates.add(itemId);
            }
        }

        if (shieldCandidates.isEmpty()) return null;
        return shieldCandidates.get(random.nextInt(shieldCandidates.size()));
    }

    private String pickWeaponForRuleAndTier(EliteMobsConfig config, EliteMobsConfig.MobRule mobRule, int tierIndex) {
        if (config.gearConfig.defaultWeaponCatalog == null || config.gearConfig.defaultWeaponCatalog.isEmpty()) return null;

        String wantedRarity = pickRarityForTier(config, tierIndex);

        List<String> requiredFragments = (mobRule.weaponIdMustContain == null) ? List.of() : mobRule.weaponIdMustContain;

        List<String> forbiddenFragments = (mobRule.weaponIdMustNotContain == null) ? List.of() : mobRule.weaponIdMustNotContain;

        boolean hasRequiredFragments = hasAnyNonBlank(requiredFragments);

        ArrayList<String> weaponCandidates = new ArrayList<>();

        
        for (String itemId : config.gearConfig.defaultWeaponCatalog) {
            if (itemId == null || itemId.isBlank()) continue;
            if (Item.getAssetMap().getAsset(itemId) == null) continue;
            if (isShieldItemId(itemId)) continue;
            if (!wantedRarity.equals(classifyWeaponRarity(config, itemId))) continue;
            if (passesWeaponFilters(itemId, requiredFragments, forbiddenFragments)) weaponCandidates.add(itemId);
        }

        
        if (weaponCandidates.isEmpty()) {
            for (String allowedRarity : allowedRaritiesForTier(config, tierIndex)) {
                for (String itemId : config.gearConfig.defaultWeaponCatalog) {
                    if (itemId == null || itemId.isBlank()) continue;
                    if (Item.getAssetMap().getAsset(itemId) == null) continue;
                    if (isShieldItemId(itemId)) continue;
                    if (!allowedRarity.equals(classifyWeaponRarity(config, itemId))) continue;
                    if (passesWeaponFilters(itemId, requiredFragments, forbiddenFragments))
                        weaponCandidates.add(itemId);
                }
                if (!weaponCandidates.isEmpty()) break;
            }
        }

        
        if (weaponCandidates.isEmpty() && hasRequiredFragments) return null;

        
        if (weaponCandidates.isEmpty()) {
            for (String itemId : config.gearConfig.defaultWeaponCatalog) {
                if (itemId == null || itemId.isBlank()) continue;
                if (Item.getAssetMap().getAsset(itemId) == null) continue;
                if (isShieldItemId(itemId)) continue;
                if (passesWeaponFilters(itemId, requiredFragments, forbiddenFragments)) weaponCandidates.add(itemId);
            }
        }

        if (weaponCandidates.isEmpty()) return null;
        return weaponCandidates.get(random.nextInt(weaponCandidates.size()));
    }


    private boolean passesWeaponFilters(String weaponItemId, List<String> requiredFragments,
                                        List<String> forbiddenFragments) {
        String lowercaseWeaponId = weaponItemId.toLowerCase(Locale.ROOT);

        if (forbiddenFragments != null) {
            for (String forbiddenFragment : forbiddenFragments) {
                if (forbiddenFragment == null || forbiddenFragment.isBlank()) continue;
                if (lowercaseWeaponId.contains(forbiddenFragment.toLowerCase(Locale.ROOT))) return false;
            }
        }

        if (requiredFragments != null && !requiredFragments.isEmpty()) {
            boolean matchesAnyRequiredFragment = false;

            for (String requiredFragment : requiredFragments) {
                if (requiredFragment == null || requiredFragment.isBlank()) continue;

                if (lowercaseWeaponId.contains(requiredFragment.toLowerCase(Locale.ROOT))) {
                    matchesAnyRequiredFragment = true;
                    break;
                }
            }

            return matchesAnyRequiredFragment;
        }

        return true;
    }

    private static boolean hasAnyNonBlank(List<String> strings) {
        if (strings == null || strings.isEmpty()) return false;

        for (String value : strings) {
            if (value != null && !value.isBlank()) return true;
        }

        return false;
    }

    private String pickRarityForTier(EliteMobsConfig config, int tierIndex) {
        if (config.gearConfig.defaultTierEquipmentRarityWeights == null || config.gearConfig.defaultTierEquipmentRarityWeights.isEmpty()) {
            return DEFAULT_RARITY;
        }

        if (tierIndex < 0 || tierIndex >= config.gearConfig.defaultTierEquipmentRarityWeights.size()) {
            return DEFAULT_RARITY;
        }

        Map<String, Double> weights = config.gearConfig.defaultTierEquipmentRarityWeights.get(tierIndex);
        if (weights == null || weights.isEmpty()) return DEFAULT_RARITY;

        double totalWeight = 0.0;
        for (double weight : weights.values()) totalWeight += Math.max(0.0, weight);
        if (totalWeight <= 0.0) return DEFAULT_RARITY;

        double selection = random.nextDouble() * totalWeight;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            selection -= Math.max(0.0, entry.getValue());
            if (selection <= 0.0) return entry.getKey();
        }

        return weights.keySet().iterator().next();
    }


    private List<String> allowedRaritiesForTier(EliteMobsConfig config, int tierIndex) {
        if (config.gearConfig.defaultTierAllowedRarities == null) return List.of(DEFAULT_RARITY);
        if (tierIndex < 0 || tierIndex >= config.gearConfig.defaultTierAllowedRarities.size()) {
            return List.of(DEFAULT_RARITY);
        }

        List<String> rarities = config.gearConfig.defaultTierAllowedRarities.get(tierIndex);
        return (rarities == null || rarities.isEmpty()) ? List.of(DEFAULT_RARITY) : rarities;
    }


    private List<String> pickArmorMaterialsOfRarity(EliteMobsConfig config, String wantedRarity) {
        if (config.gearConfig.defaultArmorMaterials == null || config.gearConfig.defaultArmorMaterials.isEmpty()) return List.of();

        ArrayList<String> materials = new ArrayList<>();
        for (String material : config.gearConfig.defaultArmorMaterials) {
            if (material == null || material.isBlank()) continue;
            if (wantedRarity.equals(classifyArmorRarity(config, material))) materials.add(material);
        }

        return materials;
    }

    private String classifyArmorRarity(EliteMobsConfig config, String armorMaterial) {
        if (config.gearConfig.defaultArmorRarityRules == null || config.gearConfig.defaultArmorRarityRules.isEmpty()) return DEFAULT_RARITY;

        String lowercaseMaterial = armorMaterial.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> entry : config.gearConfig.defaultArmorRarityRules.entrySet()) {
            String matchFragment = entry.getKey();
            if (matchFragment == null || matchFragment.isBlank()) continue;

            if (lowercaseMaterial.contains(matchFragment.toLowerCase(Locale.ROOT))) {
                String rarity = entry.getValue();
                return (rarity == null || rarity.isBlank()) ? DEFAULT_RARITY : rarity;
            }
        }

        return DEFAULT_RARITY;
    }

    private String classifyWeaponRarity(EliteMobsConfig config, String itemId) {
        if (config.gearConfig.defaultWeaponRarityRules == null || config.gearConfig.defaultWeaponRarityRules.isEmpty()) return DEFAULT_RARITY;

        String lowercaseItemId = itemId.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> entry : config.gearConfig.defaultWeaponRarityRules.entrySet()) {
            String matchFragment = entry.getKey();
            if (matchFragment == null || matchFragment.isBlank()) continue;

            if (lowercaseItemId.contains(matchFragment.toLowerCase(Locale.ROOT))) {
                String rarity = entry.getValue();
                return (rarity == null || rarity.isBlank()) ? DEFAULT_RARITY : rarity;
            }
        }

        return DEFAULT_RARITY;
    }
}
