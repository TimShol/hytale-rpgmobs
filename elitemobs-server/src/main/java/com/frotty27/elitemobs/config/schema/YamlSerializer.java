package com.frotty27.elitemobs.config.schema;

import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public final class YamlSerializer {

    private YamlSerializer() {}

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(YamlSerializer.class.getName());

    private static final Yaml YAML = new Yaml();

    
    private static final Map<Class<?>, List<Field>> ALL_FIELDS_CACHE = new HashMap<>();

    private record FieldRef(Object owner, Field field) {}

    public static <T> T loadOrCreate(Path configDirectory, T javaDefaultsInstance) {
        try {
            Files.createDirectories(configDirectory);

            Map<String, List<FieldRef>> annotatedFieldsByFileName =
                    groupAnnotatedFieldsByFileName(javaDefaultsInstance);

            Map<String, Map<String, Object>> yamlRootByFileName = new LinkedHashMap<>();
            for (String fileName : annotatedFieldsByFileName.keySet()) {
                Path yamlFilePath = configDirectory.resolve(fileName);
                yamlRootByFileName.put(fileName, readYamlFileAsStringKeyMap(yamlFilePath));
            }

            
            applyYamlOverrides(javaDefaultsInstance, annotatedFieldsByFileName, yamlRootByFileName);

            
            writeYamlFiles(configDirectory, javaDefaultsInstance, annotatedFieldsByFileName, yamlRootByFileName);

            return javaDefaultsInstance;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return javaDefaultsInstance;
        }
    }

    public static String readConfigVersion(Path configDirectory, String mainFileName, String versionKey) {
        Path path = configDirectory.resolve(mainFileName);
        Map<String, Object> yaml = readYamlFileAsStringKeyMap(path);
        
        
        Object ver = yaml.get(versionKey);
        if (ver != null) return String.valueOf(ver);

        
        for (Object value : yaml.values()) {
            if (value instanceof Map<?, ?> group) {
                Object groupedVer = group.get(versionKey);
                if (groupedVer != null) return String.valueOf(groupedVer);
            }
        }
        
        return "0.0.0";
    }

    
    private static Map<String, List<FieldRef>> groupAnnotatedFieldsByFileName(Object configInstance) {
        Map<String, List<FieldRef>> out = new LinkedHashMap<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        collectAnnotatedFields(configInstance, visited, out);
        return out;
    }

    private static void collectAnnotatedFields(
            Object instance,
            IdentityHashMap<Object, Boolean> visited,
            Map<String, List<FieldRef>> out
    ) {
        if (instance == null) return;
        if (visited.put(instance, Boolean.TRUE) != null) return;

        for (Field field : getAllInstanceFields(instance.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (isIgnoredField(field)) continue;

            field.setAccessible(true);

            Cfg meta = field.getAnnotation(Cfg.class);
            if (meta != null) {
                out.computeIfAbsent(meta.file(), _ -> new ArrayList<>()).add(new FieldRef(instance, field));
                continue;
            }

            if (!isPojoType(field.getType())) continue;

            Object child;
            try {
                child = field.get(instance);
            } catch (Throwable ignored) {
                continue;
            }

            collectAnnotatedFields(child, visited, out);
        }
    }

    private static List<Field> getAllInstanceFields(Class<?> type) {
        List<Field> cached = ALL_FIELDS_CACHE.get(type);
        if (cached != null) return cached;

        ArrayList<Field> fields = new ArrayList<>();
        Class<?> cur = type;
        while (cur != null && cur != Object.class) {
            fields.addAll(Arrays.asList(cur.getDeclaredFields()));
            cur = cur.getSuperclass();
        }

        ALL_FIELDS_CACHE.put(type, fields);
        return fields;
    }

    private static boolean isIgnoredField(Field field) {
        if (field == null) return true;

        int mods = field.getModifiers();
        if (Modifier.isTransient(mods)) return true;

        return field.getAnnotation(YamlIgnore.class) != null;
    }

    
    private static Map<String, Object> readYamlFileAsStringKeyMap(Path yamlFilePath) {
        if (!Files.exists(yamlFilePath)) return new LinkedHashMap<>();

        try (Reader reader = Files.newBufferedReader(yamlFilePath, StandardCharsets.UTF_8)) {
            Object loadedYaml = YAML.load(reader);

            if (loadedYaml instanceof Map<?, ?> yamlMap) {
                return toStringKeyMap(yamlMap);
            }

            return new LinkedHashMap<>();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    private static Map<String, Object> toStringKeyMap(Map<?, ?> rawMap) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null) continue;
            out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }

    
    private static <T> void applyYamlOverrides(
            T configInstance,
            Map<String, List<FieldRef>> annotatedFieldsByFileName,
            Map<String, Map<String, Object>> yamlRootByFileName
    ) throws IllegalAccessException {

        for (Map.Entry<String, List<FieldRef>> fileEntry : annotatedFieldsByFileName.entrySet()) {
            String fileName = fileEntry.getKey();
            Map<String, Object> rootYaml = yamlRootByFileName.getOrDefault(fileName, Map.of());

            for (FieldRef fieldRef : fileEntry.getValue()) {
                Field field = fieldRef.field;
                Object owner = fieldRef.owner;
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (isIgnoredField(field)) continue;
                if (field.getAnnotation(CfgVersion.class) != null) continue; 

                Cfg meta = field.getAnnotation(Cfg.class);
                if (meta == null) continue;

                String yamlKey = getYamlKeyForField(field, meta);

                Map<String, Object> yamlScope = resolveYamlScope(rootYaml, meta);
                if (yamlScope == null) continue;

                Object yamlValue = yamlScope.get(yamlKey);
                if (yamlValue == null && !yamlScope.containsKey(yamlKey)) continue;

                field.setAccessible(true);
                Object currentValue = field.get(owner);

                
                if (yamlValue == null) {
                    if (field.getType().isPrimitive()) continue;
                    if (currentValue != null) continue;
                    field.set(owner, null);
                    continue;
                }

                Object convertedValue = convertYamlValueToFieldType(field, yamlValue, currentValue);
                if (convertedValue == null) continue;

                field.set(owner, convertedValue);
            }
        }
    }

    private static Map<String, Object> resolveYamlScope(Map<String, Object> rootYaml, Cfg meta) {
        String group = safeGroupName(meta);
        if (group.isBlank()) return rootYaml;

        Object section = rootYaml.get(group);
        if (!(section instanceof Map<?, ?> map)) return null;

        
        return toStringKeyMap(map);
    }

    private static String safeGroupName(Cfg meta) {
        try {
            String g = meta.group();
            return (g == null) ? "" : g.trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String getYamlKeyForField(Field field, Cfg meta) {
        if (meta.key() != null && !meta.key().isBlank()) return meta.key();
        return field.getName();
    }

    
    private static Object convertYamlValueToFieldType(Field field, Object yamlValue, Object currentValue) {
        if (yamlValue == null) return null;

        Class<?> fieldType = field.getType();

        
        if (fieldType.isEnum()) return parseEnum(fieldType, yamlValue);

        
        if (fieldType == String.class) return String.valueOf(yamlValue);
        if (fieldType == boolean.class || fieldType == Boolean.class) return toBoolean(yamlValue);
        if (fieldType == int.class || fieldType == Integer.class) return clampNumber(field, toInt(yamlValue), fieldType);
        if (fieldType == long.class || fieldType == Long.class) return clampNumber(field, toLong(yamlValue), fieldType);
        if (fieldType == float.class || fieldType == Float.class) return clampNumber(field, toFloat(yamlValue), fieldType);
        if (fieldType == double.class || fieldType == Double.class) return clampNumber(field, toDouble(yamlValue), fieldType);

        
        if (fieldType.isArray()) {
            if (!(yamlValue instanceof List<?> yamlList)) return null;

            Class<?> componentType = fieldType.getComponentType();
            int desiredLength = yamlList.size();
            FixedArraySize fixedArraySize = field.getAnnotation(FixedArraySize.class);
            if (fixedArraySize != null && fixedArraySize.value() > 0) {
                desiredLength = fixedArraySize.value();
                if (yamlList.size() != desiredLength) {
                    LOGGER.warning(String.format(
                            "Config array size mismatch for '%s': expected=%d, got=%d (will %s)",
                            field.getName(),
                            desiredLength,
                            yamlList.size(),
                            (yamlList.size() > desiredLength) ? "truncate" : "pad with defaults"
                    ));
                }
            }

            Object arrayValue = Array.newInstance(componentType, desiredLength);
            Object defaultArray = (currentValue != null && currentValue.getClass().isArray()) ? currentValue : null;
            int defaultLength = (defaultArray != null) ? Array.getLength(defaultArray) : 0;

            for (int i = 0; i < desiredLength; i++) {
                Object yamlEl = (i < yamlList.size()) ? yamlList.get(i) : null;
                Object convertedEl = (yamlEl != null) ? convertScalar(componentType, yamlEl) : null;

                if (convertedEl == null) {
                    if (defaultArray != null && i < defaultLength) convertedEl = Array.get(defaultArray, i);
                    if (convertedEl == null && componentType.isPrimitive()) convertedEl = primitiveDefault(componentType);
                }

                Array.set(arrayValue, i, convertedEl);
            }

            return arrayValue;
        }

        
        if (List.class.isAssignableFrom(fieldType)) {
            if (!(yamlValue instanceof List<?> yamlList)) return null;

            Class<?> elementType = getListElementType(field);
            ArrayList<Object> convertedList = new ArrayList<>(yamlList.size());

            for (Object yamlEl : yamlList) {
                if (elementType != null && isPojoType(elementType) && yamlEl instanceof Map<?, ?> yamlMap) {
                    convertedList.add(mapToPojo(yamlMap, elementType));
                } else {
                    convertedList.add(convertScalar(elementType, yamlEl));
                }
            }

            return convertedList;
        }

        
        if (Map.class.isAssignableFrom(fieldType)) {
            if (!(yamlValue instanceof Map<?, ?> yamlMap)) return null;

            Class<?> keyType = getMapKeyType(field);
            Class<?> valType = getMapValueType(field);

            LinkedHashMap<Object, Object> merged = new LinkedHashMap<>();

            
            if (currentValue instanceof Map<?, ?> curMap) {
                merged.putAll(curMap);
            }

            for (Map.Entry<?, ?> e : yamlMap.entrySet()) {
                Object k = convertScalar(keyType, e.getKey());
                if (k == null) continue;

                Object rawV = e.getValue();

                
                if (rawV == null) {
                    Object existing = merged.get(k);
                    if (existing != null) continue;
                    merged.put(k, null);
                    continue;
                }

                Object existingEntry = merged.get(k);

                Object v;
                if (valType != null && isPojoType(valType) && rawV instanceof Map<?, ?> inner) {
                    
                    if (valType.isInstance(existingEntry)) {
                        v = mapToPojoInto(inner, existingEntry);
                    } else {
                        v = mapToPojo(inner, valType);
                    }
                } else {
                    v = convertScalar(valType, rawV);
                    
                    if (v == null && existingEntry != null) v = existingEntry;
                }

                merged.put(k, v);
            }

            return merged;
        }

        
        if (yamlValue instanceof Map<?, ?> yamlMap && isPojoType(fieldType)) {
            if (fieldType.isInstance(currentValue)) {
                return mapToPojoInto(yamlMap, currentValue);
            }
            return mapToPojo(yamlMap, fieldType);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E parseEnum(Class<?> enumType, Object yamlValue) {
        try {
            return Enum.valueOf((Class<E>) enumType, String.valueOf(yamlValue).trim().toUpperCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object convertScalar(Class<?> wantedType, Object yamlValue) {
        if (yamlValue == null) return null;
        if (wantedType == null) return yamlValue;

        if (wantedType.isEnum()) return parseEnum(wantedType, yamlValue);

        if (wantedType == String.class) return String.valueOf(yamlValue);
        if (wantedType == boolean.class || wantedType == Boolean.class) return toBoolean(yamlValue);
        if (wantedType == int.class || wantedType == Integer.class) return toInt(yamlValue);
        if (wantedType == long.class || wantedType == Long.class) return toLong(yamlValue);
        if (wantedType == float.class || wantedType == Float.class) return toFloat(yamlValue);
        if (wantedType == double.class || wantedType == Double.class) return toDouble(yamlValue);

        if (wantedType.isInstance(yamlValue)) return yamlValue;

        if (yamlValue instanceof Map<?, ?> yamlMap && isPojoType(wantedType)) {
            return mapToPojo(yamlMap, wantedType);
        }

        return yamlValue;
    }

    private static Object primitiveDefault(Class<?> primitiveType) {
        if (primitiveType == boolean.class) return false;
        if (primitiveType == int.class) return 0;
        if (primitiveType == long.class) return 0L;
        if (primitiveType == float.class) return 0f;
        if (primitiveType == double.class) return 0d;
        if (primitiveType == short.class) return (short) 0;
        if (primitiveType == byte.class) return (byte) 0;
        if (primitiveType == char.class) return (char) 0;
        return 0;
    }

    private static Object clampNumber(Field field, Number value, Class<?> targetType) {
        if (value == null || field == null || targetType == null) return value;

        Min min = field.getAnnotation(Min.class);
        Max max = field.getAnnotation(Max.class);
        if (min == null && max == null) return value;

        double clamped = value.doubleValue();
        if (min != null) clamped = Math.max(min.value(), clamped);
        if (max != null) clamped = Math.min(max.value(), clamped);

        if (targetType == int.class || targetType == Integer.class) return (int) clamped;
        if (targetType == long.class || targetType == Long.class) return (long) clamped;
        if (targetType == float.class || targetType == Float.class) return (float) clamped;
        return clamped;
    }

    
    private static void writeYamlFiles(
            Path configDirectory,
            Object configInstance,
            Map<String, List<FieldRef>> annotatedFieldsByFileName,
            Map<String, Map<String, Object>> yamlRootByFileName
    ) throws Exception {

        for (Map.Entry<String, List<FieldRef>> fileEntry : annotatedFieldsByFileName.entrySet()) {
            String fileName = fileEntry.getKey();
            List<FieldRef> fields = fileEntry.getValue();

            Path yamlFilePath = configDirectory.resolve(fileName);
            Map<String, Object> yamlRoot = yamlRootByFileName.get(fileName);

            String yamlText = buildYamlText(configInstance, fields, yamlRoot);

            Files.writeString(
                    yamlFilePath,
                    yamlText,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    private static String buildYamlText(Object configInstance, List<FieldRef> fields, Map<String, Object> yamlRoot) throws Exception {
        StringBuilder out = new StringBuilder(8192);

        out.append("# Auto-generated by EliteMobs.\n");
        out.append("# - Java defaults are the source of truth\n");
        out.append("# - YAML overrides are applied on boot\n");
        out.append("# - Missing keys are re-added automatically\n\n");

        LinkedHashMap<String, List<FieldRef>> byGroup = new LinkedHashMap<>();
        for (FieldRef fieldRef : fields) {
            Field field = fieldRef.field;
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (isIgnoredField(field)) continue;

            Cfg meta = field.getAnnotation(Cfg.class);
            if (meta == null) continue;

            String groupName = safeGroupName(meta);
            byGroup.computeIfAbsent(groupName, _ -> new ArrayList<>()).add(fieldRef);
        }

        List<FieldRef> rootFields = byGroup.remove("");
        if (rootFields != null && !rootFields.isEmpty()) {
            appendFieldBlock(out, configInstance, rootFields, yamlRoot, 0);
            out.append("\n");
        }

        for (Map.Entry<String, List<FieldRef>> groupEntry : byGroup.entrySet()) {
            String groupName = groupEntry.getKey();
            if (groupName == null || groupName.isBlank()) continue;

            out.append(groupName).append(":\n");
            appendFieldBlock(out, configInstance, groupEntry.getValue(), yamlRoot, 2);
            out.append("\n");
        }

        return out.toString();
    }

    private static void appendFieldBlock(
            StringBuilder out,
            Object configInstance,
            List<FieldRef> fields,
            Map<String, Object> yamlRoot,
            int baseIndent
    ) throws Exception {

        for (FieldRef fieldRef : fields) {
            Field field = fieldRef.field;
            Object owner = fieldRef.owner;
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (isIgnoredField(field)) continue;

            Cfg meta = field.getAnnotation(Cfg.class);
            if (meta == null) continue;

            String yamlKey = getYamlKeyForField(field, meta);

            if (meta.comment() != null && !meta.comment().isBlank()) {
                for (String line : meta.comment().split("\n")) {
                    indent(out, baseIndent);
                    out.append("# ").append(line).append("\n");
                }
            }

            field.setAccessible(true);

            Map<String, Object> yamlScope = resolveYamlScope(yamlRoot, meta);

            Object valueToWrite;

            if (yamlScope != null && yamlScope.containsKey(yamlKey) && field.getAnnotation(CfgVersion.class) == null) {
                
                
                Object userValue = yamlScope.get(yamlKey);

                if (userValue == null) {
                    
                    valueToWrite = null;
                } else if (shouldWriteMergedValue(field, userValue)) {
                    valueToWrite = field.get(owner); 
                } else {
                    valueToWrite = userValue; 
                }
            } else {
                
                valueToWrite = field.get(owner);
            }

            indent(out, baseIndent);
            out.append(yamlKey).append(":");
            appendYamlValue(out, valueToWrite, baseIndent);
            out.append("\n\n");
        }
    }

    private static boolean shouldWriteMergedValue(Field field, Object userYamlValue) {
        if (field == null || userYamlValue == null) return false;

        Class<?> t = field.getType();

        
        if (t.isArray()) return true;
        if (Map.class.isAssignableFrom(t)) return true;
        if (List.class.isAssignableFrom(t)) return true;
        if (isPojoType(t)) return true;

        
        if (userYamlValue instanceof Map<?, ?>) return true;
        return userYamlValue instanceof List<?>;
    }

    private static void appendYamlValue(StringBuilder out, Object value, int indent) {
        if (value == null) {
            out.append(" null");
            return;
        }

        
        if (value instanceof Enum<?> e) {
            out.append(" ").append(e.name());
            return;
        }

        Class<?> valueType = value.getClass();

        if (valueType.isArray()) {
            int len = Array.getLength(value);
            if (len == 0) {
                out.append(" []");
                return;
            }

            out.append("\n");
            for (int i = 0; i < len; i++) {
                indent(out, indent + 2);
                out.append("- ");
                appendYamlValue(out, Array.get(value, i), indent + 2);
                out.append("\n");
            }
            return;
        }

        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                out.append(" []");
                return;
            }

            
            if (isScalarOnlyList(list)) {
                appendFlowList(out, list);
                return;
            }

            out.append("\n");
            for (Object element : list) {
                indent(out, indent + 2);
                out.append("-");

                
                if (element instanceof List<?> innerList && isScalarOnlyList(innerList)) {
                    appendFlowList(out, innerList);
                    out.append("\n");
                    continue;
                }

                
                if (element instanceof Map<?, ?> innerMap && !innerMap.isEmpty() && isScalarOnlyMap(innerMap)) {
                    appendFlowMap(out, innerMap);
                    out.append("\n");
                    continue;
                }

                out.append(" ");
                appendYamlValue(out, element, indent + 2);
                out.append("\n");
            }
            return;
        }

        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                out.append(" {}");
                return;
            }

            out.append("\n");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                indent(out, indent + 2);
                out.append(entry.getKey()).append(":");

                Object entryValue = entry.getValue();
                if (isScalarValue(entryValue)) out.append(" ");

                appendYamlValue(out, entryValue, indent + 2);
                out.append("\n");
            }
            return;
        }

        if (isPojoType(valueType)) {
            out.append("\n");

            for (Field f : getAllInstanceFields(valueType)) {
                int mods = f.getModifiers();
                if (Modifier.isStatic(mods)) continue;
                if (isIgnoredField(f)) continue;

                f.setAccessible(true);

                Object fv;
                try {
                    fv = f.get(value);
                } catch (Throwable ignored) {
                    continue;
                }

                indent(out, indent + 2);
                out.append(f.getName()).append(":");
                if (isScalarValue(fv)) out.append(" ");

                appendYamlValue(out, fv, indent + 2);
                out.append("\n");
            }
            return;
        }

        out.append(" ").append(formatScalar(value));
    }

    private static boolean isScalarValue(Object value) {
        if (value == null) return true;
        if (value instanceof Enum<?>) return true;
        Class<?> c = value.getClass();
        return c.isPrimitive() || value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character;
    }

    private static String formatScalar(Object value) {
        return switch (value) {
            case null -> "null";
            case Enum<?> e -> e.name();
            case String s -> quote(s);
            case Character ch -> quote(String.valueOf(ch));
            default -> String.valueOf(value);
        };
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void indent(StringBuilder out, int spaces) {
        out.append(" ".repeat(Math.max(0, spaces)));
    }

    
    private static boolean isScalarOnlyMap(Map<?, ?> map) {
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() == null) return false;
            Object k = e.getKey();
            if (!(k instanceof String) && !(k instanceof Number) && !(k instanceof Boolean)) return false;
            if (!isScalarValue(e.getValue())) return false;
        }
        return true;
    }

    private static void appendFlowMap(StringBuilder out, Map<?, ?> map) {
        out.append(" { ");
        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (i++ > 0) out.append(", ");
            out.append(entry.getKey()).append(": ").append(formatScalar(entry.getValue()));
        }
        out.append(" }");
    }

    private static boolean isScalarOnlyList(List<?> list) {
        for (Object o : list) {
            if (!isScalarValue(o)) return false;
        }
        return true;
    }

    private static void appendFlowList(StringBuilder out, List<?> list) {
        out.append(" [");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(formatScalar(list.get(i)));
        }
        out.append("]");
    }

    private static Class<?> getListElementType(Field field) {
        Type gt = field.getGenericType();
        if (!(gt instanceof ParameterizedType pt)) return null;
        return toClass(pt.getActualTypeArguments()[0]);
    }

    private static Class<?> getMapKeyType(Field field) {
        Type gt = field.getGenericType();
        if (!(gt instanceof ParameterizedType pt)) return null;
        return toClass(pt.getActualTypeArguments()[0]);
    }

    private static Class<?> getMapValueType(Field field) {
        Type gt = field.getGenericType();
        if (!(gt instanceof ParameterizedType pt)) return null;
        return toClass(pt.getActualTypeArguments()[1]);
    }

    private static Class<?> toClass(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) return c;
        return null;
    }

    
    private static boolean isPojoType(Class<?> type) {
        if (type.isPrimitive()) return false;
        if (type == String.class) return false;
        if (Number.class.isAssignableFrom(type)) return false;
        if (type == Boolean.class) return false;
        if (type == Character.class) return false;
        if (type.isEnum()) return false;
        if (List.class.isAssignableFrom(type)) return false;
        if (Map.class.isAssignableFrom(type)) return false;
        return !type.isArray();
    }

    private static Object mapToPojo(Map<?, ?> map, Class<?> pojoClass) {
        try {
            Object instance = pojoClass.getDeclaredConstructor().newInstance();
            return mapToPojoInto(map, instance);
        } catch (Throwable ignored) {
            return map;
        }
    }

    private static Object mapToPojoInto(Map<?, ?> map, Object targetPojo) {
        try {
            Class<?> pojoClass = targetPojo.getClass();

            for (Field field : getAllInstanceFields(pojoClass)) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods)) continue;
                if (isIgnoredField(field)) continue;

                field.setAccessible(true);

                String key = field.getName();
                if (!map.containsKey(key)) continue;

                Object rawValue = map.get(key);
                Object currentValue = field.get(targetPojo);

                
                if (rawValue == null) {
                    if (field.getType().isPrimitive()) continue;
                    if (currentValue != null) continue;
                    field.set(targetPojo, null);
                    continue;
                }

                Object convertedValue = convertYamlValueToFieldType(field, rawValue, currentValue);
                if (convertedValue != null) field.set(targetPojo, convertedValue);
            }

            return targetPojo;
        } catch (Throwable ignored) {
            return targetPojo;
        }
    }

    
    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Float toFloat(Object value) {
        if (value instanceof Number n) return n.floatValue();
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }
}
