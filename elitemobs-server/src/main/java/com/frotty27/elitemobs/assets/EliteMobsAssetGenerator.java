package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.exceptions.FeatureConfigurationException;
import com.frotty27.elitemobs.exceptions.ReflectiveAccessException;
import com.frotty27.elitemobs.exceptions.TemplatePlaceholderException;
import com.frotty27.elitemobs.exceptions.TemplateSyntaxException;
import com.frotty27.elitemobs.features.EliteMobsFeatureRegistry;
import com.frotty27.elitemobs.features.EliteMobsUndeadSummonAbilityFeature;
import com.frotty27.elitemobs.features.IEliteMobsFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.frotty27.elitemobs.assets.TemplateNameGenerator.TEMPLATE_SUFFIX;

public final class EliteMobsAssetGenerator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String TEMPLATE_ROOT = "ServerTemplates";
    private static final String OUTPUT_ROOT = "Server";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final String RAW_PREFIX = "raw.";

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private EliteMobsAssetGenerator() {}

    public static void generateAll(Path eliteMobsDirectory, EliteMobsConfig config, boolean cleanFirst) {
        Path outputRootDirectory = eliteMobsDirectory.resolve(OUTPUT_ROOT);

        try {
            if (cleanFirst) deleteDirectoryIfExists(outputRootDirectory);
            Files.createDirectories(outputRootDirectory);

            int writtenFileCount = 0;
            int processedJsonFileCount = 0;

            // Populate spawn marker entries before template processing so the
            // marker template can embed valid NPC entries (build-8 rejects empty NPCs).
            config.populateSummonMarkerEntriesIfEmpty();

            DotModelIndex modelIndex = DotModelIndex.build(config);

            try (ResourceTree resourceTree = openResourceTreeFromJar()) {
                for (Path sourcePath : resourceTree.walkResourceRoot()) {
                    if (Files.isDirectory(sourcePath)) continue;

                    Path relativePath = resourceTree.relativizeFromRoot(sourcePath);
                    String fileName = sourcePath.getFileName().toString();
                    String lowercaseFileName = fileName.toLowerCase(Locale.ROOT);

                    Path destinationPath = outputRootDirectory.resolve(relativePath.toString().replace('\\', '/'));
                    Files.createDirectories(destinationPath.getParent());


                    if (lowercaseFileName.endsWith(TEMPLATE_SUFFIX)) {
                        String templateText;
                        try {
                            templateText = Files.readString(sourcePath, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new TemplateSyntaxException("Failed to read template: " + sourcePath);
                        }


                        if (templateText.contains("${") && !templateText.contains("}")) {
                            throw new TemplateSyntaxException("Malformed placeholder in template: " + sourcePath);
                        }

                        // Skip spawn marker templates whose NPCs would be empty (build-8 rejects these)
                        if (lowercaseFileName.contains("summon_marker") && templateText.contains(
                                "spawnMarkerEntriesJson")) {
                            String testRender = applyPlaceholders(templateText, modelIndex, 0);
                            if (testRender.contains("\"NPCs\": []") || testRender.contains("\"NPCs\":[]")) {
                                LOGGER.atWarning().log("[AssetGen] Skipping spawn marker template with empty NPCs: %s",
                                                       fileName
                                );
                                continue;
                            }
                        }

                        String baseTemplateId = TemplateNameGenerator.getBaseTemplateNameFromPath(fileName);
                        boolean isTieredTemplate =
                                (baseTemplateId != null && modelIndex.forceTieredTemplateBaseIds.contains(baseTemplateId))
                                        || isTemplateTieredByResolvedPlaceholderTypes(templateText, modelIndex);

                        if (isTieredTemplate) {
                            int generatedCount = generateTierVariants(relativePath, outputRootDirectory, templateText, modelIndex);
                            writtenFileCount += generatedCount;
                            processedJsonFileCount += generatedCount;
                            continue;
                        }

                        Path outputPath = outputRootDirectory.resolve(stripTemplateSuffix(relativePath));
                        Files.createDirectories(outputPath.getParent());

                        String renderedOutput = applyPlaceholders(templateText, modelIndex, -1);
                        writeUtf8(outputPath, renderedOutput);

                        writtenFileCount++;
                        processedJsonFileCount++;
                        continue;
                    }


                    if (!lowercaseFileName.endsWith(".json")) {
                        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                        writtenFileCount++;
                        continue;
                    }

                    String inputJson = Files.readString(sourcePath, StandardCharsets.UTF_8);
                    String renderedJson = applyPlaceholders(inputJson, modelIndex, -1);
                    writeUtf8(destinationPath, renderedJson);

                    writtenFileCount++;
                    processedJsonFileCount++;
                }
            }

            int extraSummonAssets = generateSummonRoleAssets(outputRootDirectory, config);
            writtenFileCount += extraSummonAssets;
            processedJsonFileCount += extraSummonAssets;

            int extraSummonRoleVariants = generateSummonRoleVariantAssets(outputRootDirectory, config);
            writtenFileCount += extraSummonRoleVariants;
            processedJsonFileCount += extraSummonRoleVariants;

            LOGGER.atInfo().log(String.format(
                    "[EliteMobs] Generated assets to: %s (files=%d, jsonProcessed=%d)",
                    outputRootDirectory.toAbsolutePath(),
                    writtenFileCount,
                    processedJsonFileCount
            ));

        } catch (Throwable throwable) {
            LOGGER.atWarning().log(String.format(
                    "[EliteMobs] Asset generation failed: %s",
                    throwable
            ));
            throwable.printStackTrace();
        }
    }


    private static int generateTierVariants(
            Path relativeTemplatePath,
            Path outputRootDirectory,
            String templateJson,
            DotModelIndex modelIndex
    ) throws IOException {

        Path relativeParentDirectory = relativeTemplatePath.getParent();
        String templateFileName = relativeTemplatePath.getFileName().toString();


        String baseTemplateId = TemplateNameGenerator.getBaseTemplateNameFromPath(templateFileName);
        if (baseTemplateId == null || baseTemplateId.isBlank()) return 0;

        int generatedCount = 0;


        for (int tierIndex = 0; tierIndex < 5; tierIndex++) {
            String outputFileName =
                    TemplateNameGenerator.appendTierSuffix(baseTemplateId, modelIndex.config, tierIndex) + ".json";

            Path destinationPath =
                    resolveOutputPath(outputRootDirectory, relativeParentDirectory, outputFileName);

            Files.createDirectories(destinationPath.getParent());

            String renderedOutput = applyPlaceholders(templateJson, modelIndex, tierIndex);
            writeUtf8(destinationPath, renderedOutput);
            generatedCount++;
        }

        return generatedCount;
    }

    private static Path resolveOutputPath(Path outputRootDirectory, Path relativeParentDirectory, String fileName) {
        if (relativeParentDirectory == null) return outputRootDirectory.resolve(fileName);
        String normalizedParentPath = relativeParentDirectory.toString().replace('\\', '/');
        return outputRootDirectory.resolve(normalizedParentPath).resolve(fileName);
    }


    private static boolean isTemplateTieredByResolvedPlaceholderTypes(String templateText, DotModelIndex modelIndex) {
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(templateText);
        while (placeholderMatcher.find()) {
            String rawExpression = placeholderMatcher.group(1);
            if (rawExpression == null) continue;

            String dotPath = rawExpression.trim();
            if (dotPath.startsWith(RAW_PREFIX)) dotPath = dotPath.substring(RAW_PREFIX.length()).trim();
            if (dotPath.isBlank()) continue;

            Object resolvedValue = resolveDotPathValue(dotPath, modelIndex, -1);
            if (isArrayOrList(resolvedValue)) return true;
        }
        return false;
    }

    private static boolean isArrayOrList(Object value) {
        if (value == null) return false;
        return value.getClass().isArray() || (value instanceof List<?>);
    }


    private static String applyPlaceholders(String inputText, DotModelIndex modelIndex, int tierIndex) {
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(inputText);
        StringBuilder outputBuilder = new StringBuilder(inputText.length());

        while (placeholderMatcher.find()) {
            String expression = placeholderMatcher.group(1).trim();
            String replacementText = resolveDotExpression(expression, modelIndex, tierIndex);
            if (replacementText == null) replacementText = placeholderMatcher.group(0);

            placeholderMatcher.appendReplacement(outputBuilder, Matcher.quoteReplacement(replacementText));
        }

        placeholderMatcher.appendTail(outputBuilder);
        return outputBuilder.toString();
    }

    private static String resolveDotExpression(String expression, DotModelIndex modelIndex, int tierIndex) {
        if (expression == null || expression.isBlank()) return null;

        String dotPath = expression.trim();
        boolean useRawReplacement = dotPath.startsWith(RAW_PREFIX);
        if (useRawReplacement) dotPath = dotPath.substring(RAW_PREFIX.length()).trim();

        Object resolvedValue = resolveDotPathValue(dotPath, modelIndex, tierIndex);
        if (resolvedValue == null) return null;


        resolvedValue = selectTierElementIfNeeded(resolvedValue, tierIndex);
        if (resolvedValue == null) return null;


        resolvedValue = convertTemplatePathToAssetIdIfNeeded(resolvedValue, modelIndex, tierIndex);

        return toJsonReplacement(resolvedValue, useRawReplacement);
    }

    private static Object selectTierElementIfNeeded(Object value, int tierIndex) {
        if (tierIndex < 0) return value;
        if (!isArrayOrList(value)) return value;
        return getIndexedElement(value, tierIndex);
    }

    private static Object convertTemplatePathToAssetIdIfNeeded(Object value, DotModelIndex modelIndex, int tierIndex) {
        if (!(value instanceof String stringValue)) return value;

        String normalizedPath = stringValue.trim().replace('\\', '/');
        if (!normalizedPath.toLowerCase(Locale.ROOT).endsWith(TEMPLATE_SUFFIX)) return value;

        String resolvedAssetId = (tierIndex >= 0)
                ? TemplateNameGenerator.getTemplateNameWithTierFromPath(normalizedPath, modelIndex.config, tierIndex)
                : TemplateNameGenerator.getBaseTemplateNameFromPath(normalizedPath);

        return (resolvedAssetId == null) ? "" : resolvedAssetId;
    }

    private static Object resolveDotPathValue(String dotPath, DotModelIndex modelIndex, int tierIndex) {
        List<PathSegment> pathSegments = PathSegment.parse(dotPath);
        if (pathSegments.isEmpty()) return null;

        Object currentValue = null;
        int startSegmentIndex = 0;


        Object singleSegmentRoot = modelIndex.byNamespaceAndKey.get(pathSegments.get(0).name);
        if (singleSegmentRoot != null) {
            currentValue = singleSegmentRoot;
            startSegmentIndex = 1;
        }

        else if (pathSegments.size() >= 2) {
            String namespaceAndKey = pathSegments.get(0).name + "." + pathSegments.get(1).name;
            Object namespacedRootObject = modelIndex.byNamespaceAndKey.get(namespaceAndKey);

            if (namespacedRootObject != null) {
                currentValue = namespacedRootObject;
                startSegmentIndex = 2;
            }
        }

        if (currentValue == null) {
            currentValue = modelIndex.config;
        }

        for (int segmentIndex = startSegmentIndex; segmentIndex < pathSegments.size(); segmentIndex++) {
            PathSegment pathSegment = pathSegments.get(segmentIndex);

            if (currentValue instanceof IEliteMobsFeature feature && "config".equals(pathSegment.name)) {
                currentValue = feature.getConfig(modelIndex.config);
                if (currentValue == null) {
                    throw new FeatureConfigurationException("Feature '" + feature.getFeatureKey() + "' returned null config.");
                }
            } else {
                currentValue = getPublicFieldOrMapValue(currentValue, pathSegment.name);
            }

            if (currentValue == null) {
                throw new TemplatePlaceholderException(dotPath, "segment '" + pathSegment.name + "' resolved to null");
            }

            if (pathSegment.index == null) continue;

            int resolvedIndex = pathSegment.index;
            if (resolvedIndex == Integer.MIN_VALUE) {
                if (tierIndex < 0) {
                    throw new TemplatePlaceholderException(dotPath, "[tier] requested but no tier context available");
                }
                resolvedIndex = tierIndex;
            }

            currentValue = getIndexedElement(currentValue, resolvedIndex);
            if (currentValue == null) {
                throw new TemplatePlaceholderException(dotPath, "index " + resolvedIndex + " out of bounds or null");
            }
        }

        return currentValue;
    }

    private static Object getPublicFieldOrMapValue(Object rootObject, String fieldOrKeyName) {
        if (rootObject == null || fieldOrKeyName == null || fieldOrKeyName.isBlank()) return null;

        if (rootObject instanceof Map<?, ?> map) {
            Object directValue = map.get(fieldOrKeyName);
            if (directValue != null || map.containsKey(fieldOrKeyName)) return directValue;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object entryKey = entry.getKey();
                if (entryKey != null && fieldOrKeyName.equals(String.valueOf(entryKey))) return entry.getValue();
            }
            return null;
        }

        try {
            Field publicField = rootObject.getClass().getField(fieldOrKeyName);
            return publicField.get(rootObject);
        } catch (NoSuchFieldException ignored) {
            return null;
        } catch (Throwable e) {
            throw new ReflectiveAccessException(fieldOrKeyName, e);
        }
    }

    private static Object getIndexedElement(Object value, int index) {
        if (value == null) return null;

        if (value.getClass().isArray()) {
            int arrayLength = Array.getLength(value);
            if (index < 0 || index >= arrayLength) return null;
            return Array.get(value, index);
        }

        if (value instanceof List<?> list) {
            if (index < 0 || index >= list.size()) return null;
            return list.get(index);
        }

        return null;
    }

    private static String toJsonReplacement(Object value, boolean useRawReplacement) {
        if (value == null) return "null";

        if (value instanceof String stringValue) {
            String trimmed = stringValue.trim();
            if (isJsonLiteral(trimmed)) return trimmed;
        }

        if (value.getClass().isEnum()) value = value.toString();

        String json = GSON.toJson(value);

        if (!useRawReplacement) return json;

        if (value instanceof String) {
            if (json.length() >= 2 && json.startsWith("\"") && json.endsWith("\"")) {
                return json.substring(1, json.length() - 1);
            }
        }

        return json;
    }

    private static boolean isJsonLiteral(String text) {
        if (text == null || text.isEmpty()) return false;
        char firstCharacter = text.charAt(0);
        return firstCharacter == '{'
                || firstCharacter == '['
                || "true".equals(text)
                || "false".equals(text)
                || "null".equals(text);
    }


    private static final class DotModelIndex {
        final EliteMobsConfig config;
        final Map<String, Object> byNamespaceAndKey = new LinkedHashMap<>();
        final Set<String> forceTieredTemplateBaseIds = new HashSet<>();

        private DotModelIndex(EliteMobsConfig config) {
            this.config = config;
        }

        static DotModelIndex build(EliteMobsConfig config) {
            DotModelIndex modelIndex = new DotModelIndex(config);
            modelIndex.byNamespaceAndKey.put("features", EliteMobsFeatureRegistry.getInstance().getFeaturesByKey());
            IdentityHashMap<Object, Boolean> visitedObjectIdentities = new IdentityHashMap<>();
            scanModelGraph(config, visitedObjectIdentities, modelIndex);
            return modelIndex;
        }

        private static void scanModelGraph(
                Object object,
                IdentityHashMap<Object, Boolean> visitedObjectIdentities,
                DotModelIndex modelIndex
        ) {
            if (object == null) return;
            if (visitedObjectIdentities.put(object, Boolean.TRUE) != null) return;

            if (object instanceof AssetConfig assetConfig) {
                indexAssetConfig(modelIndex, assetConfig);
            }

            Class<?> objectClass = object.getClass();

            if (objectClass.isArray()) {
                int arrayLength = Array.getLength(object);
                for (int elementIndex = 0; elementIndex < arrayLength; elementIndex++) {
                    scanModelGraph(Array.get(object, elementIndex), visitedObjectIdentities, modelIndex);
                }
                return;
            }

            if (object instanceof List<?> list) {
                for (Object element : list) scanModelGraph(element, visitedObjectIdentities, modelIndex);
                return;
            }

            if (object instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object mapKey = entry.getKey();
                    Object mapValue = entry.getValue();

                    if (mapValue instanceof AssetConfig assetConfig) {
                        assetConfig.setKeyIfBlank(String.valueOf(mapKey));
                        indexAssetConfig(modelIndex, assetConfig);
                    }

                    scanModelGraph(mapValue, visitedObjectIdentities, modelIndex);
                }
                return;
            }

            if (isScalarOrJdkLeaf(objectClass)) return;

            for (Field field : getAllFields(objectClass)) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);

                Object fieldValue;
                try {
                    fieldValue = field.get(object);
                } catch (Throwable ignored) {
                    continue;
                }

                scanModelGraph(fieldValue, visitedObjectIdentities, modelIndex);
            }
        }

        private static void indexAssetConfig(DotModelIndex modelIndex, AssetConfig assetConfig) {
            String namespaceId = (assetConfig.namespace() == null) ? "" : assetConfig.namespace().id();
            String configKey = safe(assetConfig.key());

            if (!namespaceId.isBlank() && !configKey.isBlank()) {
                modelIndex.byNamespaceAndKey.put(namespaceId + "." + configKey, assetConfig);
            }


            if (!(assetConfig instanceof TieredAssetConfig)) return;

            for (String templatePath : ((TieredAssetConfig) assetConfig).templates.values()) {
                if (templatePath == null || templatePath.isBlank()) continue;

                String baseTemplateId = TemplateNameGenerator.getBaseTemplateNameFromPath(templatePath);
                if (baseTemplateId != null && !baseTemplateId.isBlank()) {
                    modelIndex.forceTieredTemplateBaseIds.add(baseTemplateId);
                }
            }
        }

        private static String safe(String text) {
            return (text == null) ? "" : text.trim();
        }

        private static boolean isScalarOrJdkLeaf(Class<?> type) {
            if (type.isPrimitive()) return true;
            if (type.isEnum()) return true;
            if (type == String.class) return true;
            if (Number.class.isAssignableFrom(type)) return true;
            if (type == Boolean.class) return true;
            return type.getName().startsWith("java.time.");
        }

        private static List<Field> getAllFields(Class<?> type) {
            ArrayList<Field> fields = new ArrayList<>();
            Class<?> current = type;
            while (current != null && current != Object.class) {
                fields.addAll(Arrays.asList(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
            return fields;
        }
    }


    private record PathSegment(String name, Integer index) {
        static List<PathSegment> parse(String dotPath) {
            if (dotPath == null) return List.of();

            String[] rawParts = dotPath.split("\\.");
            ArrayList<PathSegment> pathSegments = new ArrayList<>(rawParts.length);

            for (String rawPart : rawParts) {
                if (rawPart == null) return List.of();

                String segmentText = rawPart.trim();
                if (segmentText.isEmpty()) return List.of();

                Integer parsedIndex = null;

                int bracketIndex = segmentText.indexOf('[');
                if (bracketIndex >= 0 && segmentText.endsWith("]")) {
                    String baseName = segmentText.substring(0, bracketIndex).trim();
                    String insideText = segmentText.substring(bracketIndex + 1, segmentText.length() - 1).trim();
                    if (baseName.isEmpty()) return List.of();

                    if ("tier".equalsIgnoreCase(insideText)) {
                        parsedIndex = Integer.MIN_VALUE;
                    } else {
                        try {
                            parsedIndex = Integer.parseInt(insideText);
                        } catch (NumberFormatException ignored) {
                            return List.of();
                        }
                    }

                    segmentText = baseName;
                }

                pathSegments.add(new PathSegment(segmentText, parsedIndex));
            }

            return pathSegments;
        }
    }


    private static String stripTemplateSuffix(Path relativePath) {
        String normalizedPath = relativePath.toString().replace('\\', '/');
        return stripTemplateSuffix(normalizedPath);
    }

    private static String stripTemplateSuffix(String filenameOrRelativePath) {
        String normalizedPath = filenameOrRelativePath.replace('\\', '/');
        if (normalizedPath.toLowerCase(Locale.ROOT).endsWith(TEMPLATE_SUFFIX)) {
            return normalizedPath.substring(0, normalizedPath.length() - TEMPLATE_SUFFIX.length()) + ".json";
        }
        return normalizedPath;
    }

    private static void writeUtf8(Path destinationPath, String content) throws IOException {
        Files.writeString(destinationPath, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void deleteDirectoryIfExists(Path directory) throws IOException {
        if (!Files.exists(directory)) return;

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {}
            });
        }
    }

    private static ResourceTree openResourceTreeFromJar() throws Exception {
        ClassLoader classLoader = EliteMobsAssetGenerator.class.getClassLoader();
        URL url = classLoader.getResource(TEMPLATE_ROOT);
        if (url == null) {

            Path buildResources = Paths.get("build", "resources", "main", TEMPLATE_ROOT);
            if (Files.exists(buildResources)) return new ResourceTree(null, buildResources, false);

            Path sourceResources = Paths.get("src", "main", "resources", TEMPLATE_ROOT);
            if (Files.exists(sourceResources)) return new ResourceTree(null, sourceResources, false);

            throw new IllegalStateException("Missing resources folder: " + TEMPLATE_ROOT);
        }

        URI uri = url.toURI();

        if ("jar".equalsIgnoreCase(uri.getScheme())) {
            FileSystem fileSystem;
            try {
                fileSystem = FileSystems.newFileSystem(uri, Map.of());
            } catch (FileSystemAlreadyExistsException alreadyExists) {
                fileSystem = FileSystems.getFileSystem(uri);
            }

            Path root = fileSystem.getPath("/" + TEMPLATE_ROOT);
            return new ResourceTree(fileSystem, root, true);
        }

        Path root = Paths.get(uri);
        return new ResourceTree(null, root, false);
    }

    private record ResourceTree(FileSystem fileSystem, Path root, boolean closeFileSystem) implements AutoCloseable {
        List<Path> walkResourceRoot() throws IOException {
            try (Stream<Path> paths = Files.walk(root)) {
                return paths.toList();
            }
        }

        Path relativizeFromRoot(Path path) {
            return root.relativize(path);
        }

        @Override
        public void close() {
            if (closeFileSystem && fileSystem != null) {
                try {
                    fileSystem.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private static int generateSummonRoleAssets(Path outputRootDirectory, EliteMobsConfig config) throws IOException {
        if (config == null || config.abilitiesConfig == null || config.abilitiesConfig.defaultAbilities == null) return 0;
        var abilityConfig = config.abilitiesConfig.defaultAbilities.get(EliteMobsUndeadSummonAbilityFeature.ABILITY_UNDEAD_SUMMON);
        if (!(abilityConfig instanceof EliteMobsConfig.SummonAbilityConfig summonConfig)) return 0;
        cleanupSummonRoleAssets(outputRootDirectory);
        if (summonConfig.spawnMarkerEntriesByRole == null || summonConfig.spawnMarkerEntriesByRole.isEmpty()) {
            config.populateSummonMarkerEntriesByRoleIfEmpty();
        }
        if (summonConfig.spawnMarkerEntriesByRole == null || summonConfig.spawnMarkerEntriesByRole.isEmpty()) return 0;

        String rootTemplateText = readResourceText(
                "ServerTemplates/Item/RootInteractions/NPCs/EliteMobs/EliteMobs_Ability_UndeadSummon_Root.template.json"
        );
        String entryTemplateText = readResourceText(
                "ServerTemplates/Item/Interactions/NPCs/EliteMobs/EliteMobs_Ability_UndeadSummon_Entry.template.json"
        );
        if (rootTemplateText == null || entryTemplateText == null) return 0;

        int written = 0;

        for (Map.Entry<String, List<EliteMobsConfig.SummonMarkerEntry>> entry : summonConfig.spawnMarkerEntriesByRole.entrySet()) {
            if (entry == null) continue;
            String roleIdentifier = entry.getKey();
            if (roleIdentifier == null || roleIdentifier.isBlank()) continue;
            String normalizedRoleIdentifier = EliteMobsConfig.normalizeRoleIdentifier(roleIdentifier);
            if (normalizedRoleIdentifier == null || normalizedRoleIdentifier.isBlank()) continue;
            List<EliteMobsConfig.SummonMarkerEntry> npcEntries = entry.getValue();
            if (npcEntries == null || npcEntries.isEmpty()) continue;

            String markerBaseId = "EliteMobs_UndeadBow_Summon_Marker";
            String entryBaseId = "EliteMobs_Ability_UndeadSummon_EntryInteraction_" + normalizedRoleIdentifier;
            String rootBaseId = "EliteMobs_Ability_UndeadSummon_RootInteraction_" + normalizedRoleIdentifier;

            for (int tierIndex = 0; tierIndex < 5; tierIndex++) {
                String markerId = TemplateNameGenerator.appendTierSuffix(markerBaseId, config, tierIndex);
                String entryId = TemplateNameGenerator.appendTierSuffix(entryBaseId, config, tierIndex);
                String entryJson = entryTemplateText.replace(
                        "${features.UndeadSummon.config.templates.summonMarker}",
                        "\"" + markerId + "\""
                );
                Path entryPath = outputRootDirectory.resolve(Paths.get(
                        "Item", "Interactions", "NPCs", "EliteMobs", entryId + ".json"
                ));
                Files.createDirectories(entryPath.getParent());
                writeUtf8(entryPath, entryJson);
                written++;

                String rootId = TemplateNameGenerator.appendTierSuffix(rootBaseId, config, tierIndex);
                String rootJson = rootTemplateText.replace(
                        "${features.UndeadSummon.config.templates.entryInteraction}",
                        "\"" + entryId + "\""
                );
                Path rootPath = outputRootDirectory.resolve(Paths.get(
                        "Item", "RootInteractions", "NPCs", "EliteMobs", rootId + ".json"
                ));
                Files.createDirectories(rootPath.getParent());
                writeUtf8(rootPath, rootJson);
                written++;
            }
        }

        return written;
    }


    private static int generateSummonRoleVariantAssets(Path outputRootDirectory, EliteMobsConfig config) throws IOException {
        if (config == null || config.abilitiesConfig == null || config.abilitiesConfig.defaultAbilities == null) return 0;
        var abilityConfig = config.abilitiesConfig.defaultAbilities.get(EliteMobsUndeadSummonAbilityFeature.ABILITY_UNDEAD_SUMMON);
        if (!(abilityConfig instanceof EliteMobsConfig.SummonAbilityConfig summonConfig)) return 0;
        cleanupSummonMarkerAssets(outputRootDirectory);
        if (summonConfig.spawnMarkerEntriesByRole == null || summonConfig.spawnMarkerEntriesByRole.isEmpty()) {
            config.populateSummonMarkerEntriesByRoleIfEmpty();
        }
        if (summonConfig.spawnMarkerEntriesByRole == null || summonConfig.spawnMarkerEntriesByRole.isEmpty()) return 0;

        LinkedHashSet<String> baseRoleIds = new LinkedHashSet<>();
        if (summonConfig.spawnMarkerEntriesByRole != null) {
            for (List<EliteMobsConfig.SummonMarkerEntry> entries : summonConfig.spawnMarkerEntriesByRole.values()) {
                collectSummonBaseRoleIds(entries, baseRoleIds);
            }
        }
        collectSummonBaseRoleIds(summonConfig.spawnMarkerEntries, baseRoleIds);

        if (baseRoleIds.isEmpty()) return 0;

        int written = 0;
        for (String baseRoleId : baseRoleIds) {
            String variantRoleId = EliteMobsConfig.buildSummonVariantRoleId(baseRoleId);
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("Type", "Variant");
            role.put("Reference", baseRoleId);
            role.put("SpawnParticles", "Undead_Digging");
            role.put("SpawnParticlesOffset", List.of(0, 1, 0));
            role.put("Modify", new LinkedHashMap<>());

            Path rolePath = outputRootDirectory.resolve(Paths.get(
                    "NPC", "Roles", "EliteMobs", variantRoleId + ".json"
            ));
            Files.createDirectories(rolePath.getParent());
            writeUtf8(rolePath, GSON.toJson(role));
            written++;
        }

        return written;
    }

    private static void collectSummonBaseRoleIds(
            List<EliteMobsConfig.SummonMarkerEntry> entries,
            Set<String> out
    ) {
        if (entries == null || out == null) return;
        for (EliteMobsConfig.SummonMarkerEntry entry : entries) {
            if (entry == null || entry.Name == null) continue;
            String name = entry.Name.trim();
            if (name.isEmpty()) continue;
            if (name.startsWith(EliteMobsConfig.SUMMON_ROLE_PREFIX)) {
                name = name.substring(EliteMobsConfig.SUMMON_ROLE_PREFIX.length()).trim();
            }
            if (!name.isEmpty()) out.add(name);
        }
    }

    private static void cleanupSummonMarkerAssets(Path outputRootDirectory) {
        Path markersDir = outputRootDirectory.resolve(Paths.get("NPC", "Spawn", "Markers", "EliteMobs"));
        if (!Files.isDirectory(markersDir)) return;
        try (var stream = Files.list(markersDir)) {
            stream.filter(path -> path.getFileName().toString().contains("_Summon_Marker_"))
                  .forEach(path -> {
                      try {
                          String text = Files.readString(path, StandardCharsets.UTF_8);
                          if (text.contains(EliteMobsConfig.SUMMON_ROLE_PREFIX)) {
                              Files.deleteIfExists(path);
                          }
                      } catch (IOException ignored) {}
                  });
        } catch (IOException ignored) {}
    }

    private static void cleanupSummonRoleAssets(Path outputRootDirectory) {
        Path rolesDir = outputRootDirectory.resolve(Paths.get("NPC", "Roles", "EliteMobs"));
        if (Files.isDirectory(rolesDir)) {
            try (var stream = Files.list(rolesDir)) {
                stream.filter(path -> path.getFileName().toString().startsWith(EliteMobsConfig.SUMMON_ROLE_PREFIX))
                      .forEach(path -> {
                          try {
                              Files.deleteIfExists(path);
                          } catch (IOException ignored) {}
                      });
            } catch (IOException ignored) {}
        }

        Path markersDir = outputRootDirectory.resolve(Paths.get("NPC", "Spawn", "Markers", "EliteMobs"));
        if (Files.isDirectory(markersDir)) {
            try (var stream = Files.list(markersDir)) {
                stream.filter(path -> path.getFileName().toString().contains("_Summon_Marker_"))
                      .forEach(path -> {
                          try {
                              String text = Files.readString(path, StandardCharsets.UTF_8);
                              if (text.contains(EliteMobsConfig.SUMMON_ROLE_PREFIX)) {
                                  Files.deleteIfExists(path);
                              }
                          } catch (IOException ignored) {}
                      });
            } catch (IOException ignored) {}
        }
    }


    private static String readResourceText(String resourcePath) throws IOException {
        ClassLoader classLoader = EliteMobsAssetGenerator.class.getClassLoader();
        try (var stream = classLoader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                Path fallback = Paths.get("src", "main", "resources", resourcePath);
                if (Files.exists(fallback)) {
                    return Files.readString(fallback, StandardCharsets.UTF_8);
                }
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
