package com.inventorymanager.frontend.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {}

    public static Map<String, Object> map(String json) throws Exception {
        return MAPPER.readValue(json, new TypeReference<>() {});
    }
}
