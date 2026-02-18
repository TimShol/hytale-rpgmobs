package com.frotty27.rpgmobs.assets;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetConfigHelpersTest {

    @Test
    void tieredAssetIdFromTemplateKey() {
        RPGMobsConfig cfg = new RPGMobsConfig();

        RPGMobsConfig.AbilityConfig ability = new RPGMobsConfig.AbilityConfig();
        ability.templates.add(RPGMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                              "Item/RootInteractions/NPCs/RPGMobs/RPGMobs_Ability_ChargeLeap_Root.template.json"
        );

        String id = AssetConfigHelpers.getTieredAssetIdFromTemplateKey(cfg,
                                                                       ability,
                                                                       RPGMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                                                                       0
        );

        assertTrue(id.startsWith("RPGMobs_Ability_ChargeLeap_"));
        assertTrue(id.contains("Root"));
        assertTrue(id.endsWith("Tier_1"));
    }

    @Test
    void onlyTemplatePathReturnsTieredId() {
        RPGMobsConfig cfg = new RPGMobsConfig();

        RPGMobsConfig.EntityEffectConfig effect = new RPGMobsConfig.EntityEffectConfig();
        effect.templates.add("Entity/Effects/RPGMobs/RPGMobs_EntityEffect_ProjectileResistance.template.json");

        String id = AssetConfigHelpers.getTieredAssetIdFromOnlyTemplate(cfg, effect, 4);

        assertTrue(id.startsWith("RPGMobs_EntityEffect_ProjectileResistance_"));
        assertTrue(id.endsWith("Tier_5"));
    }

    @Test
    void enabledPerTierRespectsFlags() {
        TieredAssetConfig cfg = new RPGMobsConfig.AbilityConfig();
        cfg.isEnabled = true;
        cfg.isEnabledPerTier = new boolean[]{true, false, true, true, true};

        assertTrue(AssetConfigHelpers.isTieredAssetConfigEnabledForTier(cfg, 0));
        assertFalse(AssetConfigHelpers.isTieredAssetConfigEnabledForTier(cfg, 1));
    }

    @Test
    void safeCastReturnsNullForNoTemplates() {
        RPGMobsConfig cfg = new RPGMobsConfig();
        RPGMobsConfig.EntityEffectConfig effect = new RPGMobsConfig.EntityEffectConfig();

        String id = AssetConfigHelpers.getTieredAssetIdFromOnlyTemplate(cfg, effect, 0);
        assertTrue(id == null || id.isBlank());
    }
}
