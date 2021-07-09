package com.halloffame.thriftjsoa.boot.config;

import com.halloffame.thriftjsoa.config.BaseProxyConfig;
import com.halloffame.thriftjsoa.config.server.NonblockingServerConfig;
import com.halloffame.thriftjsoa.config.server.SimpleServerConfig;
import com.halloffame.thriftjsoa.config.server.ThreadPoolServerConfig;
import com.halloffame.thriftjsoa.config.server.ThreadedSelectorServerConfig;
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

}
