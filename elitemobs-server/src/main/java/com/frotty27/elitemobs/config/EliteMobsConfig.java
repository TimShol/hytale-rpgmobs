package com.frotty27.elitemobs.config;

import com.frotty27.elitemobs.assets.AssetConfig;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.assets.TieredAssetConfig;
import com.frotty27.elitemobs.config.schema.*;
import com.frotty27.elitemobs.features.EliteMobsConsumablesFeature;
import com.frotty27.elitemobs.features.EliteMobsProjectileResistanceEffectFeature;
import com.frotty27.elitemobs.features.EliteMobsUndeadSummonAbilityFeature;
import com.frotty27.elitemobs.systems.ability.AbilityIds;
import com.google.gson.Gson;

import java.util.*;

import static com.frotty27.elitemobs.utils.Constants.TIERS_AMOUNT;

public final class EliteMobsConfig {

    private static final List<String> DENY_ABILITY_CHARGE_LEAP_LIST = List.of("Eye_Void",
                                                                              "Crawler_Void",
                                                                              "Skeleton_Burnt_Praetorian",
                                                                              "_Gunner"

    );

    private static final List<String> UNDEAD_ROLE_NAME_CONTAINS = List.of("skeleton", "zombie", "wraith");
    private static final List<String> DAMAGE_MELEE_ONLY_NOT_CONTAINS = List.of("shortbow",
                                                                               "crossbow",
                                                                               "staff",
                                                                               "pickaxe",
                                                                               "bomb",
                                                                               "kunai",
                                                                               "blunderbuss",
                                                                               "spellbook"
    );
    private static final List<String> DAMAGE_MELEE_SWORDS_ONLY = List.of("_sword");
    private static final List<String> DAMAGE_MELEE_AXES_ONLY = List.of("axe");
    private static final List<String> DAMAGE_MELEE_LONGSWORD_ONLY = List.of("longsword");
    private static final List<String> DAMAGE_MELEE_CLUBS_ONLY = List.of("club");
    private static final List<String> DAMAGE_MELEE_SPEARS_ONLY = List.of("spear");
    private static final List<String> DAMAGE_MELEE_DAGGERS_ONLY = List.of("daggers");
    private static final List<String> DAMAGE_MELEE_PICKAXE_ONLY = List.of("pickaxe");
    private static final List<String> DAMAGE_MELEE_SHARP_WEAPONS_ONLY = List.of("sword", "axe");
    private static final List<String> DAMAGE_MELEE_TWO_HANDED_SHARP_WEAPONS_ONLY = List.of("longsword", "battleaxe");
    private static final List<String> DAMAGE_RANGED_BOWS_ONLY = List.of("shortbow", "crossbow");
    private static final List<String> DAMAGE_RANGED_STAFFS_ONLY = List.of("staff");
    private static final List<String> DAMAGE_RANGED_GUN_BLUNDERBUSS_ONLY = List.of("blunderbuss");
    private static final List<String> DAMAGE_RANGED_SPELLBOOK_ONLY = List.of("spellbook");

    public static final String SUMMON_ROLE_PREFIX = "EliteMobs_Summon_";
    public static final int DEFAULT_SUMMON_MAX_ALIVE = 7;
    public static final int SUMMON_MAX_ALIVE_MIN = 0;
    public static final int SUMMON_MAX_ALIVE_MAX = 50;

    @CfgVersion
    @Cfg(group = "System", file = "core.yml", comment = "Configuration version. Automatically updated by the mod. WARNING: If this field is missing or set to 0.0.0, ALL config files will be deleted and regenerated with fresh defaults on next startup. Do not remove this field.")
    public String configVersion = "0.0.0";

    public final SpawningConfig spawning = new SpawningConfig();
    public final MobsConfig mobsConfig = new MobsConfig();
    public final HealthConfig healthConfig = new HealthConfig();
    public final DamageConfig damageConfig = new DamageConfig();
    public final ModelConfig modelConfig = new ModelConfig();
    public final GearConfig gearConfig = new GearConfig();
    public final LootConfig lootConfig = new LootConfig();
    public final NameplatesConfig nameplatesConfig = new NameplatesConfig();
    public final AssetGeneratorConfig assetGenerator = new AssetGeneratorConfig();
    public final AbilitiesConfig abilitiesConfig = new AbilitiesConfig();
    public final EffectsConfig effectsConfig = new EffectsConfig();
    public final ConsumablesConfig consumablesConfig = new ConsumablesConfig();
    public final DebugConfig debugConfig = new DebugConfig();
    public final ReconcileConfig reconcileConfig = new ReconcileConfig();

    public enum ProgressionStyle {
        ENVIRONMENT,         
        DISTANCE_FROM_SPAWN, 
        NONE                 
    }

    private static Map<String, List<String>> defaultTierPrefixesByFamily() {
        Map<String, List<String>> m = new LinkedHashMap<>();

        m.put("zombie", List.of("Rotting", "Ravenous", "Putrid", "Monstrous", "Evolved"));
        m.put("zombie_burnt", List.of("Charred", "Smoldering", "Ashen", "Cinderborn", "Infernal"));
        m.put("zombie_frost", List.of("Chilled", "Rimed", "Frostbitten", "Glacial", "Permafrost"));
        m.put("zombie_sand", List.of("Dustworn", "Scoured", "Dune-Cursed", "Tomb-Woken", "Sunwithered"));
        m.put("zombie_aberrant", List.of("Twisted", "Warped", "Mutated", "Horrid", "Eldritch"));

        m.put("skeleton", List.of("Broken", "Reforged", "Grim", "Deathbound", "Ascendant"));
        m.put("skeleton_burnt", List.of("Charred", "Smoldering", "Ashen", "Cinderforged", "Infernal"));
        m.put("skeleton_frost", List.of("Chilled", "Rimed", "Frostbound", "Glacial", "Permafrost"));
        m.put("skeleton_sand", List.of("Dustworn", "Scoured", "Sun-Cursed", "Tomb-Bound", "Sandscoured"));
        m.put("skeleton_pirate", List.of("Bilge", "Saltstained", "Blackwater", "Drowned", "Forsaken"));
        m.put("skeleton_incandescent", List.of("Husk", "Vanguard", "Sentinel", "Warden", "Paragon"));

        m.put("goblin", List.of("Sneaky", "Cutthroat", "Brutal", "Overseer", "Overlord"));
        m.put("trork", List.of("Rough", "Hardened", "Blooded", "Warbound", "Warlord"));
        m.put("outlander", List.of("Ragged", "Veteran", "Battle-Scarred", "Ruthless", "Legendary"));

        m.put("void", List.of("Faded", "Shaded", "Umbral", "Abyssal", "Voidborn"));

        m.put("default", List.of("Common", "Uncommon", "Rare", "Epic", "Legendary"));
        return m;
    }

    private static Map<String, EnvironmentTierRule> defaultEnvironmentTierSpawns() {
        Map<String, EnvironmentTierRule> map = new LinkedHashMap<>();

        EnvironmentTierRule defaultRule = new EnvironmentTierRule();
        defaultRule.enabled = true;
        defaultRule.spawnChancePerTier = new double[]{0.46, 0.28, 0.16, 0.08, 0.04};
        map.put("default", defaultRule);

        EnvironmentTierRule Env_Zone1 = new EnvironmentTierRule();
        Env_Zone1.enabled = true;
        Env_Zone1.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1", Env_Zone1);

        EnvironmentTierRule Env_Zone1_Autumn = new EnvironmentTierRule();
        Env_Zone1_Autumn.enabled = true;
        Env_Zone1_Autumn.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Autumn", Env_Zone1_Autumn);

        EnvironmentTierRule Env_Zone1_Azure = new EnvironmentTierRule();
        Env_Zone1_Azure.enabled = true;
        Env_Zone1_Azure.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Azure", Env_Zone1_Azure);

        EnvironmentTierRule Env_Zone1_Caves = new EnvironmentTierRule();
        Env_Zone1_Caves.enabled = true;
        Env_Zone1_Caves.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves", Env_Zone1_Caves);

        EnvironmentTierRule Env_Zone1_Caves_Forests = new EnvironmentTierRule();
        Env_Zone1_Caves_Forests.enabled = true;
        Env_Zone1_Caves_Forests.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Forests", Env_Zone1_Caves_Forests);

        EnvironmentTierRule Env_Zone1_Caves_Goblins = new EnvironmentTierRule();
        Env_Zone1_Caves_Goblins.enabled = true;
        Env_Zone1_Caves_Goblins.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Goblins", Env_Zone1_Caves_Goblins);

        EnvironmentTierRule Env_Zone1_Caves_Mountains = new EnvironmentTierRule();
        Env_Zone1_Caves_Mountains.enabled = true;
        Env_Zone1_Caves_Mountains.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Mountains", Env_Zone1_Caves_Mountains);

        EnvironmentTierRule Env_Zone1_Caves_Plains = new EnvironmentTierRule();
        Env_Zone1_Caves_Plains.enabled = true;
        Env_Zone1_Caves_Plains.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Plains", Env_Zone1_Caves_Plains);

        EnvironmentTierRule Env_Zone1_Caves_Rats = new EnvironmentTierRule();
        Env_Zone1_Caves_Rats.enabled = true;
        Env_Zone1_Caves_Rats.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Rats", Env_Zone1_Caves_Rats);

        EnvironmentTierRule Env_Zone1_Caves_Spiders = new EnvironmentTierRule();
        Env_Zone1_Caves_Spiders.enabled = true;
        Env_Zone1_Caves_Spiders.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Spiders", Env_Zone1_Caves_Spiders);

        EnvironmentTierRule Env_Zone1_Caves_Swamps = new EnvironmentTierRule();
        Env_Zone1_Caves_Swamps.enabled = true;
        Env_Zone1_Caves_Swamps.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Swamps", Env_Zone1_Caves_Swamps);

        EnvironmentTierRule Env_Zone1_Caves_Volcanic_T1 = new EnvironmentTierRule();
        Env_Zone1_Caves_Volcanic_T1.enabled = true;
        Env_Zone1_Caves_Volcanic_T1.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Volcanic_T1", Env_Zone1_Caves_Volcanic_T1);

        EnvironmentTierRule Env_Zone1_Caves_Volcanic_T2 = new EnvironmentTierRule();
        Env_Zone1_Caves_Volcanic_T2.enabled = true;
        Env_Zone1_Caves_Volcanic_T2.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Volcanic_T2", Env_Zone1_Caves_Volcanic_T2);

        EnvironmentTierRule Env_Zone1_Caves_Volcanic_T3 = new EnvironmentTierRule();
        Env_Zone1_Caves_Volcanic_T3.enabled = true;
        Env_Zone1_Caves_Volcanic_T3.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Caves_Volcanic_T3", Env_Zone1_Caves_Volcanic_T3);

        EnvironmentTierRule Env_Zone1_Dungeons = new EnvironmentTierRule();
        Env_Zone1_Dungeons.enabled = true;
        Env_Zone1_Dungeons.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Dungeons", Env_Zone1_Dungeons);

        EnvironmentTierRule Env_Zone1_Encounters = new EnvironmentTierRule();
        Env_Zone1_Encounters.enabled = true;
        Env_Zone1_Encounters.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Encounters", Env_Zone1_Encounters);

        EnvironmentTierRule Env_Zone1_Forests = new EnvironmentTierRule();
        Env_Zone1_Forests.enabled = true;
        Env_Zone1_Forests.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Forests", Env_Zone1_Forests);

        EnvironmentTierRule Env_Zone1_Graveyard = new EnvironmentTierRule();
        Env_Zone1_Graveyard.enabled = true;
        Env_Zone1_Graveyard.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Graveyard", Env_Zone1_Graveyard);

        EnvironmentTierRule Env_Zone1_Kweebec = new EnvironmentTierRule();
        Env_Zone1_Kweebec.enabled = true;
        Env_Zone1_Kweebec.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Kweebec", Env_Zone1_Kweebec);

        EnvironmentTierRule Env_Zone1_Mage_Towers = new EnvironmentTierRule();
        Env_Zone1_Mage_Towers.enabled = true;
        Env_Zone1_Mage_Towers.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Mage_Towers", Env_Zone1_Mage_Towers);

        EnvironmentTierRule Env_Zone1_Mineshafts = new EnvironmentTierRule();
        Env_Zone1_Mineshafts.enabled = true;
        Env_Zone1_Mineshafts.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Mineshafts", Env_Zone1_Mineshafts);

        EnvironmentTierRule Env_Zone1_Mountains = new EnvironmentTierRule();
        Env_Zone1_Mountains.enabled = true;
        Env_Zone1_Mountains.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Mountains", Env_Zone1_Mountains);

        EnvironmentTierRule Env_Zone1_Plains = new EnvironmentTierRule();
        Env_Zone1_Plains.enabled = true;
        Env_Zone1_Plains.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Plains", Env_Zone1_Plains);

        EnvironmentTierRule Env_Zone1_Shores = new EnvironmentTierRule();
        Env_Zone1_Shores.enabled = true;
        Env_Zone1_Shores.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Shores", Env_Zone1_Shores);

        EnvironmentTierRule Env_Zone1_Swamps = new EnvironmentTierRule();
        Env_Zone1_Swamps.enabled = true;
        Env_Zone1_Swamps.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Swamps", Env_Zone1_Swamps);

        EnvironmentTierRule Env_Zone1_Trork = new EnvironmentTierRule();
        Env_Zone1_Trork.enabled = true;
        Env_Zone1_Trork.spawnChancePerTier = new double[]{0.60, 0.25, 0.15, 0.00, 0.00};
        map.put("Env_Zone1_Trork", Env_Zone1_Trork);

        EnvironmentTierRule Env_Zone2 = new EnvironmentTierRule();
        Env_Zone2.enabled = true;
        Env_Zone2.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2", Env_Zone2);

        EnvironmentTierRule Env_Zone2_Caves = new EnvironmentTierRule();
        Env_Zone2_Caves.enabled = true;
        Env_Zone2_Caves.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves", Env_Zone2_Caves);

        EnvironmentTierRule Env_Zone2_Caves_Deserts = new EnvironmentTierRule();
        Env_Zone2_Caves_Deserts.enabled = true;
        Env_Zone2_Caves_Deserts.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Deserts", Env_Zone2_Caves_Deserts);

        EnvironmentTierRule Env_Zone2_Caves_Goblins = new EnvironmentTierRule();
        Env_Zone2_Caves_Goblins.enabled = true;
        Env_Zone2_Caves_Goblins.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Goblins", Env_Zone2_Caves_Goblins);

        EnvironmentTierRule Env_Zone2_Caves_Plateaus = new EnvironmentTierRule();
        Env_Zone2_Caves_Plateaus.enabled = true;
        Env_Zone2_Caves_Plateaus.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Plateaus", Env_Zone2_Caves_Plateaus);

        EnvironmentTierRule Env_Zone2_Caves_Rats = new EnvironmentTierRule();
        Env_Zone2_Caves_Rats.enabled = true;
        Env_Zone2_Caves_Rats.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Rats", Env_Zone2_Caves_Rats);

        EnvironmentTierRule Env_Zone2_Caves_Savanna = new EnvironmentTierRule();
        Env_Zone2_Caves_Savanna.enabled = true;
        Env_Zone2_Caves_Savanna.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Savanna", Env_Zone2_Caves_Savanna);

        EnvironmentTierRule Env_Zone2_Caves_Scarak = new EnvironmentTierRule();
        Env_Zone2_Caves_Scarak.enabled = true;
        Env_Zone2_Caves_Scarak.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Scarak", Env_Zone2_Caves_Scarak);

        EnvironmentTierRule Env_Zone2_Caves_Scrub = new EnvironmentTierRule();
        Env_Zone2_Caves_Scrub.enabled = true;
        Env_Zone2_Caves_Scrub.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Scrub", Env_Zone2_Caves_Scrub);

        EnvironmentTierRule Env_Zone2_Caves_Volcanic_T1 = new EnvironmentTierRule();
        Env_Zone2_Caves_Volcanic_T1.enabled = true;
        Env_Zone2_Caves_Volcanic_T1.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Volcanic_T1", Env_Zone2_Caves_Volcanic_T1);

        EnvironmentTierRule Env_Zone2_Caves_Volcanic_T2 = new EnvironmentTierRule();
        Env_Zone2_Caves_Volcanic_T2.enabled = true;
        Env_Zone2_Caves_Volcanic_T2.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Volcanic_T2", Env_Zone2_Caves_Volcanic_T2);

        EnvironmentTierRule Env_Zone2_Caves_Volcanic_T3 = new EnvironmentTierRule();
        Env_Zone2_Caves_Volcanic_T3.enabled = true;
        Env_Zone2_Caves_Volcanic_T3.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Caves_Volcanic_T3", Env_Zone2_Caves_Volcanic_T3);

        EnvironmentTierRule Env_Zone2_Deserts = new EnvironmentTierRule();
        Env_Zone2_Deserts.enabled = true;
        Env_Zone2_Deserts.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Deserts", Env_Zone2_Deserts);

        EnvironmentTierRule Env_Zone2_Dungeons = new EnvironmentTierRule();
        Env_Zone2_Dungeons.enabled = true;
        Env_Zone2_Dungeons.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Dungeons", Env_Zone2_Dungeons);

        EnvironmentTierRule Env_Zone2_Encounters = new EnvironmentTierRule();
        Env_Zone2_Encounters.enabled = true;
        Env_Zone2_Encounters.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Encounters", Env_Zone2_Encounters);

        EnvironmentTierRule Env_Zone2_Feran = new EnvironmentTierRule();
        Env_Zone2_Feran.enabled = true;
        Env_Zone2_Feran.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Feran", Env_Zone2_Feran);

        EnvironmentTierRule Env_Zone2_Mage_Towers = new EnvironmentTierRule();
        Env_Zone2_Mage_Towers.enabled = true;
        Env_Zone2_Mage_Towers.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Mage_Towers", Env_Zone2_Mage_Towers);

        EnvironmentTierRule Env_Zone2_Mineshafts = new EnvironmentTierRule();
        Env_Zone2_Mineshafts.enabled = true;
        Env_Zone2_Mineshafts.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Mineshafts", Env_Zone2_Mineshafts);

        EnvironmentTierRule Env_Zone2_Oasis = new EnvironmentTierRule();
        Env_Zone2_Oasis.enabled = true;
        Env_Zone2_Oasis.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Oasis", Env_Zone2_Oasis);

        EnvironmentTierRule Env_Zone2_Plateaus = new EnvironmentTierRule();
        Env_Zone2_Plateaus.enabled = true;
        Env_Zone2_Plateaus.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Plateaus", Env_Zone2_Plateaus);

        EnvironmentTierRule Env_Zone2_Savanna = new EnvironmentTierRule();
        Env_Zone2_Savanna.enabled = true;
        Env_Zone2_Savanna.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Savanna", Env_Zone2_Savanna);

        EnvironmentTierRule Env_Zone2_Scarak = new EnvironmentTierRule();
        Env_Zone2_Scarak.enabled = true;
        Env_Zone2_Scarak.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Scarak", Env_Zone2_Scarak);

        EnvironmentTierRule Env_Zone2_Scrub = new EnvironmentTierRule();
        Env_Zone2_Scrub.enabled = true;
        Env_Zone2_Scrub.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Scrub", Env_Zone2_Scrub);

        EnvironmentTierRule Env_Zone2_Shores = new EnvironmentTierRule();
        Env_Zone2_Shores.enabled = true;
        Env_Zone2_Shores.spawnChancePerTier = new double[]{0.50, 0.25, 0.18, 0.07, 0.00};
        map.put("Env_Zone2_Shores", Env_Zone2_Shores);

        EnvironmentTierRule Env_Zone3 = new EnvironmentTierRule();
        Env_Zone3.enabled = true;
        Env_Zone3.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3", Env_Zone3);

        EnvironmentTierRule Env_Zone3_Caves = new EnvironmentTierRule();
        Env_Zone3_Caves.enabled = true;
        Env_Zone3_Caves.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves", Env_Zone3_Caves);

        EnvironmentTierRule Env_Zone3_Caves_Forests = new EnvironmentTierRule();
        Env_Zone3_Caves_Forests.enabled = true;
        Env_Zone3_Caves_Forests.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves_Forests", Env_Zone3_Caves_Forests);

        EnvironmentTierRule Env_Zone3_Caves_Glacial = new EnvironmentTierRule();
        Env_Zone3_Caves_Glacial.enabled = true;
        Env_Zone3_Caves_Glacial.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves_Glacial", Env_Zone3_Caves_Glacial);

        EnvironmentTierRule Env_Zone3_Caves_Mountains = new EnvironmentTierRule();
        Env_Zone3_Caves_Mountains.enabled = true;
        Env_Zone3_Caves_Mountains.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves_Mountains", Env_Zone3_Caves_Mountains);

        EnvironmentTierRule Env_Zone3_Caves_Spider = new EnvironmentTierRule();
        Env_Zone3_Caves_Spider.enabled = true;
        Env_Zone3_Caves_Spider.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves_Spider", Env_Zone3_Caves_Spider);

        EnvironmentTierRule Env_Zone3_Caves_Tundra = new EnvironmentTierRule();
        Env_Zone3_Caves_Tundra.enabled = true;
        Env_Zone3_Caves_Tundra.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves_Tundra", Env_Zone3_Caves_Tundra);

        EnvironmentTierRule Env_Zone3_Caves_Volcanic_T1 = new EnvironmentTierRule();
        Env_Zone3_Caves_Volcanic_T1.enabled = true;
        Env_Zone3_Caves_Volcanic_T1.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves_Volcanic_T1", Env_Zone3_Caves_Volcanic_T1);

        EnvironmentTierRule Env_Zone3_Caves_Volcanic_T2 = new EnvironmentTierRule();
        Env_Zone3_Caves_Volcanic_T2.enabled = true;
        Env_Zone3_Caves_Volcanic_T2.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves_Volcanic_T2", Env_Zone3_Caves_Volcanic_T2);

        EnvironmentTierRule Env_Zone3_Caves_Volcanic_T3 = new EnvironmentTierRule();
        Env_Zone3_Caves_Volcanic_T3.enabled = true;
        Env_Zone3_Caves_Volcanic_T3.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Caves_Volcanic_T3", Env_Zone3_Caves_Volcanic_T3);

        EnvironmentTierRule Env_Zone3_Dungeons = new EnvironmentTierRule();
        Env_Zone3_Dungeons.enabled = true;
        Env_Zone3_Dungeons.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Dungeons", Env_Zone3_Dungeons);

        EnvironmentTierRule Env_Zone3_Encounters = new EnvironmentTierRule();
        Env_Zone3_Encounters.enabled = true;
        Env_Zone3_Encounters.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Encounters", Env_Zone3_Encounters);

        EnvironmentTierRule Env_Zone3_Forests = new EnvironmentTierRule();
        Env_Zone3_Forests.enabled = true;
        Env_Zone3_Forests.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Forests", Env_Zone3_Forests);

        EnvironmentTierRule Env_Zone3_Glacial = new EnvironmentTierRule();
        Env_Zone3_Glacial.enabled = true;
        Env_Zone3_Glacial.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Glacial", Env_Zone3_Glacial);

        EnvironmentTierRule Env_Zone3_Glacial_Henges = new EnvironmentTierRule();
        Env_Zone3_Glacial_Henges.enabled = true;
        Env_Zone3_Glacial_Henges.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Glacial_Henges", Env_Zone3_Glacial_Henges);

        EnvironmentTierRule Env_Zone3_Hedera = new EnvironmentTierRule();
        Env_Zone3_Hedera.enabled = true;
        Env_Zone3_Hedera.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Hedera", Env_Zone3_Hedera);

        EnvironmentTierRule Env_Zone3_Mage_Towers = new EnvironmentTierRule();
        Env_Zone3_Mage_Towers.enabled = true;
        Env_Zone3_Mage_Towers.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Mage_Towers", Env_Zone3_Mage_Towers);

        EnvironmentTierRule Env_Zone3_Mineshafts = new EnvironmentTierRule();
        Env_Zone3_Mineshafts.enabled = true;
        Env_Zone3_Mineshafts.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Mineshafts", Env_Zone3_Mineshafts);

        EnvironmentTierRule Env_Zone3_Mountains = new EnvironmentTierRule();
        Env_Zone3_Mountains.enabled = true;
        Env_Zone3_Mountains.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Mountains", Env_Zone3_Mountains);

        EnvironmentTierRule Env_Zone3_Outlander = new EnvironmentTierRule();
        Env_Zone3_Outlander.enabled = true;
        Env_Zone3_Outlander.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Outlander", Env_Zone3_Outlander);

        EnvironmentTierRule Env_Zone3_Shores = new EnvironmentTierRule();
        Env_Zone3_Shores.enabled = true;
        Env_Zone3_Shores.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Shores", Env_Zone3_Shores);

        EnvironmentTierRule Env_Zone3_Tundra = new EnvironmentTierRule();
        Env_Zone3_Tundra.enabled = true;
        Env_Zone3_Tundra.spawnChancePerTier = new double[]{0.00, 0.32, 0.28, 0.22, 0.18};
        map.put("Env_Zone3_Tundra", Env_Zone3_Tundra);

        EnvironmentTierRule Env_Zone4 = new EnvironmentTierRule();
        Env_Zone4.enabled = true;
        Env_Zone4.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4", Env_Zone4);

        EnvironmentTierRule Env_Zone4_Caves = new EnvironmentTierRule();
        Env_Zone4_Caves.enabled = true;
        Env_Zone4_Caves.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Caves", Env_Zone4_Caves);

        EnvironmentTierRule Env_Zone4_Caves_Volcanic = new EnvironmentTierRule();
        Env_Zone4_Caves_Volcanic.enabled = true;
        Env_Zone4_Caves_Volcanic.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Caves_Volcanic", Env_Zone4_Caves_Volcanic);

        EnvironmentTierRule Env_Zone4_Crucible = new EnvironmentTierRule();
        Env_Zone4_Crucible.enabled = true;
        Env_Zone4_Crucible.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Crucible", Env_Zone4_Crucible);

        EnvironmentTierRule Env_Zone4_Dungeons = new EnvironmentTierRule();
        Env_Zone4_Dungeons.enabled = true;
        Env_Zone4_Dungeons.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Dungeons", Env_Zone4_Dungeons);

        EnvironmentTierRule Env_Zone4_Encounters = new EnvironmentTierRule();
        Env_Zone4_Encounters.enabled = true;
        Env_Zone4_Encounters.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Encounters", Env_Zone4_Encounters);

        EnvironmentTierRule Env_Zone4_Forests = new EnvironmentTierRule();
        Env_Zone4_Forests.enabled = true;
        Env_Zone4_Forests.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Forests", Env_Zone4_Forests);

        EnvironmentTierRule Env_Zone4_Jungles = new EnvironmentTierRule();
        Env_Zone4_Jungles.enabled = true;
        Env_Zone4_Jungles.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Jungles", Env_Zone4_Jungles);

        EnvironmentTierRule Env_Zone4_Mage_Towers = new EnvironmentTierRule();
        Env_Zone4_Mage_Towers.enabled = true;
        Env_Zone4_Mage_Towers.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Mage_Towers", Env_Zone4_Mage_Towers);

        EnvironmentTierRule Env_Zone4_Sewers = new EnvironmentTierRule();
        Env_Zone4_Sewers.enabled = true;
        Env_Zone4_Sewers.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Sewers", Env_Zone4_Sewers);

        EnvironmentTierRule Env_Zone4_Shores = new EnvironmentTierRule();
        Env_Zone4_Shores.enabled = true;
        Env_Zone4_Shores.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Shores", Env_Zone4_Shores);

        EnvironmentTierRule Env_Zone4_Volcanoes = new EnvironmentTierRule();
        Env_Zone4_Volcanoes.enabled = true;
        Env_Zone4_Volcanoes.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Volcanoes", Env_Zone4_Volcanoes);

        EnvironmentTierRule Env_Zone4_Wastes = new EnvironmentTierRule();
        Env_Zone4_Wastes.enabled = true;
        Env_Zone4_Wastes.spawnChancePerTier = new double[]{0.00, 0.00, 0.40, 0.33, 0.27};
        map.put("Env_Zone4_Wastes", Env_Zone4_Wastes);

        return map;
    }

    public static final class NameplatesConfig {
        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Enable or disable nameplates globally.")
        public boolean enableMobNameplates = true;

        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Nameplate style: RANKED_ROLE (recommended) or SIMPLE.")
        public NameplateMode nameplateMode = NameplateMode.RANKED_ROLE;

        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Enable nameplates for specific tiers.")
        public boolean[] mobNameplatesEnabledPerTier = {true, true, true, true, true};

        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Include specific role names (case-insensitive). Empty allows all.")
        public List<String> mobNameplateMustContainRoles = List.of();

        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Exclude specific role names (case-insensitive).")
        public List<String> mobNameplateMustNotContainRoles = List.of();

        @FixedArraySize(TIERS_AMOUNT)
        @Default
        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Visual indicators for each tier.")
        public String[] monNameplatePrefixPerTier = {"[•]", "[• •]", "[• • •]", "[• • • •]", "[• • • • •]"};

        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Tier-based name prefixes per family (Zombie, Skeleton, etc.). Each list must have 5 values.")
        public Map<String, List<String>> defaultedTierPrefixesByFamily = defaultTierPrefixesByFamily();
    }

    public static final class ReconcileConfig {
        @Min(0.0)
        @Default
        @Cfg(group = "Reconcile", file = "core.yml", comment = "Ticks to reconcile existing elites after a config reload (0 to disable).")
        public int reconcileWindowTicks = 40;

        @Default
        @Cfg(group = "Reconcile", file = "core.yml", comment = "Announce when reconciliation starts and ends in the logs.")
        public boolean announceReconcile = true;
    }

    public enum NameplateMode {
        SIMPLE, RANKED_ROLE
    }

    public static final class SpawningConfig {
        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Progression system: ENVIRONMENT (Zone-based), DISTANCE_FROM_SPAWN (Linear scaling), or NONE (Random).")
        public ProgressionStyle progressionStyle = ProgressionStyle.ENVIRONMENT;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Used if style is ENVIRONMENT. Enable zone-specific tier probabilities.")
        public boolean enableEnvironmentTierSpawns = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Global tier weights used for NONE style or as fallback. Higher = more common.")
        public double[] spawnChancePerTier = {0.46, 0.28, 0.16, 0.08, 0.04};

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Blocks required per tier transition (e.g. 1000m = Tier 1, 2000m = Tier 2).")
        public double distancePerTier = 1000.0;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Block interval for applying bonus stats (e.g. every 100 blocks).")
        public double distanceBonusInterval = 100.0;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Health multiplier bonus added per interval (0.01 = +1% health every 100m).")
        public float distanceHealthBonusPerInterval = 0.01f;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Damage multiplier bonus added per interval (0.005 = +0.5% damage every 100m).")
        public float distanceDamageBonusPerInterval = 0.005f;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Max bonus health multiplier added by distance progression (0.5 = +50% base health max).")
        public float distanceHealthBonusCap = 0.5f;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Max bonus damage multiplier added by distance progression (0.5 = +50% base damage max).")
        public float distanceDamageBonusCap = 0.5f;

        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Zone-specific rules. Key is environment id (e.g. Env_Zone1_Forests).")
        public Map<String, EnvironmentTierRule> defaultEnvironmentTierSpawns = defaultEnvironmentTierSpawns();
    }

    public static final class EnvironmentTierRule {
        @Default
        public boolean enabled = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        public double[] spawnChancePerTier = {0.46, 0.28, 0.16, 0.08, 0.04};
    }

    public static final class HealthConfig {
        @Default
        @Cfg(group = "Health", file = "stats.yml", comment = "Enable or disable health scaling for EliteMobs.")
        public boolean enableMobHealthScaling = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Health", file = "stats.yml", comment = "Base health multiplier per tier.")
        public float[] mobHealthMultiplierPerTier = {0.3f, 0.6f, 1.2f, 1.8f, 2.6f};

        @Default
        @Min(0.0)
        @Max(1.0)
        @Cfg(group = "Health", file = "stats.yml", comment = "Random health variance multiplier (e.g. 0.05 = +/-5% health).")
        public float mobHealthRandomVariance = 0.05f;
    }

    public static final class AssetGeneratorConfig {
        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @YamlIgnore
        public transient String[] tierSuffixes = {"Tier_1", "Tier_2", "Tier_3", "Tier_4", "Tier_5"};
    }

    public static final class GearConfig {
        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Gear", file = "gear.yml", comment = "Number of armor slots to fill per tier (0-4).")
        public int[] armorPiecesToEquipPerTier = {0, 1, 2, 3, 4};

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Gear", file = "gear.yml", comment = "Probability of equipping a utility item (shield/torch) per tier.")
        public double[] shieldUtilityChancePerTier = {0.0, 0.0, 0.20, 0.40, 0.60};

        @Min(0.001)
        @Max(1.0)
        @Default
        @Cfg(group = "Gear", file = "gear.yml", comment = "Minimum item durability fraction on spawn (0.0 to 1.0).")
        public double spawnGearDurabilityMin = 0.02;

        @Min(0.001)
        @Max(1.0)
        @Default
        @Cfg(group = "Gear", file = "gear.yml", comment = "Maximum item durability fraction on spawn (0.0 to 1.0).")
        public double spawnGearDurabilityMax = 0.30;

        @Default
        @Cfg(group = "Gear", file = "gear.yml", comment = "Weapon ID's that contain these words will be marked as two-handed (no shield).")
        public List<String> twoHandedWeaponIds = new ArrayList<>(List.of("shortbow",
                                                                         "crossbow",
                                                                         "spear",
                                                                         "staff",
                                                                         "battleaxe",
                                                                         "longsword",
                                                                         "bomb"
        ));

        @Cfg(group = "Gear", file = "gear.yml", comment = "Weapon rarity rules: maps ID fragments to rarities. First match wins.")
        public Map<String, String> defaultWeaponRarityRules = defaultWeaponRarityRules();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Armor rarity rules: maps ID fragments to rarities. First match wins.")
        public Map<String, String> defaultArmorRarityRules = defaultArmorRarityRules();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Allowed rarities per tier (Tier 1-5).")
        public List<List<String>> defaultTierAllowedRarities = defaultTierAllowedRarities();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Probability of equipping a rarity per tier.")
        public List<Map<String, Double>> defaultTierEquipmentRarityWeights = defaultTierEquipmentRarityWeights();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Valid weapon IDs for Elite generation.")
        public List<String> defaultWeaponCatalog = defaultWeaponCatalog();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Valid armor materials for Elite generation.")
        public List<String> defaultArmorMaterials = defaultArmorMaterials();
    }

    private static Map<String, String> defaultWeaponRarityRules() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("scarab", "common");
        m.put("silversteel", "common");
        m.put("iron_rusty", "common");
        m.put("steel_rusty", "common");
        m.put("wood", "common");
        m.put("crude", "common");
        m.put("copper", "common");

        m.put("iron", "uncommon");
        m.put("stone", "uncommon");
        m.put("steel", "uncommon");
        m.put("scrap", "uncommon");
        m.put("bronze_ancient", "uncommon");
        m.put("bronze", "uncommon");
        m.put("potion_poison", "uncommon");

        m.put("thorium", "rare");
        m.put("spectral", "rare");
        m.put("bone", "rare");
        m.put("doomed", "rare");
        m.put("cobalt", "rare");
        m.put("ancient_steel", "rare");
        m.put("steel_ancient", "rare");
        m.put("tribal", "rare");
        m.put("bomb_stun", "rare");

        m.put("adamantite", "epic");
        m.put("onyxium", "epic");
        m.put("mithril", "epic");
        m.put("void", "epic");
        m.put("Halloween_Broomstick", "epic");
        m.put("bomb_continuous", "epic");
        m.put("praetorian", "epic");

        m.put("flame", "legendary");

        return m;
    }

    private static Map<String, String> defaultArmorRarityRules() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("cotton", "common");
        m.put("linen", "common");
        m.put("silk", "common");
        m.put("wool", "common");
        m.put("wood", "common");
        m.put("copper", "common");
        m.put("club_zombie", "common");

        m.put("diving", "uncommon");
        m.put("kweebec", "uncommon");
        m.put("leather", "uncommon");
        m.put("trork", "uncommon");
        m.put("iron", "uncommon");
        m.put("steel", "uncommon");
        m.put("bronze", "uncommon");

        m.put("thorium", "rare");
        m.put("cobalt", "rare");
        m.put("steel_ancient", "rare");
        m.put("cindercloth", "rare");
        m.put("bronze_ornate", "rare");

        m.put("prisma", "epic");
        m.put("adamantite", "epic");
        m.put("mithril", "epic");
        m.put("praetorian", "epic");
        m.put("onyxium", "epic");

        return m;
    }

    private static List<List<String>> defaultTierAllowedRarities() {
        return List.of(List.of("common"),                    
                       List.of("uncommon"),                  
                       List.of("rare"),                      
                       List.of("epic"),                      
                       List.of("epic", "legendary")           
        );
    }

    private static List<Map<String, Double>> defaultTierEquipmentRarityWeights() {
        return List.of(mapOf("common", 1.0),                              
                       mapOf("uncommon", 1.0),                            
                       mapOf("rare", 0.70, "uncommon", 0.30),             
                       mapOf("epic", 0.70, "rare", 0.30),                 
                       mapOf("legendary", 0.40, "epic", 0.60)             
        );
    }

    private static List<String> defaultWeaponCatalog() {
        return new ArrayList<>(List.of("Weapon_Axe_Adamantite",
                                       "Weapon_Axe_Bone",
                                       "Weapon_Axe_Cobalt",
                                       "Weapon_Axe_Copper",
                                       "Weapon_Axe_Crude",
                                       "Weapon_Axe_Doomed",
                                       "Weapon_Axe_Iron_Rusty",
                                       "Weapon_Axe_Iron",
                                       "Weapon_Axe_Mithril",
                                       "Weapon_Axe_Onyxium",
                                       "Weapon_Axe_Stone_Trork",
                                       "Weapon_Axe_Thorium",
                                       "Weapon_Axe_Tribal",

                
                                       "Weapon_Battleaxe_Adamantite",
                                       "Weapon_Battleaxe_Cobalt",
                                       "Weapon_Battleaxe_Copper",
                                       "Weapon_Battleaxe_Crude",
                                       "Weapon_Battleaxe_Doomed",
                                       "Weapon_Battleaxe_Iron",
                                       "Weapon_Battleaxe_Mithril",
                                       "Weapon_Battleaxe_Onyxium",
                                       "Weapon_Battleaxe_Scarab",
                                       "Weapon_Battleaxe_Scythe_Void",
                                       "Weapon_Battleaxe_Steel_Rusty",
                                       "Weapon_Battleaxe_Stone_Trork",
                                       "Weapon_Battleaxe_Thorium",
                                       "Weapon_Battleaxe_Tribal",
                                       "Weapon_Battleaxe_Wood_Fence",

                
                                       "Weapon_Club_Adamantite",
                                       "Weapon_Club_Cobalt",
                                       "Weapon_Club_Copper",
                                       "Weapon_Club_Crude",
                                       "Weapon_Club_Doomed",
                                       "Weapon_Club_Iron_Rusty",
                                       "Weapon_Club_Iron",
                                       "Weapon_Club_Mithril",
                                       "Weapon_Club_Onyxium",
                                       "Weapon_Club_Scrap",
                                       "Weapon_Club_Steel_Flail_Rusty",
                                       "Weapon_Club_Stone_Trork",
                                       "Weapon_Club_Thorium",
                                       "Weapon_Club_Tribal",
                                       "Weapon_Club_Zombie_Arm",
                                       "Weapon_Club_Zombie_Burnt_Arm",
                                       "Weapon_Club_Zombie_Burnt_Leg",
                                       "Weapon_Club_Zombie_Frost_Arm",
                                       "Weapon_Club_Zombie_Frost_Leg",
                                       "Weapon_Club_Zombie_Leg",
                                       "Weapon_Club_Zombie_Sand_Arm",
                                       "Weapon_Club_Zombie_Sand_Leg",

                
                                       "Weapon_Crossbow_Ancient_Steel",
                                       "Weapon_Crossbow_Iron",

                
                                       "Weapon_Daggers_Adamantite_Saurian",
                                       "Weapon_Daggers_Adamantite",
                                       "Weapon_Daggers_Bone",
                                       "Weapon_Daggers_Bronze_Ancient",
                                       "Weapon_Daggers_Bronze",
                                       "Weapon_Daggers_Claw_Bone",
                                       "Weapon_Daggers_Cobalt",
                                       "Weapon_Daggers_Copper",
                                       "Weapon_Daggers_Crude",
                                       "Weapon_Daggers_Doomed",
                                       "Weapon_Daggers_Fang_Doomed",
                                       "Weapon_Daggers_Iron",
                                       "Weapon_Daggers_Mithril",
                                       "Weapon_Daggers_Onyxium",
                                       "Weapon_Daggers_Stone_Trork",
                                       "Weapon_Daggers_Thorium",

                
                                       "Weapon_Longsword_Adamantite_Saurian",
                                       "Weapon_Longsword_Adamantite",
                                       "Weapon_Longsword_Cobalt",
                                       "Weapon_Longsword_Copper",
                                       "Weapon_Longsword_Crude",
                                       "Weapon_Longsword_Flame",
                                       "Weapon_Longsword_Iron",
                                       "Weapon_Longsword_Katana",
                                       "Weapon_Longsword_Mithril",
                                       "Weapon_Longsword_Onyxium",
                                       "Weapon_Longsword_Praetorian",
                                       "Weapon_Longsword_Scarab",
                                       "Weapon_Longsword_Spectral",
                                       "Weapon_Longsword_Stone_Trork",
                                       "Weapon_Longsword_Thorium",
                                       "Weapon_Longsword_Tribal",
                                       "Weapon_Longsword_Void",

                
                                       "Weapon_Mace_Adamantite",
                                       "Weapon_Mace_Cobalt",
                                       "Weapon_Mace_Copper",
                                       "Weapon_Mace_Crude",
                                       "Weapon_Mace_Iron",
                                       "Weapon_Mace_Mithril",
                                       "Weapon_Mace_Onyxium",
                                       "Weapon_Mace_Prisma",
                                       "Weapon_Mace_Scrap",
                                       "Weapon_Mace_Stone_Trork",
                                       "Weapon_Mace_Thorium",

                
                                       "Weapon_Shield_Adamantite",
                                       "Weapon_Shield_Cobalt",
                                       "Weapon_Shield_Copper",
                                       "Weapon_Shield_Doomed",
                                       "Weapon_Shield_Iron",
                                       "Weapon_Shield_Mithril",
                                       "Weapon_Shield_Onyxium",
                                       "Weapon_Shield_Orbis_Incandescent",
                                       "Weapon_Shield_Orbis_Knight",
                                       "Weapon_Shield_Praetorian",
                                       "Weapon_Shield_Rusty",
                                       "Weapon_Shield_Scrap_Spiked",
                                       "Weapon_Shield_Scrap",
                                       "Weapon_Shield_Thorium",
                                       "Weapon_Shield_Wood",

                
                                       "Weapon_Shortbow_Adamantite",
                                       "Weapon_Shortbow_Bomb",
                                       "Weapon_Shortbow_Bronze",
                                       "Weapon_Shortbow_Cobalt",
                                       "Weapon_Shortbow_Combat",
                                       "Weapon_Shortbow_Copper",
                                       "Weapon_Shortbow_Crude",
                                       "Weapon_Shortbow_Doomed",
                                       "Weapon_Shortbow_Flame",
                                       "Weapon_Shortbow_Frost",
                                       "Weapon_Shortbow_Iron_Rusty",
                                       "Weapon_Shortbow_Iron",
                                       "Weapon_Shortbow_Mithril",
                                       "Weapon_Shortbow_Onyxium",
                                       "Weapon_Shortbow_Pull",
                                       "Weapon_Shortbow_Ricochet",
                                       "Weapon_Shortbow_Thorium",
                                       "Weapon_Shortbow_Vampire",

                
                                       "Weapon_Spear_Adamantite_Saurian",
                                       "Weapon_Spear_Adamantite",
                                       "Weapon_Spear_Bone",
                                       "Weapon_Spear_Bronze",
                                       "Weapon_Spear_Cobalt",
                                       "Weapon_Spear_Copper",
                                       "Weapon_Spear_Crude",
                                       "Weapon_Spear_Double_Incandescent",
                                       "Weapon_Spear_Fishbone",
                                       "Weapon_Spear_Iron",
                                       "Weapon_Spear_Leaf",
                                       "Weapon_Spear_Mithril",
                                       "Weapon_Spear_Onyxium",
                                       "Weapon_Spear_Scrap",
                                       "Weapon_Spear_Stone_Trork",
                                       "Weapon_Spear_Thorium",
                                       "Weapon_Spear_Tribal",

                
                                       "Halloween_Broomstick",
                                       "Weapon_Staff_Adamantite",
                                       "Weapon_Staff_Bo_Bamboo",
                                       "Weapon_Staff_Bo_Wood",
                                       "Weapon_Staff_Bone",
                                       "Weapon_Staff_Bronze",
                                       "Weapon_Staff_Cane",
                                       "Weapon_Staff_Cobalt",
                                       "Weapon_Staff_Copper",
                                       "Weapon_Staff_Crystal_Fire_Trork",
                                       "Weapon_Staff_Crystal_Flame",
                                       "Weapon_Staff_Crystal_Ice",
                                       "Weapon_Staff_Crystal_Purple",
                                       "Weapon_Staff_Crystal_Red",
                                       "Weapon_Staff_Doomed",
                                       "Weapon_Staff_Frost",
                                       "Weapon_Staff_Iron",
                                       "Weapon_Staff_Mithril",
                                       "Weapon_Staff_Onion",
                                       "Weapon_Staff_Onyxium",
                                       "Weapon_Staff_Thorium",
                                       "Weapon_Staff_Wizard",
                                       "Weapon_Staff_Wood_Kweebec",
                                       "Weapon_Staff_Wood_Rotten",
                                       "Weapon_Staff_Wood",

                
                                       "Weapon_Sword_Adamantite",
                                       "Weapon_Sword_Bone",
                                       "Weapon_Sword_Bronze_Ancient",
                                       "Weapon_Sword_Bronze",
                                       "Weapon_Sword_Cobalt",
                                       "Weapon_Sword_Copper",
                                       "Weapon_Sword_Crude",
                                       "Weapon_Sword_Cutlass",
                                       "Weapon_Sword_Doomed",
                                       "Weapon_Sword_Frost",
                                       "Weapon_Sword_Iron",
                                       "Weapon_Sword_Mithril",
                                       "Weapon_Sword_Nexus",
                                       "Weapon_Sword_Onyxium",
                                       "Weapon_Sword_Runic",
                                       "Weapon_Sword_Scrap",
                                       "Weapon_Sword_Silversteel",
                                       "Weapon_Sword_Steel_Incandescent",
                                       "Weapon_Sword_Steel_Rusty",
                                       "Weapon_Sword_Steel",
                                       "Weapon_Sword_Stone_Trork",
                                       "Weapon_Sword_Thorium",
                                       "Weapon_Sword_Wood",

                
                                       "Tool_Pickaxe_Adamantite",
                                       "Tool_Pickaxe_Cobalt",
                                       "Tool_Pickaxe_Copper",
                                       "Tool_Pickaxe_Crude",
                                       "Tool_Pickaxe_Iron",
                                       "Tool_Pickaxe_Mithril",
                                       "Tool_Pickaxe_Onyxium",
                                       "Tool_Pickaxe_Scrap",
                                       "Tool_Pickaxe_Thorium",
                                       "Tool_Pickaxe_Wood",

                
                                       "Weapon_Bomb",
                                       "Weapon_Bomb_Stun",
                                       "Weapon_Bomb_Potion_Poison",
                                       "Weapon_Bomb_Continuous",

                
                                       "Weapon_Kunai",

                                       "Weapon_Gun_Blunderbuss",
                                       "Weapon_Gun_Blunderbuss_Rusty",

                                       "Weapon_Spellbook_Demon",
                                       "Weapon_Spellbook_Fire",
                                       "Weapon_Spellbook_Grimoire_Brown",
                                       "Weapon_Spellbook_Grimoire_Purple",
                                       "Weapon_Spellbook_Rekindle_Embers"
        ));
    }

    private static List<String> defaultArmorMaterials() {
        return new ArrayList<>(List.of("Adamantite",
                                       "Bronze",
                                       "Bronze_Ornate",
                                       "Cloth_Cindercloth",
                                       "Cloth_Cotton",
                                       "Cloth_Linen",
                                       "Cloth_Silk",
                                       "Cloth_Wool",
                                       "Cobalt",
                                       "Copper",
                                       "Diving_Crude",
                                       "Iron",
                                       "Kweebec",
                                       "Leather_Heavy",
                                       "Leather_Light",
                                       "Leather_Medium",
                                       "Leather_Raven",
                                       "Leather_Soft",
                                       "Mithril",
                                       "Onyxium",
                                       "Prisma",
                                       "Steel",
                                       "Steel_Ancient",
                                       "Thorium",
                                       "Trork",
                                       "Wood"
        ));
    }

    public static final class ModelConfig {
        @Default
        @Cfg(group = "Model", file = "visuals.yml", comment = "Enable or disable physical size scaling per tier.")
        public boolean enableMobModelScaling = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Model", file = "visuals.yml", comment = "Physical scale multiplier per tier.")
        public float[] mobModelScaleMultiplierPerTier = {0.74f, 0.85f, 0.96f, 1.07f, 1.18f};

        @Min(0.0)
        @Max(0.2)
        @Default
        @Cfg(group = "Model", file = "visuals.yml", comment = "Random size variance (e.g., 0.04 = +/-4% size).")
        public float mobModelScaleRandomVariance = 0.04f;
    }

    public static final class LootConfig {
        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Loot", file = "loot.yml", comment = "Multiplies vanilla loot amounts per tier.")
        public int[] vanillaDroplistMultiplierPerTier = {0, 0, 2, 4, 6};

        @Min(0.0)
        @Max(1.0)
        @Default
        @Cfg(group = "Loot", file = "loot.yml", comment = "Chance for an Elite to drop its main-hand weapon.")
        public double dropWeaponChance = 0.05;

        @Min(0.0)
        @Max(1.0)
        @Default
        @Cfg(group = "Loot", file = "loot.yml", comment = "Chance for an Elite to drop an armor piece.")
        public double dropArmorPieceChance = 0.05;

        @Min(0.0)
        @Max(1.0)
        @Default
        @Cfg(group = "Loot", file = "loot.yml", comment = "Chance for an Elite to drop its off-hand item (shields, torches, etc.).")
        public double dropOffhandItemChance = 0.05;

        @Cfg(group = "Loot", file = "loot.yml", comment = "Custom loot tables for specific tiers.")
        public List<ExtraDropRule> defaultExtraDrops = defaultExtraDrops();
    }

    public static final class DamageConfig {
        @Default
        @Cfg(group = "Damage", file = "stats.yml", comment = "Enable or disable damage scaling.")
        public boolean enableMobDamageMultiplier = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Damage", file = "stats.yml", comment = "Base damage multiplier per tier.")
        public float[] mobDamageMultiplierPerTier = {0.6f, 1.1f, 1.6f, 2.1f, 2.6f};

        @Default
        @Min(0.0)
        @Max(1.0)
        @Cfg(group = "Damage", file = "stats.yml", comment = "Random damage variance multiplier (e.g. 0.05 = +/-5% damage).")
        public float mobDamageRandomVariance = 0.05f;
    }

    private static List<ExtraDropRule> defaultExtraDrops() {
        List<ExtraDropRule> list = new ArrayList<>();
        list.add(createExtraDropRule("Ingredient_Life_Essence", 1, 3, 4, 11, 21));
        list.add(createExtraDropRule("Ingredient_Life_Essence", 1, 2, 2, 3, 7));
        list.add(createExtraDropRule("Ingredient_Life_Essence", 1, 0, 1, 1, 2));

        list.add(createExtraDropRule("Ore_Copper", 0.1, 1, 2, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Copper", 0.07, 1, 2, 1, 4));
        list.add(createExtraDropRule("Ore_Iron", 0.1, 1, 3, 1, 4));
        list.add(createExtraDropRule("Ingredient_Bar_Iron", 0.07, 1, 3, 1, 3));

        
        list.add(createExtraDropRule("Ore_Silver", 0.07, 2, 4, 1, 3));
        list.add(createExtraDropRule("Ingredient_Bar_Silver", 0.05, 2, 4, 1, 2));
        list.add(createExtraDropRule("Ore_Gold", 0.07, 2, 4, 1, 3));
        list.add(createExtraDropRule("Ingredient_Bar_Gold", 0.05, 2, 4, 1, 2));
        list.add(createExtraDropRule("Ore_Cobalt", 0.07, 3, 3, 1, 3));
        list.add(createExtraDropRule("Ingredient_Bar_Cobalt", 0.05, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Bronze", 0.05, 2, 3, 1, 2));

        list.add(createExtraDropRule("Ingredient_Leather_Medium", 0.1, 2, 2, 1, 3));
        list.add(createExtraDropRule("Ingredient_Leather_Light", 0.13, 2, 2, 1, 3));

        
        list.add(createExtraDropRule("Ore_Thorium", 0.07, 3, 3, 1, 3));
        list.add(createExtraDropRule("Ingredient_Bar_Thorium", 0.05, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ore_Prisma", 0.07, 3, 4, 1, 3));
        list.add(createExtraDropRule("Ingredient_Bar_Prisma", 0.05, 3, 4, 1, 2));
        list.add(createExtraDropRule("Ingredient_Leather_Heavy", 0.09, 3, 3, 2, 5));
        list.add(createExtraDropRule("Ingredient_Leather_Medium", 0.11, 3, 3, 2, 5));
        list.add(createExtraDropRule("Ingredient_Leather_Light", 0.15, 3, 3, 2, 5));
        list.add(createExtraDropRule("Potion_Mana", 0.07, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Regen_Health", 0.07, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Health_Greater", 0.12, 4, 4, 1, 2));
        list.add(createExtraDropRule("Potion_Stamina_Greater", 0.12, 4, 4, 1, 2));

        
        list.add(createExtraDropRule("Ore_Mithril", 0.15, 4, 4, 1, 5));
        list.add(createExtraDropRule("Ingredient_Bar_Mithril", 0.1, 4, 4, 1, 5));
        list.add(createExtraDropRule("Ore_Onyxium", 0.15, 4, 4, 1, 1));
        list.add(createExtraDropRule("Ingredient_Bar_Onyxium", 0.1, 4, 4, 1, 5));
        list.add(createExtraDropRule("Ore_Adamantite", 0.15, 3, 4, 1, 5));
        list.add(createExtraDropRule("Ingredient_Bar_Adamantite", 0.1, 3, 4, 1, 5));
        list.add(createExtraDropRule("Ingredient_Leather_Heavy", 0.3, 4, 4, 3, 7));
        list.add(createExtraDropRule("Ingredient_Leather_Medium", 0.4, 4, 4, 3, 7));
        list.add(createExtraDropRule("Ingredient_Leather_Light", 0.5, 4, 4, 3, 7));
        list.add(createExtraDropRule("Tool_Repair_Kit_Iron", 0.3, 4, 4, 1, 3));
        list.add(createExtraDropRule("Potion_Mana_Large", 0.1, 4, 4, 1, 2));
        list.add(createExtraDropRule("Potion_Regen_Health_Large", 0.1, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Regen_Stamina_Large", 0.1, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Health_Greater", 0.2, 4, 4, 1, 3));
        list.add(createExtraDropRule("Potion_Stamina_Greater", 0.2, 4, 4, 1, 3));
        list.add(createExtraDropRule("Potion_Health_Large", 0.1, 4, 4, 1, 2));
        list.add(createExtraDropRule("Potion_Stamina_Large", 0.1, 4, 4, 1, 2));

        list.add(createExtraDropRule("Rock_Gem_Diamond", 0.02, 4, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Ruby", 0.03, 3, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Sapphire", 0.03, 3, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Voidstone", 0.02, 4, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Zephyr", 0.02, 4, 4, 1, 1));

        return list;
    }

    public static final class ConsumableConfig extends AssetConfig {
        public float horizontalSpeedMultiplier = 0.6f;
        public float consumeDuration = 1f;

        @Override
        public AssetType namespace() {
            return AssetType.CONSUMABLES;
        }
    }

    public static final class ConsumablesConfig {
        @Default
        @Cfg(group = "Consumables", file = "consumables.yml")
        public Map<String, ConsumableConfig> defaultConsumables = defaultConsumables();
    }

    private static Map<String, ConsumableConfig> defaultConsumables() {
        Map<String, ConsumableConfig> m = new LinkedHashMap<>();
        ConsumableConfig food_tier1 = new ConsumableConfig();
        food_tier1.consumeDuration = 1.0f;
        food_tier1.horizontalSpeedMultiplier = 0.7f;
        m.put(EliteMobsConsumablesFeature.CONSUMABLE_FOOD_TIER1, food_tier1);

        ConsumableConfig food_tier2 = new ConsumableConfig();
        food_tier2.consumeDuration = 1.2f;
        food_tier2.horizontalSpeedMultiplier = 0.7f;
        m.put(EliteMobsConsumablesFeature.CONSUMABLE_FOOD_TIER2, food_tier2);

        ConsumableConfig food_tier3 = new ConsumableConfig();
        food_tier3.consumeDuration = 1.4f;
        food_tier3.horizontalSpeedMultiplier = 0.7f;
        m.put(EliteMobsConsumablesFeature.CONSUMABLE_FOOD_TIER3, food_tier3);

        ConsumableConfig all_small_potions = new ConsumableConfig();
        all_small_potions.consumeDuration = 1.2f;
        all_small_potions.horizontalSpeedMultiplier = 0.7f;
        m.put(EliteMobsConsumablesFeature.CONSUMABLE_SMALL_POTIONS, all_small_potions);

        ConsumableConfig all_big_potions = new ConsumableConfig();
        all_big_potions.consumeDuration = 1.8f;
        all_big_potions.horizontalSpeedMultiplier = 0.7f;
        m.put(EliteMobsConsumablesFeature.CONSUMABLE_BIG_POTIONS, all_big_potions);
        return m;
    }

    public static final class EffectsConfig {
        @Default
        @Cfg(group = "Effects", file = "effects.yml", comment = "Effects configuration lives here. Say hello.")
        public Map<String, EntityEffectConfig> defaultEntityEffects = defaultEntityEffects();
    }

    private static Map<String, EntityEffectConfig> defaultEntityEffects() {
        Map<String, EntityEffectConfig> m = new LinkedHashMap<>();
        EntityEffectConfig projectileResistance = new EntityEffectConfig();
        projectileResistance.isEnabled = true;
        projectileResistance.isEnabledPerTier = new boolean[]{false, false, false, true, true};
        projectileResistance.amountMultiplierPerTier = new float[]{0f, 0f, 0f, 0.7f, 0.85f};
        projectileResistance.infinite = true;
        projectileResistance.templates.add(EliteMobsProjectileResistanceEffectFeature.EFFECT_PROJECTILE_RESISTANCE,
                                           "Entity/Effects/EliteMobs/EliteMobs_Effect_ProjectileResistance.template.json"
        );
        m.put(EliteMobsProjectileResistanceEffectFeature.EFFECT_PROJECTILE_RESISTANCE, projectileResistance);

        return m;
    }

    public static final class EntityEffectConfig extends TieredAssetConfig {
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] amountMultiplierPerTier = new float[]{1f, 1f, 1f, 1f, 1f};
        public boolean infinite = true;

        @Override
        public AssetType namespace() {
            return AssetType.EFFECTS;
        }
    }

    public static final class DebugConfig {
        @Default
        @Cfg(group = "Debug", file = "core.yml", comment = "Enables debug mode.")
        public boolean isDebugModeEnabled = false;

        @Min(1.0)
        @Default
        @Cfg(group = "Debug", file = "core.yml", comment = "Debug interval for scanning NPC's that match the MobRules in seconds.")
        public int debugMobRuleScanIntervalSeconds = 5;
    }

    public static final class AbilitiesConfig {
        @Cfg(file = "abilities.yml", comment = "Ability configuration lives here. Say hello.")
        public Map<String, AbilityConfig> defaultAbilities = defaultAbilities();
    }

    private static Map<String, AbilityConfig> defaultAbilities() {
        Map<String, AbilityConfig> m = new LinkedHashMap<>();

        ChargeLeapAbilityConfig chargeLeap = new ChargeLeapAbilityConfig();
        chargeLeap.gate.weaponIdMustNotContain = DAMAGE_MELEE_ONLY_NOT_CONTAINS;
        chargeLeap.gate.roleMustNotContain = DENY_ABILITY_CHARGE_LEAP_LIST;

        chargeLeap.isEnabled = true;
        chargeLeap.isEnabledPerTier = new boolean[]{false, false, false, true, true};
        chargeLeap.chancePerTier = new float[]{0f, 0f, 0f, 0.50f, 1.00f};
        chargeLeap.cooldownSecondsPerTier = new float[]{0f, 0f, 0f, 16f, 20f};

        chargeLeap.minRange = 9.0f;
        chargeLeap.maxRange = 30.0f;
        chargeLeap.faceTarget = true;

        chargeLeap.templates.add(AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                                 "Item/RootInteractions/NPCs/EliteMobs/EliteMobs_Ability_ChargeLeap_Root.template.json"
        );
        chargeLeap.templates.add(ChargeLeapAbilityConfig.TEMPLATE_ENTRY_INTERACTION,
                                 "Item/Interactions/NPCs/EliteMobs/EliteMobs_Ability_ChargeLeap_Entry.template.json"
        );
        chargeLeap.templates.add(ChargeLeapAbilityConfig.TEMPLATE_DAMAGE_INTERACTION,
                                 "Item/Interactions/NPCs/EliteMobs/EliteMobs_Ability_ChargeLeap_Damage.template.json"
        );

        chargeLeap.slamRangePerTier = new float[]{0f, 0f, 0f, 3f, 4f};
        chargeLeap.slamBaseDamagePerTier = new int[]{0, 0, 0, 20, 30};

        chargeLeap.applyForcePerTier = new float[]{0f, 0f, 0f, 530f, 530f};
        chargeLeap.knockbackLiftPerTier = new float[]{0f, 0f, 0f, 3f, 6f};
        chargeLeap.knockbackPushAwayPerTier = new float[]{0f, 0f, 0f, -3f, -6f};
        chargeLeap.knockbackForcePerTier = new float[]{0f, 0f, 0f, 20f, 26f};

        m.put(AbilityIds.CHARGE_LEAP, chargeLeap);

        HealLeapAbilityConfig healLeap = new HealLeapAbilityConfig();
        healLeap.isEnabled = true;
        healLeap.isEnabledPerTier = new boolean[]{false, false, false, true, true};
        healLeap.chancePerTier = new float[]{0f, 0f, 0f, 1.00f, 1.00f};
        healLeap.cooldownSecondsPerTier = new float[]{0f, 0f, 0f, 15f, 15f};

        healLeap.minHealthTriggerPercent = 0.50f;
        healLeap.maxHealthTriggerPercent = 0.50f;
        healLeap.instantHealChance = 1.00f;
        healLeap.instantHealAmountPerTier = new float[]{0f, 0f, 0f, 25f, 40f};
        healLeap.npcDrinkDurationSeconds = 3.0f;

        healLeap.applyForcePerTier = new float[]{0f, 0f, 0f, 830f, 930f};

        healLeap.templates.add(AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                                 "Item/RootInteractions/NPCs/EliteMobs/EliteMobs_Ability_HealLeap_Root.template.json"
        );
        healLeap.templates.add(AbilityConfig.TEMPLATE_ENTRY_INTERACTION,
                                 "Item/Interactions/NPCs/EliteMobs/EliteMobs_Ability_HealLeap_Entry.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_ROOT_INTERACTION_CANCEL,
                               "Item/RootInteractions/NPCs/EliteMobs/EliteMobs_Ability_HealLeap_Cancel_Root.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_ENTRY_INTERACTION_CANCEL,
                               "Item/Interactions/NPCs/EliteMobs/EliteMobs_Ability_HealLeap_Cancel_Entry.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_EFFECT_INSTANT_HEAL,
                               "Entity/Effects/EliteMobs/EliteMobs_Effect_InstantHeal.template.json"
        );

        m.put(AbilityIds.HEAL_LEAP, healLeap);

        SummonAbilityConfig undeadSummon = new SummonAbilityConfig();
        undeadSummon.isEnabled = true;
        undeadSummon.isEnabledPerTier = new boolean[]{false, false, false, true, true};
        undeadSummon.chancePerTier = new float[]{0f, 0f, 0f, 0.50f, 1.00f};
        undeadSummon.cooldownSecondsPerTier = new float[]{0f, 0f, 0f, 25f, 25f};
        undeadSummon.gate.roleMustContain = UNDEAD_ROLE_NAME_CONTAINS;

        undeadSummon.templates.add(AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                                   "Item/RootInteractions/NPCs/EliteMobs/EliteMobs_Ability_UndeadSummon_Root.template.json"
        );
        undeadSummon.templates.add(AbilityConfig.TEMPLATE_ENTRY_INTERACTION,
                                   "Item/Interactions/NPCs/EliteMobs/EliteMobs_Ability_UndeadSummon_Entry.template.json"
        );
        undeadSummon.templates.add(SummonAbilityConfig.TEMPLATE_SUMMON_MARKER,
                                   "NPC/Spawn/Markers/EliteMobs/EliteMobs_UndeadBow_Summon_Marker.template.json"
        );

        m.put(AbilityIds.SUMMON_UNDEAD, undeadSummon);
        return m;
    }

    public static class AbilityConfig extends TieredAssetConfig {
        public static final String TEMPLATE_ROOT_INTERACTION = "rootInteraction";
        public static final String TEMPLATE_ENTRY_INTERACTION = "entryInteraction";

        public AbilityGate gate = new AbilityGate();

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Chance per tier for this ability to be active on an elite (roll happens once on spawn).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] chancePerTier = {1f, 1f, 1f, 1f, 1f};
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Cooldown per tier (seconds).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] cooldownSecondsPerTier = {10f, 10f, 10f, 10f, 10f};

        @Override
        public AssetType namespace() {
            return AssetType.ABILITIES;
        }
    }

    public static final class ChargeLeapAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_DAMAGE_INTERACTION = "damageInteraction";

        public float minRange = 0f;
        public float maxRange = 0f;
        public boolean faceTarget = false;

        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] slamRangePerTier = {0f, 0f, 0f, 0f, 0f};
        @FixedArraySize(value = TIERS_AMOUNT)
        public int[] slamBaseDamagePerTier = {0, 0, 0, 0, 0};
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] applyForcePerTier = {0f, 0f, 0f, 0f, 0f};
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] knockbackLiftPerTier = {0f, 0f, 0f, 0f, 0f};
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] knockbackPushAwayPerTier = {0f, 0f, 0f, 0f, 0f};
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] knockbackForcePerTier = {0f, 0f, 0f, 0f, 0f};
    }

    public static final class HealLeapAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_ROOT_INTERACTION_CANCEL = "rootInteractionCancel";
        public static final String TEMPLATE_ENTRY_INTERACTION_CANCEL = "entryInteractionCancel";
        public static final String TEMPLATE_EFFECT_INSTANT_HEAL = "effectInstantHeal";

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Minimum health percent at which the heal can trigger (rolled once per elite on spawn).")
        public float minHealthTriggerPercent = 0.1f;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum health percent at which the heal can trigger (rolled once per elite on spawn).")
        public float maxHealthTriggerPercent = 0.4f;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Chance to use instant heal instead of regeneration.")
        public float instantHealChance = 0.5f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Instant heal amount per tier (percent of max health).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] instantHealAmountPerTier = {0f, 0f, 0f, 25f, 25f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "NPC potion drinking duration in seconds.")
        public float npcDrinkDurationSeconds = 3.0f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Item id shown in NPC hand while drinking.")
        public String npcDrinkItemId = "Potion_Health_Greater";

        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] applyForcePerTier = {0f, 0f, 0f, 0f, 0f};
    }

    public static final class SummonAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_SUMMON_MARKER = "summonMarker";

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum number of active summoned minions per summoner (0 disables summoning). Clamped to 0..50.")
        public int maxAlive = DEFAULT_SUMMON_MAX_ALIVE;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Role identifiers used to pick which minions get summoned. First match (role name contains this text) wins. 'default' is used if none match.")
        public List<String> roleIdentifiers = new ArrayList<>(List.of("Skeleton_Frost",
                                                                      "Skeleton_Sand",
                                                                      "Skeleton_Burnt",
                                                                      "Skeleton_Incandescent",
                                                                      "Skeleton_Pirate",
                                                                      "Skeleton",
                                                                      "Zombie_Burnt",
                                                                      "Zombie_Frost",
                                                                      "Zombie_Sand",
                                                                      "Zombie"
        ));
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight for role-matched skeleton archers in the summon pool.")
        public double skeletonArcherWeight = 100;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight for extra zombies added to all pools (0 = disabled, only zombie summoners get zombies from primary entries).")
        public double zombieWeight = 0;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight for wraiths in the skeleton summon pool (~20% chance).")
        public double wraithWeight = 25;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight for aberrant zombies in the zombie summon pool (~20% chance).")
        public double aberrantWeight = 25;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Optional explicit spawn marker entries (advanced). If empty, EliteMobs builds this automatically from mob rules.")
        public List<SummonMarkerEntry> spawnMarkerEntries = new ArrayList<>();
        @YamlIgnore
        public String spawnMarkerEntriesJson = "[]";

        @YamlIgnore
        public Map<String, List<SummonMarkerEntry>> spawnMarkerEntriesByRole = new LinkedHashMap<>();
    }

    public static final class SummonMarkerEntry {
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "NPC id to spawn.")
        public String Name = "";
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight in the spawn marker pool.")
        public double Weight = 100;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Flock asset id used to pick the group size.")
        public String Flock = "EliteMobs_Summon_3_7";
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Spawn timing for this entry (ISO-8601 duration, e.g. PT1S).")
        public String SpawnAfterGameTime = "PT0S";
    }

    public static final class AbilityGate {
        public List<String> roleMustContain = List.of();
        public List<String> roleMustNotContain = List.of();
        public List<String> weaponIdMustContain = List.of();
        public List<String> weaponIdMustNotContain = List.of();
        public List<AbilityRule> rules = List.of();
    }

    public static final class AbilityRule {
        
        public boolean deny = true;

        public boolean enabled = true;
        public boolean[] enabledPerTier = {true, true, true, true, true};

        public List<String> roleMustContain = List.of();
        public List<String> roleMustNotContain = List.of();
        public List<String> weaponIdMustContain = List.of();
        public List<String> weaponIdMustNotContain = List.of();
    }

    public static final class MobsConfig {
        @Cfg(group = "MobRules", file = "mobrules.yml", comment = "Mob rules: decide what to do if our scan found a NPC Entity with this id. If the id of the mob is not on the list, it will get the fist (it won't be transformed into an EliteMob. First mobRule match wins btw)")
        public Map<String, MobRule> defaultMobRules = defaultMobRules();
    }

    private static Map<String, MobRule> defaultMobRules() {
        Map<String, MobRule> m = new LinkedHashMap<>();

        m.put("Goblin_Duke",
              mobRule(true,
                      List.of("Goblin_Duke"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_CLUBS_ONLY,
                      List.of()

              )
        );

        m.put("Goblin_Hermit",
              mobRule(true,
                      List.of("Goblin_Hermit"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Goblin_Lobber_Patrol",
              mobRule(true,
                      List.of("Goblin_Lobber_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.ONLY_IF_EMPTY,
                      List.of(),
                      List.of()
              )
        );

        m.put("Goblin_Lobber",
              mobRule(true,
                      List.of("Goblin_Lobber"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.ONLY_IF_EMPTY,
                      List.of(),
                      List.of()
              )
        );

        m.put("Goblin_Miner_Patrol",
              mobRule(true,
                      List.of("Goblin_Miner_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_PICKAXE_ONLY,
                      List.of()
              )
        );

        m.put("Goblin_Miner",
              mobRule(true,
                      List.of("Goblin_Miner"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_PICKAXE_ONLY,
                      List.of()
              )
        );

        m.put("Goblin_Ogre",
              mobRule(true,
                      List.of("Goblin_Ogre"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_DAGGERS_ONLY,
                      List.of()
              )
        );

        m.put("Goblin_Scavenger_Battleaxe",
              mobRule(true,
                      List.of("Goblin_Scavenger_Battleaxe"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_AXES_ONLY,
                      List.of()
              )
        );

        m.put("Goblin_Scavenger_Sword",
              mobRule(true,
                      List.of("Goblin_Scavenger_Sword"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Goblin_Scavenger",
              mobRule(true,
                      List.of("Goblin_Scavenger"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Goblin_Scrapper_Patrol",
              mobRule(true,
                      List.of("Goblin_Scrapper_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Goblin_Scrapper",
              mobRule(true,
                      List.of("Goblin_Scrapper"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Goblin_Thief_Patrol",
              mobRule(true,
                      List.of("Goblin_Thief_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Goblin_Thief",
              mobRule(true,
                      List.of("Goblin_Thief"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Outlander_Berserker",
              mobRule(true,
                      List.of("Outlander_Berserker"),
                      List.of(),
                      List.of(),
                      List.of("wolf"),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Outlander_Brute",
              mobRule(true,
                      List.of("Outlander_Brute"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_AXES_ONLY,
                      List.of()
              )
        );

        m.put("Outlander_Cultist",
              mobRule(true,
                      List.of("Outlander_Cultist"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of("kunai", "daggers"),
                      List.of()
              )
        );

        m.put("Outlander_Hunter",
              mobRule(true,
                      List.of("Outlander_Hunter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Outlander_Marauder",
              mobRule(true,
                      List.of("Outlander_Marauder"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SHARP_WEAPONS_ONLY,
                      List.of()
              )
        );

        m.put("Outlander_Peon",
              mobRule(true,
                      List.of("Outlander_Peon"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_PICKAXE_ONLY,
                      List.of()
              )
        );

        m.put("Outlander_Priest",
              mobRule(true,
                      List.of("Outlander_Priest"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Outlander_Sorcerer",
              mobRule(true,
                      List.of("Outlander_Sorcerer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Outlander_Stalker",
              mobRule(true,
                      List.of("Outlander_Stalker"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of("shortbow", "crossbow", "dagger"),
                      List.of()
              )
        );

        m.put("Trork_Brawler",
              mobRule(true,
                      List.of("Trork_Brawler"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_CLUBS_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Chieftain",
              mobRule(true,
                      List.of("Trork_Chieftain"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_AXES_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Doctor_Witch",
              mobRule(true,
                      List.of("Trork_Doctor_Witch"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Guard",
              mobRule(true,
                      List.of("Trork_Guard"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(DAMAGE_MELEE_SPEARS_ONLY.getFirst(), DAMAGE_MELEE_DAGGERS_ONLY.getFirst()),
                      List.of()
              )
        );

        m.put("Trork_Hunter",
              mobRule(true,
                      List.of("Trork_Hunter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Mauler",
              mobRule(true,
                      List.of("Trork_Mauler"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_CLUBS_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Sentry_Patrol",
              mobRule(true,
                      List.of("Trork_Sentry_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SPEARS_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Sentry",
              mobRule(true,
                      List.of("Trork_Sentry"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SPEARS_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Shaman",
              mobRule(true,
                      List.of("Trork_Shaman"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Unarmed",
              mobRule(true,
                      List.of("Trork_Unarmed"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Trork_Warrior_Patrol",
              mobRule(true,
                      List.of("Trork_Warrior_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_TWO_HANDED_SHARP_WEAPONS_ONLY,
                      List.of()
              )
        );

        m.put("Trork_Warrior",
              mobRule(true,
                      List.of("Trork_Warrior"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_TWO_HANDED_SHARP_WEAPONS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Archer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Archer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Archer_Wander",
              mobRule(true,
                      List.of("Skeleton_Archer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Archer",
              mobRule(true,
                      List.of("Skeleton_Archer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Archmage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Archmage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Archmage_Wander",
              mobRule(true,
                      List.of("Skeleton_Archmage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Archmage",
              mobRule(true,
                      List.of("Skeleton_Archmage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Fighter_Patrol",
              mobRule(true,
                      List.of("Skeleton_Fighter_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Fighter_Wander",
              mobRule(true,
                      List.of("Skeleton_Fighter_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Fighter",
              mobRule(true,
                      List.of("Skeleton_Fighter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Knight_Patrol",
              mobRule(true,
                      List.of("Skeleton_Knight_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Knight_Wander",
              mobRule(true,
                      List.of("Skeleton_Knight_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Knight",
              mobRule(true,
                      List.of("Skeleton_Knight"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Mage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Mage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Mage_Wander",
              mobRule(true,
                      List.of("Skeleton_Mage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Mage",
              mobRule(true,
                      List.of("Skeleton_Mage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Ranger_Patrol",
              mobRule(true,
                      List.of("Skeleton_Ranger_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Ranger_Wander",
              mobRule(true,
                      List.of("Skeleton_Ranger_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Ranger",
              mobRule(true,
                      List.of("Skeleton_Ranger"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Scout_Patrol",
              mobRule(true,
                      List.of("Skeleton_Scout_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Scout_Wander",
              mobRule(true,
                      List.of("Skeleton_Scout_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Scout",
              mobRule(true,
                      List.of("Skeleton_Scout"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Soldier_Patrol",
              mobRule(true,
                      List.of("Skeleton_Soldier_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Soldier_Wander",
              mobRule(true,
                      List.of("Skeleton_Soldier_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Soldier",
              mobRule(true,
                      List.of("Skeleton_Soldier"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Alchemist_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Alchemist_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Alchemist_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Alchemist_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Alchemist",
              mobRule(true,
                      List.of("Skeleton_Burnt_Alchemist"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Archer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Archer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Archer_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Archer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Archer",
              mobRule(true,
                      List.of("Skeleton_Burnt_Archer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Gunner_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Gunner_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_GUN_BLUNDERBUSS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Gunner_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Gunner_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_GUN_BLUNDERBUSS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Gunner",
              mobRule(true,
                      List.of("Skeleton_Burnt_Gunner"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_GUN_BLUNDERBUSS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Knight_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Knight_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Knight_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Knight_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Knight",
              mobRule(true,
                      List.of("Skeleton_Burnt_Knight"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Lancer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Lancer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_AXES_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Lancer_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Lancer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_AXES_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Lancer",
              mobRule(true,
                      List.of("Skeleton_Burnt_Lancer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_AXES_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Praetorian_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Praetorian_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Praetorian_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Praetorian_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Praetorian",
              mobRule(true,
                      List.of("Skeleton_Burnt_Praetorian"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Soldier_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Soldier_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Soldier_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Soldier_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Soldier",
              mobRule(true,
                      List.of("Skeleton_Burnt_Soldier"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Burnt_Wizard_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Wizard_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Wizard_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Wizard_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Wizard",
              mobRule(true,
                      List.of("Skeleton_Burnt_Wizard"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Archer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Archer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Archer_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Archer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Archer",
              mobRule(true,
                      List.of("Skeleton_Frost_Archer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Archmage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Archmage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Archmage_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Archmage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Archmage",
              mobRule(true,
                      List.of("Skeleton_Frost_Archmage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Fighter_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Fighter_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Frost_Fighter_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Fighter_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Frost_Fighter",
              mobRule(true,
                      List.of("Skeleton_Frost_Fighter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Frost_Knight_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Knight_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Frost_Knight_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Knight_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Frost_Knight",
              mobRule(true,
                      List.of("Skeleton_Frost_Knight"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Frost_Mage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Mage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Mage_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Mage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Mage",
              mobRule(true,
                      List.of("Skeleton_Frost_Mage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Ranger_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Ranger_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Ranger_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Ranger_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Ranger",
              mobRule(true,
                      List.of("Skeleton_Frost_Ranger"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Scout_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Scout_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Scout_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Scout_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Scout",
              mobRule(true,
                      List.of("Skeleton_Frost_Scout"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Frost_Soldier_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Soldier_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Frost_Soldier_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Soldier_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Frost_Soldier",
              mobRule(true,
                      List.of("Skeleton_Frost_Soldier"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Incandescent_Fighter_Patrol",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Fighter_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Incandescent_Fighter_Wander",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Fighter_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Incandescent_Fighter",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Fighter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Incandescent_Footman_Patrol",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Footman_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SPEARS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Incandescent_Footman_Wander",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Footman_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SPEARS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Incandescent_Footman",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Footman"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SPEARS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Incandescent_Head",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Head"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Skeleton_Incandescent_Mage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Mage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Incandescent_Mage_Wander",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Mage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Incandescent_Mage",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Mage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Captain_Patrol",
              mobRule(true,
                      List.of("Skeleton_Pirate_Captain_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Captain_Wander",
              mobRule(true,
                      List.of("Skeleton_Pirate_Captain_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Captain",
              mobRule(true,
                      List.of("Skeleton_Pirate_Captain"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Gunner_Patrol",
              mobRule(true,
                      List.of("Skeleton_Pirate_Gunner_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_GUN_BLUNDERBUSS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Gunner_Wander",
              mobRule(true,
                      List.of("Skeleton_Pirate_Gunner_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_GUN_BLUNDERBUSS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Gunner",
              mobRule(true,
                      List.of("Skeleton_Pirate_Gunner"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_GUN_BLUNDERBUSS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Striker_Patrol",
              mobRule(true,
                      List.of("Skeleton_Pirate_Striker_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Striker_Wander",
              mobRule(true,
                      List.of("Skeleton_Pirate_Striker_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Pirate_Striker",
              mobRule(true,
                      List.of("Skeleton_Pirate_Striker"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Archer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Archer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Archer_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Archer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Archer",
              mobRule(true,
                      List.of("Skeleton_Sand_Archer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Archmage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Archmage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Archmage_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Archmage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Archmage",
              mobRule(true,
                      List.of("Skeleton_Sand_Archmage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_STAFFS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Assassin_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Assassin_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_DAGGERS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Assassin_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Assassin_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_DAGGERS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Assassin",
              mobRule(true,
                      List.of("Skeleton_Sand_Assassin"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_DAGGERS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Guard_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Guard_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Guard_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Guard_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Guard",
              mobRule(true,
                      List.of("Skeleton_Sand_Guard"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_SWORDS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Mage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Mage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Mage_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Mage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Mage",
              mobRule(true,
                      List.of("Skeleton_Sand_Mage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_SPELLBOOK_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Ranger_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Ranger_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Ranger_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Ranger_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Ranger",
              mobRule(true,
                      List.of("Skeleton_Sand_Ranger"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Scout_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Scout_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Scout_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Scout_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Scout",
              mobRule(true,
                      List.of("Skeleton_Sand_Scout"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_RANGED_BOWS_ONLY,
                      List.of()
              )
        );

        m.put("Skeleton_Sand_Soldier_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Soldier_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Sand_Soldier_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Soldier_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton_Sand_Soldier",
              mobRule(true,
                      List.of("Skeleton_Sand_Soldier"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Skeleton",
              mobRule(true,
                      List.of("Skeleton"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        m.put("Zombie_Aberrant_Big",
              mobRule(true,
                      List.of("Zombie_Aberrant_Big"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Zombie_Aberrant_Small",
              mobRule(true,
                      List.of("Zombie_Aberrant_Small"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Zombie_Aberrant",
              mobRule(true,
                      List.of("Zombie_Aberrant"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Zombie_Burnt",
              mobRule(true,
                      List.of("Zombie_Burnt"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Zombie_Frost",
              mobRule(true,
                      List.of("Zombie_Frost"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Zombie_Sand",
              mobRule(true,
                      List.of("Zombie_Sand"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Zombie",
              mobRule(true,
                      List.of("Zombie"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Crawler_Void",
              mobRule(true,
                      List.of("Crawler_Void"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Eye_Void",
              mobRule(true,
                      List.of("Eye_Void"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Spawn_Void",
              mobRule(true,
                      List.of("Spawn_Void"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      DAMAGE_MELEE_LONGSWORD_ONLY,
                      List.of()
              )
        );

        m.put("Spectre_Void",
              mobRule(true,
                      List.of("Spectre_Void"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of()
              )
        );

        m.put("Wraith",
              mobRule(true,
                      List.of("Wraith"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, false, false},
                      WeaponOverrideMode.ALWAYS,
                      List.of(),
                      DAMAGE_MELEE_ONLY_NOT_CONTAINS
              )
        );

        return m;
    }


    public enum WeaponOverrideMode {
        NONE,            
        ONLY_IF_EMPTY,   
        ALWAYS,           
    }

    public static final class ExtraDropRule {
        public String itemId = "";
        public double chance = 0.0;
        public int minTierInclusive = 0;
        public int maxTierInclusive = 0;
        public int minQty = 1;
        public int maxQty = 1;
    }

    public static final class MobRule {
        public boolean enabled = true;

        public List<String> matchExact = List.of();
        public List<String> matchStartsWith = List.of();
        public List<String> matchContains = List.of();
        public List<String> matchExcludes = List.of();

        public boolean[] enableWeaponOverrideForTier = new boolean[]{true, true, true, true, true};
        public WeaponOverrideMode weaponOverrideMode = WeaponOverrideMode.ALWAYS;

        public List<String> weaponIdMustContain = List.of();
        public List<String> weaponIdMustNotContain = List.of();

        
    }

    private static MobRule mobRule(boolean enabled, List<String> matchExact, List<String> matchStartsWith,
                                   List<String> contains, List<String> excludes, boolean[] enableWeaponOverrideForTier,
                                   WeaponOverrideMode overrideMode, List<String> mustContain,
                                   List<String> mustNotContain) {
        MobRule r = new MobRule();
        r.matchExact = matchExact;
        r.matchStartsWith = matchStartsWith;
        r.enabled = enabled;
        r.matchContains = contains;
        r.matchExcludes = excludes;
        r.enableWeaponOverrideForTier = enableWeaponOverrideForTier;
        r.weaponOverrideMode = overrideMode;
        r.weaponIdMustContain = mustContain;
        r.weaponIdMustNotContain = mustNotContain;
        return r;
    }

    private static ExtraDropRule createExtraDropRule(String itemId, double chance, int minTier, int maxTier, int minQty,
                                                     int maxQty) {
        ExtraDropRule r = new ExtraDropRule();
        r.itemId = itemId;
        r.chance = chance;
        r.minTierInclusive = minTier;
        r.maxTierInclusive = maxTier;
        r.minQty = minQty;
        r.maxQty = maxQty;
        return r;
    }

    private static Map<String, Double> mapOf(String k1, double v1) {
        LinkedHashMap<String, Double> m = new LinkedHashMap<>();
        m.put(k1, v1);
        return m;
    }

    private static Map<String, Double> mapOf(String k1, double v1, String k2, double v2) {
        LinkedHashMap<String, Double> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    public void populateSummonMarkerEntriesIfEmpty() {
        SummonAbilityConfig summonConfig = null;
        if (abilitiesConfig != null && abilitiesConfig.defaultAbilities != null) {
            AbilityConfig abilityConfig = abilitiesConfig.defaultAbilities.get(EliteMobsUndeadSummonAbilityFeature.ABILITY_UNDEAD_SUMMON);
            if (abilityConfig instanceof SummonAbilityConfig s) summonConfig = s;
        }
        if (summonConfig == null) return;
        if (summonConfig.spawnMarkerEntries != null && !summonConfig.spawnMarkerEntries.isEmpty()) {
            summonConfig.spawnMarkerEntriesJson = toJson(summonConfig.spawnMarkerEntries);
        }

        populateSummonMarkerEntriesByRoleIfEmpty();
        if (summonConfig.spawnMarkerEntries == null || summonConfig.spawnMarkerEntries.isEmpty()) {
            List<SummonMarkerEntry> defaultEntries = summonConfig.spawnMarkerEntriesByRole.get("default");
            if (defaultEntries != null) {
                summonConfig.spawnMarkerEntries = defaultEntries;
                summonConfig.spawnMarkerEntriesJson = toJson(defaultEntries);
            }
        }
    }

    public void populateSummonMarkerEntriesByRoleIfEmpty() {
        SummonAbilityConfig summonConfig = null;
        if (abilitiesConfig != null && abilitiesConfig.defaultAbilities != null) {
            AbilityConfig abilityConfig = abilitiesConfig.defaultAbilities.get(EliteMobsUndeadSummonAbilityFeature.ABILITY_UNDEAD_SUMMON);
            if (abilityConfig instanceof SummonAbilityConfig s) summonConfig = s;
        }
        if (summonConfig == null) return;
        if (summonConfig.spawnMarkerEntriesByRole != null && !summonConfig.spawnMarkerEntriesByRole.isEmpty()) return;
        if (mobsConfig == null || mobsConfig.defaultMobRules == null || mobsConfig.defaultMobRules.isEmpty()) return;

        LinkedHashSet<String> archerNpcIds = new LinkedHashSet<>();
        LinkedHashSet<String> allNpcIds = new LinkedHashSet<>();
        LinkedHashSet<String> zombieNpcIds = new LinkedHashSet<>();
        LinkedHashSet<String> wraithNpcIds = new LinkedHashSet<>();
        LinkedHashSet<String> aberrantNpcIds = new LinkedHashSet<>();

        for (Map.Entry<String, MobRule> entry : mobsConfig.defaultMobRules.entrySet()) {
            if (entry == null) continue;
            MobRule rule = entry.getValue();
            if (rule == null || !rule.enabled) continue;

            List<String> ids = new ArrayList<>();
            if (rule.matchExact != null && !rule.matchExact.isEmpty()) {
                for (String id : rule.matchExact) {
                    if (id == null || id.isBlank()) continue;
                    String cleaned = stripSummonPrefix(id);
                    if (cleaned.isBlank()) continue;
                    ids.add(cleaned);
                }
            } else {
                String key = entry.getKey();
                if (key != null && !key.isBlank()) {
                    String cleaned = stripSummonPrefix(key);
                    if (!cleaned.isBlank()) ids.add(cleaned);
                }
            }

            for (String id : ids) {
                String lower = id.toLowerCase(Locale.ROOT);
                allNpcIds.add(id);
                if (lower.contains("aberrant")) {
                    aberrantNpcIds.add(id);
                } else if (lower.contains("zombie")) {
                    zombieNpcIds.add(id);
                }
                if (lower.contains("wraith")) wraithNpcIds.add(id);
            }

            if (!isTierEnabled(rule.enableWeaponOverrideForTier, 0)) continue;
            if (!hasBowWeaponConstraint(rule.weaponIdMustContain)) continue;

            archerNpcIds.addAll(ids);
        }

        if (archerNpcIds.isEmpty() && !allNpcIds.isEmpty()) {
            for (String id : allNpcIds) {
                String lower = id.toLowerCase(Locale.ROOT);
                if (lower.contains("archer") || lower.contains("ranger") || lower.contains("scout") || lower.contains(
                        "bow")) {
                    archerNpcIds.add(id);
                }
            }
        }

        if (archerNpcIds.isEmpty()) {
            List<String> fallbackIds = List.of("Skeleton_Archer",
                                               "Skeleton_Archer_Patrol",
                                               "Skeleton_Archer_Wander",
                                               "Skeleton_Ranger",
                                               "Skeleton_Ranger_Patrol",
                                               "Skeleton_Ranger_Wander",
                                               "Skeleton_Scout",
                                               "Skeleton_Scout_Patrol",
                                               "Skeleton_Scout_Wander",
                                               "Skeleton_Frost_Archer",
                                               "Skeleton_Frost_Archer_Patrol",
                                               "Skeleton_Frost_Archer_Wander",
                                               "Skeleton_Frost_Ranger",
                                               "Skeleton_Frost_Ranger_Patrol",
                                               "Skeleton_Frost_Ranger_Wander",
                                               "Skeleton_Frost_Scout",
                                               "Skeleton_Frost_Scout_Patrol",
                                               "Skeleton_Frost_Scout_Wander",
                                               "Skeleton_Burnt_Archer",
                                               "Skeleton_Burnt_Archer_Patrol",
                                               "Skeleton_Burnt_Archer_Wander",
                                               "Skeleton_Sand_Archer",
                                               "Skeleton_Sand_Archer_Patrol",
                                               "Skeleton_Sand_Archer_Wander",
                                               "Skeleton_Sand_Ranger",
                                               "Skeleton_Sand_Ranger_Patrol",
                                               "Skeleton_Sand_Ranger_Wander",
                                               "Skeleton_Sand_Scout",
                                               "Skeleton_Sand_Scout_Patrol",
                                               "Skeleton_Sand_Scout_Wander"
            );
            archerNpcIds.addAll(fallbackIds);
        }

        if (archerNpcIds.isEmpty()) return;

        ArrayList<String> roleIdentifiers = new ArrayList<>();
        if (summonConfig.roleIdentifiers != null) {
            for (String identifier : summonConfig.roleIdentifiers) {
                if (identifier == null || identifier.isBlank()) continue;
                roleIdentifiers.add(identifier.trim());
            }
        }
        roleIdentifiers.add("default");

        summonConfig.spawnMarkerEntriesByRole = new LinkedHashMap<>();

        List<String> moreSpecificIdentifiers = new ArrayList<>();

        for (String identifier : roleIdentifiers) {
            String normalizedIdentifier = normalizeRoleIdentifier(identifier);
            if (normalizedIdentifier.isBlank()) continue;

            ArrayList<String> roleBowIds = new ArrayList<>();
            ArrayList<String> roleNpcIds = new ArrayList<>();
            if ("default".equalsIgnoreCase(identifier)) {
                roleBowIds.addAll(archerNpcIds);
                roleNpcIds.addAll(allNpcIds);
            } else {
                String identifierLower = identifier.toLowerCase(Locale.ROOT);
                for (String id : allNpcIds) {
                    String idLower = id.toLowerCase(Locale.ROOT);
                    if (!idLower.contains(identifierLower)) continue;
                    if (matchesMoreSpecificIdentifier(idLower, identifierLower, moreSpecificIdentifiers)) continue;
                    roleNpcIds.add(id);
                }
                for (String id : archerNpcIds) {
                    String idLower = id.toLowerCase(Locale.ROOT);
                    if (!idLower.contains(identifierLower)) continue;
                    if (matchesMoreSpecificIdentifier(idLower, identifierLower, moreSpecificIdentifiers)) continue;
                    roleBowIds.add(id);
                }
                if (roleNpcIds.isEmpty()) roleNpcIds.addAll(allNpcIds);
                if (roleBowIds.isEmpty()) roleBowIds.addAll(archerNpcIds);
            }

            moreSpecificIdentifiers.add(identifier.toLowerCase(Locale.ROOT));

            ArrayList<SummonMarkerEntry> entries = new ArrayList<>();
            double skeletonWeight = Math.max(0.0, summonConfig.skeletonArcherWeight);
            ArrayList<String> skeletonRoleIds = roleNpcIds.isEmpty() ? roleBowIds : roleNpcIds;
            for (String npcId : skeletonRoleIds) {
                SummonMarkerEntry markerEntry = new SummonMarkerEntry();
                markerEntry.Name = npcId;
                markerEntry.Weight = skeletonWeight;
                markerEntry.Flock = "EliteMobs_Summon_3_7";
                markerEntry.SpawnAfterGameTime = "PT0S";
                entries.add(markerEntry);
            }

            double zombieWeight = Math.max(0.0, summonConfig.zombieWeight);
            if (!zombieNpcIds.isEmpty() && zombieWeight > 0.0) {
                for (String npcId : zombieNpcIds) {
                    if (!roleNpcIds.isEmpty()) {
                        String lower = npcId.toLowerCase(Locale.ROOT);
                        String identifierLower = identifier.toLowerCase(Locale.ROOT);
                        if (!lower.contains(identifierLower)) continue;
                    }
                    SummonMarkerEntry markerEntry = new SummonMarkerEntry();
                    markerEntry.Name = npcId;
                    markerEntry.Weight = zombieWeight;
                    markerEntry.Flock = "EliteMobs_Summon_3_7";
                    markerEntry.SpawnAfterGameTime = "PT0S";
                    entries.add(markerEntry);
                }
            }

            double wraithWeight = Math.max(0.0, summonConfig.wraithWeight);
            if (!wraithNpcIds.isEmpty() && wraithWeight > 0.0) {
                for (String npcId : wraithNpcIds) {
                    if (!roleNpcIds.isEmpty()) {
                        String lower = npcId.toLowerCase(Locale.ROOT);
                        String identifierLower = identifier.toLowerCase(Locale.ROOT);
                        if (!lower.contains(identifierLower)) continue;
                    }
                    SummonMarkerEntry markerEntry = new SummonMarkerEntry();
                    markerEntry.Name = npcId;
                    markerEntry.Weight = wraithWeight;
                    markerEntry.Flock = "EliteMobs_Summon_3_7";
                    markerEntry.SpawnAfterGameTime = "PT0S";
                    entries.add(markerEntry);
                }
            }

            double aberrantWeight = Math.max(0.0, summonConfig.aberrantWeight);
            if (!aberrantNpcIds.isEmpty() && aberrantWeight > 0.0 && identifier.toLowerCase(Locale.ROOT).contains(
                    "zombie")) {
                for (String npcId : aberrantNpcIds) {
                    SummonMarkerEntry markerEntry = new SummonMarkerEntry();
                    markerEntry.Name = npcId;
                    markerEntry.Weight = aberrantWeight;
                    markerEntry.Flock = "EliteMobs_Summon_3_7";
                    markerEntry.SpawnAfterGameTime = "PT0S";
                    entries.add(markerEntry);
                }
            }

            summonConfig.spawnMarkerEntriesByRole.put(normalizedIdentifier, entries);
        }
    }

    private static boolean matchesMoreSpecificIdentifier(String npcIdLower, String currentIdentifierLower,
                                                         List<String> moreSpecificIdentifiers) {
        for (String specific : moreSpecificIdentifiers) {
            if (specific.equals(currentIdentifierLower)) continue;
            if (specific.length() > currentIdentifierLower.length() && specific.contains(currentIdentifierLower) && npcIdLower.contains(
                    specific)) {
                return true;
            }
        }
        return false;
    }

    public void upgradeSummonMarkerEntriesToVariantIds() {
        SummonAbilityConfig summonConfig = null;
        if (abilitiesConfig != null && abilitiesConfig.defaultAbilities != null) {
            AbilityConfig abilityConfig = abilitiesConfig.defaultAbilities.get(EliteMobsUndeadSummonAbilityFeature.ABILITY_UNDEAD_SUMMON);
            if (abilityConfig instanceof SummonAbilityConfig s) summonConfig = s;
        }
        if (summonConfig == null) return;
        if (summonConfig.spawnMarkerEntriesByRole != null) {
            for (List<SummonMarkerEntry> entries : summonConfig.spawnMarkerEntriesByRole.values()) {
                upgradeSummonEntries(entries);
            }
        }
        if (summonConfig.spawnMarkerEntries != null) {
            upgradeSummonEntries(summonConfig.spawnMarkerEntries);
        }
    }

    private static void upgradeSummonEntries(List<SummonMarkerEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        for (SummonMarkerEntry entry : entries) {
            if (entry == null || entry.Name == null) continue;
            String name = stripSummonPrefix(entry.Name);
            if (name.isEmpty()) continue;
            entry.Name = name;
        }
    }

    private static boolean isSummonArcherRoleName(String roleName) {
        if (roleName == null) return false;
        String lower = roleName.toLowerCase(Locale.ROOT);
        if (lower.contains("zombie") || lower.contains("wraith")) return false;
        return lower.contains("archer") || lower.contains("ranger") || lower.contains("scout") || lower.contains("bow");
    }

    public boolean isSummonMarkerEntriesEmpty() {
        SummonAbilityConfig summonConfig = null;
        if (abilitiesConfig != null && abilitiesConfig.defaultAbilities != null) {
            AbilityConfig abilityConfig = abilitiesConfig.defaultAbilities.get(EliteMobsUndeadSummonAbilityFeature.ABILITY_UNDEAD_SUMMON);
            if (abilityConfig instanceof SummonAbilityConfig s) summonConfig = s;
        }
        if (summonConfig == null) return true;
        boolean emptyBase = summonConfig.spawnMarkerEntries == null || summonConfig.spawnMarkerEntries.isEmpty();
        boolean emptyByRole = summonConfig.spawnMarkerEntriesByRole == null || summonConfig.spawnMarkerEntriesByRole.isEmpty();
        return emptyBase && emptyByRole;
    }

    public static String normalizeRoleIdentifier(String identifier) {
        if (identifier == null) return "";
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.equalsIgnoreCase("default")) return "Default";
        String normalized = trimmed.replaceAll("[^A-Za-z0-9_]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "").replaceAll("_+$", "");
        return normalized;
    }

    private static String stripSummonPrefix(String id) {
        if (id == null) return "";
        String trimmed = id.trim();
        if (trimmed.startsWith(SUMMON_ROLE_PREFIX)) {
            return trimmed.substring(SUMMON_ROLE_PREFIX.length()).trim();
        }
        return trimmed;
    }

    public static String buildSummonVariantRoleId(String baseRoleId) {
        if (baseRoleId == null || baseRoleId.isBlank()) return baseRoleId;
        if (baseRoleId.startsWith(SUMMON_ROLE_PREFIX)) return baseRoleId;
        return SUMMON_ROLE_PREFIX + baseRoleId;
    }

    private static String toJson(Object value) {
        try {
            return new Gson().toJson(value);
        } catch (Throwable ignored) {
            return "[]";
        }
    }

    private static boolean isTierEnabled(boolean[] enabledPerTier, int tierIndex) {
        if (enabledPerTier == null || tierIndex < 0 || tierIndex >= enabledPerTier.length) return false;
        return enabledPerTier[tierIndex];
    }

    private static boolean hasBowWeaponConstraint(List<String> mustContain) {
        if (mustContain == null || mustContain.isEmpty()) return false;
        for (String fragment : mustContain) {
            if (fragment == null) continue;
            String lower = fragment.toLowerCase(Locale.ROOT);
            if (lower.contains("shortbow") || lower.contains("crossbow")) return true;
        }
        return false;
    }

    public Map<String, ? extends AssetConfig> getAssetConfigForType(AssetType type) {
        if (type == null) return null;
        return switch (type) {
            case ABILITIES -> abilitiesConfig.defaultAbilities;
            case EFFECTS -> effectsConfig.defaultEntityEffects;
            case CONSUMABLES -> consumablesConfig.defaultConsumables;
        };
    }

    
    public void migrate(String fromVersion) {
        if (fromVersion == null || fromVersion.equals(configVersion)) return;

        if (isOlder(fromVersion, "1.1.0")) {
            AbilityConfig heal = abilitiesConfig.defaultAbilities.get("heal_leap");
            if (heal instanceof HealLeapAbilityConfig h) {
                if (h.minHealthTriggerPercent == 0.1f) h.minHealthTriggerPercent = 0.50f;
                if (h.maxHealthTriggerPercent == 0.4f) h.maxHealthTriggerPercent = 0.50f;
                if (h.instantHealChance == 0.5f) h.instantHealChance = 1.00f;
            }
        }

    }

    private static boolean isOlder(String v1, String v2) {
        try {
            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");
            for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
                int n1 = Integer.parseInt(parts1[i]);
                int n2 = Integer.parseInt(parts2[i]);
                if (n1 < n2) return true;
                if (n1 > n2) return false;
            }
            return parts1.length < parts2.length;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
