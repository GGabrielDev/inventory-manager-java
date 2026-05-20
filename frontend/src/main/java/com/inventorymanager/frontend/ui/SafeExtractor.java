package com.inventorymanager.frontend.ui;

import java.util.Map;
import java.util.Optional;

public final class SafeExtractor {
    
    private SafeExtractor() {}

    public static long safeLong(Map<String, Object> data, String key, long defaultValue) {
        if (data == null || !data.containsKey(key)) return defaultValue;
        Object val = data.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static String safeString(Map<String, Object> data, String key, String defaultValue) {
        if (data == null || !data.containsKey(key)) return defaultValue;
        Object val = data.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    public static <T> Optional<T> extract(Map<String, Object> data, String key, Class<T> type) {
        if (data == null || !data.containsKey(key)) return Optional.empty();
        Object val = data.get(key);
        if (type.isInstance(val)) {
            return Optional.of(type.cast(val));
        }
        return Optional.empty();
    }
}
