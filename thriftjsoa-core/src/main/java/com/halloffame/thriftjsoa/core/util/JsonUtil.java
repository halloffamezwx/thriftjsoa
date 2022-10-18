package com.halloffame.thriftjsoa.core.util;
/*
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * jackson工具
 * @author zhuwx
 */
/*
public class JsonUtil {

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        
        //设置Jackson序列化时只包含不为空的字段
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        //设置在反序列化时忽略在JSON字符串中存在，而在JavaBean中不存在的属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static ObjectMapper getObjectMapper() {
        return mapper;
    }

    /**
     * 序列化
     */
    /*
    public static String serialize(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 反序列化
     */
    /*
    public static <T> T deserialize(String json, Class<T> cls) {
        try {
            return mapper.readValue(json, cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}*/
