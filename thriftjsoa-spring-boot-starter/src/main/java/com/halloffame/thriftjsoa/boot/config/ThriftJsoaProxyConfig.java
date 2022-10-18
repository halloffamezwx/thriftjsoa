package com.halloffame.thriftjsoa.boot.config;

import com.halloffame.thriftjsoa.core.config.BaseProxyConfig;
import com.halloffame.thriftjsoa.core.config.server.*;
import lombok.Data;

/**
 * spring boot starter的代理端属性配置
 * @author zhuwx
 */
@Data
public class ThriftJsoaProxyConfig extends BaseProxyConfig {

    /**
     * 服务模式
     */
    private ThreadedSelectorServerConfig threadedSelectorServerConfig = new ThreadedSelectorServerConfig();
    private NonblockingServerConfig nonblockingServerConfig;
    private ThreadPoolServerConfig threadPoolServerConfig;
    private SimpleServerConfig simpleServerConfig;
    private TomcatServerConfig tomcatServerConfig;
    private NettyServerConfig nettyServerConfig;

}
