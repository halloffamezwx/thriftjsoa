package com.halloffame.thriftjsoa.config;

import lombok.Data;
import org.apache.thrift.TProcessor;

/**
 * 基础配置（服务端）
 * @author zhuwx
 */
@Data
public class BaseServerConfig extends BaseConfig {

    /**
     * 服务模式
     */
	private String serverType;

    /**
     * thrift业务处理的processor
     */
    private TProcessor processor;

    /**
     * 注册中心（zookeeper）节点保存的数据：客户端请求此服务端的配置数据，例如连接池数据等
     */
    private BaseClientConfig zkClientConnServerConfig;

}
