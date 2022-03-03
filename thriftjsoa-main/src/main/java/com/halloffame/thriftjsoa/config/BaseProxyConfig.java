package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.config.server.ThreadedSelectorServerConfig;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 基础配置（代理端）
 */
@Data
@Accessors(chain = true)
public class BaseProxyConfig {

    /**
     * 服务模式配置
     */
    private BaseServerConfig serverConfig = new ThreadedSelectorServerConfig();

    /**
     * 客户端连接配置（负载均衡）
     */
    private LoadBalanceClientConfig loadBalanceClientConfig = new LoadBalanceClientConfig();

}
