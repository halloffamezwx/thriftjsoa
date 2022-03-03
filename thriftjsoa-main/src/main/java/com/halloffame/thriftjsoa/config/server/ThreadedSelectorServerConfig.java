package com.halloffame.thriftjsoa.config.server;

import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.constant.ServerType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.concurrent.ExecutorService;

/**
 * 服务模式：非阻塞io，有一条线程专门负责accept，若干条Selector线程处理网络IO，一个Worker线程池处理消息
 * @author zhuwx
 */
@ToString
@Accessors(chain = true)
public class ThreadedSelectorServerConfig extends BaseServerConfig {

    /**
     * 服务模式
     */
    @Getter
    private String serverType = ServerType.THREADED_SELECTOR.getCode();

    /**
     * 用来处理已经连接的网络IO的线程数
     */
    @Getter
    @Setter
	private int selectorThreads = 2;

    /**
     * 用来处理请求消息的工作线程数（未指定executorService），设为0时，将在selectorThreads里处理，TNonblockingServer就是这样处理的
     */
    @Getter
    @Setter
    private int workerThreads = 5;

    /**
     * accept线程的连接传到每个selector线程的阻塞队列的大小
     */
    @Getter
    @Setter
	private int acceptQueueSizePerThread = 4;

    /**
     * The ExecutorService for handling dispatched requests
     */
    @Getter
    @Setter
	private ExecutorService executorService = null;

}
