package com.halloffame.thriftjsoa.core.config.client;

import lombok.Data;

import java.util.List;

/**
 * ThriftJsoaClientConfig
 * @author zhuwx
 */
@Data
public class ThriftJsoaClientConfig {

    /**
     * 是否在ThriftJsoa服务里发起连接请求，涉及traceId的消息头协议解析处理等
     */
    private boolean inTjServer = false;

    /**
     * 客户端列表
     */
    private List<LoadBalanceClientConfig> list;
}
