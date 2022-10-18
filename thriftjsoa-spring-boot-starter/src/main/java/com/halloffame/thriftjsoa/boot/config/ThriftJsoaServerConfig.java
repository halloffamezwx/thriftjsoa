package com.halloffame.thriftjsoa.boot.config;

import com.halloffame.thriftjsoa.core.config.server.*;
import com.halloffame.thriftjsoa.core.constant.LoadBalanceType;
import lombok.Data;

/**
 * spring boot starter的服务端配置属性
 * @author zhuwx
 */
@Data
public class ThriftJsoaServerConfig {

    /**
     * 服务模式
     */
    private ThreadedSelectorServerConfig threadedSelectorServerConfig = new ThreadedSelectorServerConfig();
    private NonblockingServerConfig nonblockingServerConfig;
    private ThreadPoolServerConfig threadPoolServerConfig;
    private SimpleServerConfig simpleServerConfig;
    private TomcatServerConfig tomcatServerConfig;
    private NettyServerConfig nettyServerConfig;

    /**
     * 注解类客户端的负载均衡类型，建议使用：RANDOM_WEIGHT-随机（加权），默认不指定
     * @see LoadBalanceType#getCode()
     */
    private String loadBalanceType;

}
