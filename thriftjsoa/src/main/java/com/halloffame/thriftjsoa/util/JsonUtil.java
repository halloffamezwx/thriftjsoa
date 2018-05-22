package com.halloffame.thriftjsoa.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
    }

    public static ObjectMapper getObjectMapper() {
        return mapper;
    }

    public static String serialize(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(String json, Class<T> cls) {
        try {
            return mapper.readValue(json, cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
