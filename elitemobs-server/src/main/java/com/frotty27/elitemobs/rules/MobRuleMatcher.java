package com.frotty27.elitemobs.rules;

import com.frotty27.elitemobs.config.EliteMobsConfig;

import java.util.List;
import java.util.Map;

import static com.frotty27.elitemobs.utils.StringHelpers.normalizeLower;

public final class MobRuleMatcher {

    private static final int EXACT_BASE_SCORE = 3000;
    private static final int PREFIX_BASE_SCORE = 2000;
    private static final int CONTAINS_BASE_SCORE = 1000;

    public enum MatchKind {
        EXACT,
        PREFIX,
        CONTAINS
    }

    public record MatchResult(
            String key,
            EliteMobsConfig.MobRule mobRule,
            MatchKind matchKind,
            int score
    ) {}

    public MatchResult findBestMatch(EliteMobsConfig cfg, String roleName) {
        if (roleName == null || roleName.isBlank()) return null;

        if (cfg == null || cfg.mobsConfig.defaultMobRules == null || cfg.mobsConfig.defaultMobRules.isEmpty()) return null;

        final String lowerCaseRoleName = normalizeLower(roleName);

        MatchResult bestMatchResult = null;
        int bestScore = Integer.MIN_VALUE;

        for (Map.Entry<String, EliteMobsConfig.MobRule> mobRuleEntry : cfg.mobsConfig.defaultMobRules.entrySet()) {
            EliteMobsConfig.MobRule rule = mobRuleEntry.getValue();
            if (rule == null || !rule.enabled) continue;

            if (roleNameContainsAnyDeniedId(lowerCaseRoleName, rule.matchExcludes)) continue;

            ScoredMatch scoredMatch = scoreRule(lowerCaseRoleName, rule);
            if (scoredMatch == null) continue;

            if (scoredMatch.score > bestScore) {
                bestScore = scoredMatch.score;
                bestMatchResult = new MatchResult(mobRuleEntry.getKey(), rule, scoredMatch.kind, scoredMatch.score);
            } else if (scoredMatch.score == bestScore && bestMatchResult != null) {
                String mobRuleEntryKey = mobRuleEntry.getKey();
                if (mobRuleEntryKey != null && bestMatchResult.key() != null && mobRuleEntryKey.compareTo(bestMatchResult.key()) < 0) {
                    bestMatchResult = new MatchResult(mobRuleEntry.getKey(), rule, scoredMatch.kind, scoredMatch.score);
                }
            }
        }

        return bestMatchResult;
    }

    private record ScoredMatch(MatchKind kind, int score) {}

    private static ScoredMatch scoreRule(String roleLower, EliteMobsConfig.MobRule rule) {
        
        int exactMatchLength = longestExactMatchLength(roleLower, rule.matchExact);
        if (exactMatchLength > 0) return new ScoredMatch(MatchKind.EXACT, EXACT_BASE_SCORE + exactMatchLength);

        
        int prefixMatchLength = longestPrefixMatchLength(roleLower, rule.matchStartsWith);
        if (prefixMatchLength > 0) return new ScoredMatch(MatchKind.PREFIX, PREFIX_BASE_SCORE + prefixMatchLength);

        
        int containsMatchLength = longestContainsMatchLength(roleLower, rule.matchContains);
        if (containsMatchLength > 0) return new ScoredMatch(MatchKind.CONTAINS, CONTAINS_BASE_SCORE + containsMatchLength);

        return null;
    }

    private static boolean roleNameContainsAnyDeniedId(String id, List<String> matchExcludeList) {
        if (matchExcludeList == null || matchExcludeList.isEmpty()) return false;

        for (String matchExcludeEntry : matchExcludeList) {
            String normalizedExcludeEntry = normalizeEntry(matchExcludeEntry);
            if (normalizedExcludeEntry.isEmpty()) continue;
            if (id.contains(normalizedExcludeEntry)) return true;
        }
        return false;
    }

    private static int longestExactMatchLength(String lowerCaseRoleName, List<String> exactMatchList) {
        if (exactMatchList == null || exactMatchList.isEmpty()) return 0;

        int bestScore = 0;
        for (String exactMatchEntry : exactMatchList) {
            String normalizedExactMatchEntry = normalizeEntry(exactMatchEntry);
            if (normalizedExactMatchEntry.isEmpty()) continue;
            if (lowerCaseRoleName.equals(normalizedExactMatchEntry)) bestScore = Math.max(bestScore, normalizedExactMatchEntry.length());
        }
        return bestScore;
    }

    private static int longestPrefixMatchLength(String lowerCaseRoleName, List<String> prefixMatchList) {
        if (prefixMatchList == null || prefixMatchList.isEmpty()) return 0;

        int bestScore = 0;
        for (String prefixMatchEntry : prefixMatchList) {
            String normalizedPrefixMatchEntry = normalizeEntry(prefixMatchEntry);
            if (normalizedPrefixMatchEntry.isEmpty()) continue;
            if (lowerCaseRoleName.startsWith(normalizedPrefixMatchEntry)) bestScore = Math.max(bestScore, normalizedPrefixMatchEntry.length());
        }
        return bestScore;
    }

    private static int longestContainsMatchLength(String lowerCaseRoleName, List<String> containsMatchList) {
        if (containsMatchList == null || containsMatchList.isEmpty()) return 0;

        int bestScore = 0;
        for (String containsMatchEntry : containsMatchList) {
            String normalizedContainsMatchEntry = normalizeEntry(containsMatchEntry);
            if (normalizedContainsMatchEntry.isEmpty()) continue;
            if (lowerCaseRoleName.contains(normalizedContainsMatchEntry)) bestScore = Math.max(bestScore, normalizedContainsMatchEntry.length());
        }
        return bestScore;
    }

    private static String normalizeEntry(String entry) {
        return normalizeLower(entry);
    }
}
