package com.halloffame.thriftjsoa.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.thrift.TProcessor;

/**
 * 基础配置（服务端）
 * @author zhuwx
 */
@Data
@Accessors(chain = true)
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

    /**
     * 优雅停机检查服务状态的次数
     */
    private int shutdownCheckFrequency = 10;

    /**
     * 优雅停机检查服务状态的间隔时间，单位：ms
     */
    private long shutdownCheckIntervalTime = 1000L;

}
