package com.halloffame.thriftjsoa.boot.properties;

import com.halloffame.thriftjsoa.boot.config.ThriftJsoaProxyConfig;
import com.halloffame.thriftjsoa.boot.config.ThriftJsoaServerConfig;
import com.halloffame.thriftjsoa.boot.constant.ThriftjsoaConstant;
import com.halloffame.thriftjsoa.config.client.ThriftJsoaClientConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * spring boot starter的配置属性
 * @author zhuwx
 */
@Data
@ConfigurationProperties(prefix = ThriftjsoaConstant.THRIFTJSOA_PREFIX)
public class ThriftjsoaProperties {

    /**
     * 服务端配置
     */
    private ThriftJsoaServerConfig server;

    /**
     * 代理端配置
     */
    private ThriftJsoaProxyConfig proxy;

    /**
     * 客户端配置
     */
    private ThriftJsoaClientConfig client;

}
