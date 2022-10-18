package com.halloffame.thriftjsoa.core.config.server;

import com.halloffame.thriftjsoa.core.config.BaseServerConfig;
import com.halloffame.thriftjsoa.core.constant.ServerType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 服务模式：多线程（池）阻塞io
 * @author zhuwx
 */
@ToString
public class ThreadPoolServerConfig extends BaseServerConfig {

    /**
     * 服务模式
     */
    @Getter
    private String serverType = ServerType.THREAD_POOL.getCode();

    /**
     * 线程池的最小线程数
     */
    @Getter
    @Setter
	private int minWorkerThreads = 5;

    /**
     * 线程池的最大线程数
     */
    @Getter
    @Setter
    private int maxWorkerThreads = Integer.MAX_VALUE;

}
