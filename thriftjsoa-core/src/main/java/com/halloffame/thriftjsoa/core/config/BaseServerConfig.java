package com.halloffame.thriftjsoa.core.config;

import com.halloffame.thriftjsoa.core.config.common.ProcessorConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基础配置（服务端）
 * @author zhuwx
 */
//@Data
@Accessors(chain = true)
public class BaseServerConfig extends BaseConfig {

    /**
     * 服务模式
     */
    @Getter
	private String serverType;

    /**
     * thrift业务处理的processor列表配置
     */
    @Getter
    @Setter
    private List<ProcessorConfig> processorConfigs;

    /**
     * 服务端class列表，目前用于注册kryo
     */
    @Getter
    @Setter
    private Set<Class<?>> serviceClazzs = new HashSet<>();

    /**
     * 注册中心节点保存的数据：客户端请求此服务端的配置数据，例如连接池数据等
     */
    @Getter
    @Setter
    private BaseClientConfig registerClientConnServerConfig;

    /**
     * 优雅停机检查服务状态的次数
     */
    @Getter
    @Setter
    private int shutdownCheckFrequency = 10;

    /**
     * 优雅停机检查服务状态的间隔时间，单位：ms
     */
    @Getter
    @Setter
    private long shutdownCheckIntervalTime = 1000L;

}
