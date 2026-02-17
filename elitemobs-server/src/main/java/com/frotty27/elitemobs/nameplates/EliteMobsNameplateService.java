package com.frotty27.elitemobs.nameplates;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.nameplatebuilder.api.NameplateAPI;
import com.frotty27.nameplatebuilder.api.NameplateData;
import com.frotty27.nameplatebuilder.api.SegmentTarget;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class EliteMobsNameplateService {

    private static final String DEFAULT_MPC_NAME = "NPC";
    private static final String DEFAULT_FAMILY_KEY = "default";
    private static final Set<String> NOISE_SEGMENTS = Set.of(
            "patrol",
            "wander",
            "big",
            "small"
    );

    private static final Set<String> VARIANT_SEGMENTS = Set.of(
            "burnt",
            "frost",
            "sand",
            "pirate",
            "incandescent",
            "aberrant",
            "void"
    );

    
    public static final String SEGMENT_PREFIX = "elite-tier-prefix";
    public static final String SEGMENT_TIER = "elite-tier";
    public static final String SEGMENT_NAME = "elite-npc-type";

    public static final String DISPLAY_PREFIX = "Elite Tier Prefix";
    public static final String DISPLAY_TIER = "Elite Tier";
    public static final String DISPLAY_NAME = "Elite Type";

    public static final String EXAMPLE_PREFIX = "\u2022 \u2022 \u2022";
    public static final String EXAMPLE_TIER = "Common, ...";
    public static final String EXAMPLE_NAME = "Zombie, ...";

    
    public void describeSegments(JavaPlugin plugin) {
        NameplateAPI.describe(plugin, SEGMENT_PREFIX, DISPLAY_PREFIX, SegmentTarget.NPCS, EXAMPLE_PREFIX);
        NameplateAPI.describe(plugin, SEGMENT_TIER, DISPLAY_TIER, SegmentTarget.NPCS, EXAMPLE_TIER);
        NameplateAPI.describe(plugin, SEGMENT_NAME, DISPLAY_NAME, SegmentTarget.NPCS, EXAMPLE_NAME);
    }

    
    public void applyOrUpdateNameplate(EliteMobsConfig config, Ref<EntityStore> entityRef, Store<EntityStore> entityStore,
                                       CommandBuffer<EntityStore> commandBuffer, String roleName, int tierIndex) {
        if (config == null) return;
        if (entityRef == null || entityStore == null || commandBuffer == null) return;

        int clampedTierIndex = clampTierIndex(tierIndex);

        boolean enabled = config.nameplatesConfig.enableMobNameplates
                && areNameplatesEnabledForTier(config, clampedTierIndex)
                && passesRoleFilters(config, roleName);

        if (!enabled) {
            removeAllSegments(entityStore, entityRef);
            return;
        }

        
        String prefixText = getNameplatePrefixForTier(config, clampedTierIndex);
        String tierText = resolveTierPrefixForRole(config, roleName, clampedTierIndex);
        String nameText = resolveNameText(config, roleName);

        if (prefixText.isBlank() && tierText.isBlank() && nameText.isBlank()) {
            removeAllSegments(entityStore, entityRef);
            return;
        }

        
        ComponentType<EntityStore, NameplateData> type = NameplateAPI.getComponentType();
        NameplateData data = entityStore.getComponent(entityRef, type);
        boolean isNew = data == null;
        if (isNew) {
            data = new NameplateData();
        }

        
        setOrRemove(data, SEGMENT_PREFIX, prefixText);
        setOrRemove(data, SEGMENT_TIER, tierText);
        setOrRemove(data, SEGMENT_NAME, nameText);

        if (isNew) {
            
            
            commandBuffer.putComponent(entityRef, type, data);
        }
    }

    private static void setOrRemove(NameplateData data, String segmentId, String text) {
        if (text.isBlank()) {
            data.removeText(segmentId);
        } else {
            data.setText(segmentId, text);
        }
    }

    private static void removeAllSegments(Store<EntityStore> entityStore, Ref<EntityStore> entityRef) {
        ComponentType<EntityStore, NameplateData> type = NameplateAPI.getComponentType();
        NameplateData data = entityStore.getComponent(entityRef, type);
        if (data != null) {
            data.removeText(SEGMENT_PREFIX);
            data.removeText(SEGMENT_TIER);
            data.removeText(SEGMENT_NAME);
        }
    }

    
    private static String resolveNameText(EliteMobsConfig config, String roleName) {
        EliteMobsConfig.NameplateMode nameplateMode =
                (config.nameplatesConfig.nameplateMode != null) ? config.nameplatesConfig.nameplateMode : EliteMobsConfig.NameplateMode.RANKED_ROLE;

        return switch (nameplateMode) {
            case SIMPLE -> resolveRoleWithoutFamily(roleName);
            case RANKED_ROLE -> resolveDisplayRoleName(roleName);
        };
    }

    private static boolean areNameplatesEnabledForTier(EliteMobsConfig config, int clampedTierIndex) {
        if (config.nameplatesConfig.mobNameplatesEnabledPerTier == null) return true;
        if (config.nameplatesConfig.mobNameplatesEnabledPerTier.length <= clampedTierIndex) return true;
        return config.nameplatesConfig.mobNameplatesEnabledPerTier[clampedTierIndex];
    }

    private static String getNameplatePrefixForTier(EliteMobsConfig config, int clampedTierIndex) {
        if (config.nameplatesConfig.monNameplatePrefixPerTier == null) return "";
        if (config.nameplatesConfig.monNameplatePrefixPerTier.length <= clampedTierIndex) return "";
        return safe(config.nameplatesConfig.monNameplatePrefixPerTier[clampedTierIndex]);
    }

    
    private static boolean passesRoleFilters(EliteMobsConfig config, String roleName) {
        String roleNameLowercase = (roleName == null) ? "" : roleName.toLowerCase(Locale.ROOT);

        List<String> denyList = config.nameplatesConfig.mobNameplateMustNotContainRoles;
        if (denyList != null) {
            for (String forbiddenFragment : denyList) {
                if (forbiddenFragment == null || forbiddenFragment.isBlank()) continue;
                if (roleNameLowercase.contains(forbiddenFragment.toLowerCase(Locale.ROOT))) return false;
            }
        }

        List<String> allowList = config.nameplatesConfig.mobNameplateMustContainRoles;
        if (allowList == null || allowList.isEmpty()) return true;

        boolean hasAnyAllowRule = false;
        for (String requiredFragment : allowList) {
            if (requiredFragment == null || requiredFragment.isBlank()) continue;
            hasAnyAllowRule = true;
            if (roleNameLowercase.contains(requiredFragment.toLowerCase(Locale.ROOT))) return true;
        }

        return !hasAnyAllowRule;
    }

    
    private static String safe(String text) {
        return (text == null) ? "" : text.trim();
    }

    private static String joinNonBlank(String left, String right) {
        String leftText = safe(left);
        String rightText = safe(right);

        if (leftText.isEmpty()) return rightText;
        if (rightText.isEmpty()) return leftText;

        return leftText + " " + rightText;
    }

    
    private static String resolveRoleWithoutFamily(String roleName) {
        if (roleName == null || roleName.isBlank()) return DEFAULT_MPC_NAME;

        String[] segments = roleName.split("_");
        if (segments.length <= 1) return prettifyString(roleName);

        if (segments.length == 2 && isVariantSegment(segments[1])) {
            return prettifyString(segments[0]);
        }

        return prettifyString(joinSegments(segments, 1, segments.length));
    }

    private static String resolveDisplayRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) return DEFAULT_MPC_NAME;

        String[] segments = roleName.split("_");
        if (segments.length == 0) return DEFAULT_MPC_NAME;

        int endExclusive = segments.length;
        while (endExclusive > 0 && isNoiseSegment(segments[endExclusive - 1])) {
            endExclusive--;
        }
        if (endExclusive <= 0) return DEFAULT_MPC_NAME;

        int startInclusive = 1;
        if (endExclusive > 1 && isVariantSegment(segments[1])) {
            startInclusive = 2;
        }

        if (endExclusive <= startInclusive) {
            if (segments.length >= 2 && startInclusive == 2 && isVariantSegment(segments[1])) {
                return prettifyString(segments[0]);
            }
            return prettifyString(joinSegments(segments, 0, endExclusive));
        }

        return prettifyString(joinSegments(segments, startInclusive, endExclusive));
    }

    private static boolean isNoiseSegment(String segment) {
        if (segment == null) return true;
        return NOISE_SEGMENTS.contains(segment.toLowerCase(Locale.ROOT));
    }

    private static boolean isVariantSegment(String segment) {
        if (segment == null) return false;
        return VARIANT_SEGMENTS.contains(segment.toLowerCase(Locale.ROOT));
    }

    private static String joinSegments(String[] segments, int startInclusive, int endExclusive) {
        StringBuilder joined = new StringBuilder();

        for (int index = startInclusive; index < endExclusive; index++) {
            String segment = segments[index];
            if (segment == null || segment.isBlank()) continue;

            if (!joined.isEmpty()) joined.append('_');
            joined.append(segment);
        }

        return joined.toString();
    }

    private static String prettifyString(String text) {
        if (text == null || text.isBlank()) return DEFAULT_MPC_NAME;

        String[] parts = text.replace('_', ' ').split("\\s+");
        StringBuilder pretty = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) continue;

            if (!pretty.isEmpty()) pretty.append(' ');
            pretty.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) pretty.append(part.substring(1).toLowerCase(Locale.ROOT));
        }

        return pretty.toString();
    }

    private static String resolveTierPrefixForRole(EliteMobsConfig config, String roleName, int tierIndex) {
        Map<String, List<String>> tierPrefixesByFamily = config.nameplatesConfig.defaultedTierPrefixesByFamily;
        if (tierPrefixesByFamily == null || tierPrefixesByFamily.isEmpty()) return "";

        String familyKey = classifyFamily(roleName);

        List<String> tierPrefixes = tierPrefixesByFamily.get(familyKey);
        if (tierPrefixes == null) tierPrefixes = tierPrefixesByFamily.get(DEFAULT_FAMILY_KEY);
        if (tierPrefixes == null || tierPrefixes.isEmpty()) return "";

        int clampedTierIndex = clampTierIndex(tierIndex);
        if (clampedTierIndex < 0 || clampedTierIndex >= tierPrefixes.size()) return "";

        return safe(tierPrefixes.get(clampedTierIndex));
    }

    private static String classifyFamily(String roleName) {
        if (roleName == null) return DEFAULT_FAMILY_KEY;
        String roleNameLowercase = roleName.toLowerCase(Locale.ROOT);

        if (roleNameLowercase.contains("_void") || roleNameLowercase.startsWith("crawler_") || roleNameLowercase.startsWith(
                "eye_") || roleNameLowercase.startsWith("spawn_") || roleNameLowercase.startsWith("spectre_") || roleNameLowercase.startsWith(
                "scythe_")) {
            return "void";
        }

        if (roleNameLowercase.startsWith("zombie")) {
            if (roleNameLowercase.contains("_burnt")) return "zombie_burnt";
            if (roleNameLowercase.contains("_frost")) return "zombie_frost";
            if (roleNameLowercase.contains("_sand")) return "zombie_sand";
            if (roleNameLowercase.contains("_aberrant")) return "zombie_aberrant";
            return "zombie";
        }

        if (roleNameLowercase.startsWith("skeleton")) {
            if (roleNameLowercase.contains("_burnt")) return "skeleton_burnt";
            if (roleNameLowercase.contains("_frost")) return "skeleton_frost";
            if (roleNameLowercase.contains("_sand")) return "skeleton_sand";
            if (roleNameLowercase.contains("_pirate")) return "skeleton_pirate";
            if (roleNameLowercase.contains("_incandescent")) return "skeleton_incandescent";
            return "skeleton";
        }

        if (roleNameLowercase.startsWith("goblin")) return "goblin";
        if (roleNameLowercase.startsWith("trork")) return "trork";
        if (roleNameLowercase.startsWith("outlander")) return "outlander";

        return DEFAULT_FAMILY_KEY;
    }
}
