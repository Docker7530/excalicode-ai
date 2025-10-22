package com.excalicode.platform.core.support;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 根据 DTO 结构生成 JSON 示例, 用于不支持 JSON Schema 的模型.
 */
@Component
public class JsonExampleGenerator {

    private static final String DEFAULT_TEMPORAL_EXAMPLE = "2024-01-01T00:00:00";

    private final ObjectMapper objectMapper;
    private final Map<Class<?>, String> cache = new ConcurrentHashMap<>();

    public JsonExampleGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 生成目标类型对应的 JSON 示例字符串.
     *
     * @param targetType 目标类型
     * @return 格式化 JSON 示例
     */
    public String generateExample(Class<?> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("targetType 不能为空");
        }
        return cache.computeIfAbsent(targetType, this::createExampleJson);
    }

    private String createExampleJson(Class<?> targetType) {
        Object structure = buildStructure(targetType, targetType.getSimpleName(), new HashSet<>());
        if (structure == null) {
            structure = new LinkedHashMap<>();
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(structure);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("生成 JSON 示例失败: " + targetType.getSimpleName(), e);
        }
    }

    private Object buildStructure(Type type, String hint, Set<Class<?>> visiting) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> rawClass) {
                if (Collection.class.isAssignableFrom(rawClass)) {
                    Type elementType = parameterizedType.getActualTypeArguments()[0];
                    Object element = buildStructure(elementType, deriveHint(hint, "项"), visiting);
                    List<Object> list = new ArrayList<>();
                    list.add(defaultIfNull(element, "示例项"));
                    return list;
                }
                if (Map.class.isAssignableFrom(rawClass)) {
                    Type keyType = parameterizedType.getActualTypeArguments()[0];
                    Type valueType = parameterizedType.getActualTypeArguments()[1];
                    Object keyExample = buildStructure(keyType, deriveHint(hint, "键"), visiting);
                    String key =
                            keyExample instanceof String && StringUtils.hasText((String) keyExample)
                                    ? (String) keyExample
                                    : deriveHint(hint, "key");
                    Object valueExample =
                            buildStructure(valueType, deriveHint(hint, "值"), visiting);
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put(key, defaultIfNull(valueExample, "示例值"));
                    return map;
                }
                if (Optional.class.isAssignableFrom(rawClass)) {
                    Type nestedType = parameterizedType.getActualTypeArguments()[0];
                    return buildStructure(nestedType, hint, visiting);
                }
                return buildStructure(rawClass, hint, visiting);
            }
        }
        if (type instanceof Class<?> clazz) {
            return buildStructureForClass(clazz, hint, visiting);
        }
        return "示例值";
    }

    private Object buildStructureForClass(Class<?> clazz, String hint, Set<Class<?>> visiting) {
        if (String.class.equals(clazz)) {
            return defaultString(hint);
        }
        if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) {
            if (clazz == boolean.class || clazz == Boolean.class) {
                return Boolean.TRUE;
            }
            return 1;
        }
        if (Boolean.class.equals(clazz)) {
            return Boolean.TRUE;
        }
        if (TemporalAccessor.class.isAssignableFrom(clazz)) {
            return DEFAULT_TEMPORAL_EXAMPLE;
        }
        if (clazz.isEnum()) {
            Object[] constants = clazz.getEnumConstants();
            return constants != null && constants.length > 0 ? constants[0].toString()
                    : defaultString(hint);
        }
        if (clazz.isArray()) {
            Object element =
                    buildStructure(clazz.getComponentType(), deriveHint(hint, "项"), visiting);
            List<Object> list = new ArrayList<>();
            list.add(defaultIfNull(element, "示例项"));
            return list;
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            List<Object> list = new ArrayList<>();
            list.add(defaultString(deriveHint(hint, "项")));
            return list;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(deriveHint(hint, "key"), defaultString(deriveHint(hint, "值")));
            return map;
        }
        if (!visiting.add(clazz)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)
                    || field.isSynthetic()) {
                continue;
            }
            String propertyName = resolvePropertyName(field);
            Object value = buildStructure(field.getGenericType(), propertyName, visiting);
            result.put(propertyName, defaultIfNull(value, defaultString(propertyName)));
        }
        visiting.remove(clazz);
        return result;
    }

    private String resolvePropertyName(Field field) {
        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && StringUtils.hasText(jsonProperty.value())) {
            return jsonProperty.value();
        }
        return field.getName();
    }

    private String deriveHint(String hint, String suffix) {
        if (!StringUtils.hasText(hint)) {
            return suffix;
        }
        return hint + suffix;
    }

    private String defaultString(String hint) {
        if (StringUtils.hasText(hint)) {
            return hint + "示例";
        }
        return "示例值";
    }

    private Object defaultIfNull(Object value, Object defaultValue) {
        return value != null ? value : defaultValue;
    }
}
