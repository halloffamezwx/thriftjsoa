package com.halloffame.thriftjsoa.boot.config;

import com.halloffame.thriftjsoa.config.server.NonblockingServerConfig;
import com.halloffame.thriftjsoa.config.server.SimpleServerConfig;
import com.halloffame.thriftjsoa.config.server.ThreadPoolServerConfig;
import com.halloffame.thriftjsoa.config.server.ThreadedSelectorServerConfig;
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

}
