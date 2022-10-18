package com.halloffame.thriftjsoa.core.config.server;

import com.halloffame.thriftjsoa.core.config.BaseServerConfig;
import com.halloffame.thriftjsoa.core.constant.ServerType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.ExecutorService;

/**
 * 服务模式：netty
 * @author zhuwx
 */
@ToString
public class NettyServerConfig extends BaseServerConfig {

    /**
     * 服务模式
     */
    @Getter
    private String serverType = ServerType.NETTY.getCode();

    /**
     * workerEventLoopGroup线程数
     */
    @Getter
    @Setter
    private int nThreads;

    /**
     * ExecutorService
     */
    @Getter
    @Setter
    private ExecutorService executorService;

}
