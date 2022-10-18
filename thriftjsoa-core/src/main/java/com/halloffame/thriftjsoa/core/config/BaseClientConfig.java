package com.halloffame.thriftjsoa.core.config;

import com.halloffame.thriftjsoa.core.config.common.ClientClassConfig;
import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.List;

/**
 * 基础配置（客户端）
 * @author zhuwx
 */
@Data
public class BaseClientConfig extends BaseConfig {

    /**
     * 连接池配置
     */
    private GenericObjectPoolConfig poolConfig;

    /**
     * 最大连接数，并不会真的限制请求，只是用来负载均衡时计算权重，如果配了连接池则以poolConfig.maxTotal为准
     */
    private int maxTotal = 1;

    /**
     * 是否在ThriftJsoa服务里发起连接请求，涉及traceId的消息头协议解析处理等
     */
    private boolean inTjServer = false;

    /**
     * 客户端的class配置列表
     */
    private List<ClientClassConfig> clazzs;
}
